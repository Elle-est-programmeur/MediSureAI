package com.example.Backend.service;

import com.example.Backend.dto.*;
import com.example.Backend.model.Billing;
import com.example.Backend.model.BillingStatus;
import com.example.Backend.model.Drug;
import com.example.Backend.model.MedicalRecord;
import com.example.Backend.model.Patient;
import com.example.Backend.model.Receipt;
import com.example.Backend.model.Users;
import com.example.Backend.repository.BillingRepository;
import com.example.Backend.repository.DocumentRepository;
import com.example.Backend.repository.MedicalRecordRepository;
import com.example.Backend.repository.PatientRepository;
import com.example.Backend.repository.ReceiptRepository;
import com.example.Backend.repository.UserCredRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final GroqFormularyService groqFormularyService;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BillingRepository billingRepository;
    private final ReceiptRepository receiptRepository;
    private final BillingPaymentService billingPaymentService;
    private final UserCredRepo userCredRepo;

    /**
     * Feature 2: Health Timeline — unified view of uploaded documents, doctor-created
     * medical records, and billing events for the patient.
     */
    @Transactional(readOnly = true)
    public List<TimelineEventDTO> getTimeline(Users user) {
        Patient patient = patientRepository.findByUserId(user.getId()).orElse(null);

        List<TimelineEventDTO> events = new ArrayList<>();

        documentRepository.findByUser(user).stream()
                .filter(doc -> doc.getEventDate() != null || doc.getUploadedAt() != null)
                .forEach(doc -> events.add(TimelineEventDTO.builder()
                        .id("DOC-" + doc.getId())
                        .type("DOCUMENT")
                        .title(doc.getOriginalFileName() != null ? doc.getOriginalFileName() : "Document")
                        .details(doc.getDocumentType() != null ? doc.getDocumentType().name() : null)
                        .date(doc.getEventDate() != null ? doc.getEventDate() : doc.getUploadedAt())
                        .build()));

        if (patient != null) {
            medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                    .forEach(rec -> {
                        List<String> drugNames = rec.getDrugs() == null ? List.of()
                                : rec.getDrugs().stream().map(Drug::getName).filter(n -> n != null && !n.isBlank()).toList();
                        events.add(TimelineEventDTO.builder()
                                .id("REC-" + rec.getId())
                                .type("RECORD")
                                .title(rec.getDiagnosis() != null ? rec.getDiagnosis() : "Medical record")
                                .details(rec.getTreatmentPlan())
                                .doctorName(rec.getDoctor() != null ? rec.getDoctor().getName() : null)
                                .date(rec.getCreatedAt())
                                .tags(drugNames)
                                .build());
                    });

            billingRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                    .forEach(b -> events.add(TimelineEventDTO.builder()
                            .id("BIL-" + b.getId())
                            .type("BILLING")
                            .title("Billing: ₹" + b.getTotalCost())
                            .details((b.getDescription() != null ? b.getDescription() : "Billing entry")
                                    + " — " + (b.getStatus() != null ? b.getStatus().name() : "PENDING"))
                            .date(b.getCreatedAt())
                            .build()));
        }

        events.sort(Comparator.comparing(
                TimelineEventDTO::getDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return events;
    }

    /**
     * Feature 4: Smart Formulary Search — backed by Groq.
     */
    public SRLMResponse searchFormulary(String drugName, Users user) {
        log.info("Searching formulary for drug: {}", drugName);

        String context = "";
        try {
            List<org.springframework.ai.document.Document> policyDocs = vectorStore.similaritySearch(
                    SearchRequest.query(drugName)
                            .withTopK(5)
                            .withFilterExpression("documentType == 'INSURANCE_POLICY'")
            );
            context = policyDocs.stream()
                    .map(org.springframework.ai.document.Document::getContent)
                    .collect(Collectors.joining("\n"));
        } catch (Exception e) {
            log.warn("Vector store unavailable for policy lookup; continuing without context: {}", e.getMessage());
        }

        String answer = groqFormularyService.search(drugName, context);

        return SRLMResponse.builder()
                .query("Drug search: " + drugName)
                .finalAnswer(answer)
                .build();
    }

    // ── Profile ───────────────────────────────────────────────────────────────

    @Transactional
    public PatientProfileResponse createOrUpdateProfile(Long userId, PatientProfileRequest request) {
        Users user = userCredRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Patient patient = patientRepository.findByUserId(userId)
                .orElseGet(() -> Patient.builder().user(user).build());

        patient.setName(request.getName());
        patient.setAge(request.getAge());
        patient.setGender(request.getGender());

        Patient saved = patientRepository.save(patient);
        return toPatientResponse(saved);
    }

    @Transactional
    public PatientProfileResponse getProfile(Long userId) {
        Patient patient = getOrCreatePatient(userId);
        return toPatientResponse(patient);
    }

    // ── Records & billing (read-only patient view) ────────────────────────────

    @Transactional
    public List<MedicalRecordResponse> getMyRecords(Long userId) {
        Patient patient = getOrCreatePatient(userId);
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    @Transactional
    public List<BillingResponse> getMyBilling(Long userId) {
        Patient patient = getOrCreatePatient(userId);
        return billingRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(b -> toBillingResponse(b, receiptRepository.findByBillingId(b.getId()).orElse(null)))
                .toList();
    }

    /**
     * Mock payment flow:
     *   1. mark PROCESSING (commits)
     *   2. mark PAID + create Receipt (commits)
     * Each step is its own REQUIRES_NEW transaction so the DB genuinely transitions
     * PENDING → PROCESSING → PAID. Idempotent if already paid.
     */
    public BillingResponse payBilling(Long userId, Long billingId, String paymentMethod) {
        billingPaymentService.markProcessing(billingId, userId);
        BillingPaymentService.PaymentResult result =
                billingPaymentService.markPaidAndIssueReceipt(billingId, userId, paymentMethod);
        return toBillingResponse(result.billing(), result.receipt());
    }

    @Transactional(readOnly = true)
    public ReceiptDTO getReceipt(Long userId, Long billingId) {
        Patient patient = getOrCreatePatient(userId);
        Receipt receipt = receiptRepository.findByBillingId(billingId)
                .orElseThrow(() -> new EntityNotFoundException("No receipt for billing: " + billingId));
        Billing billing = receipt.getBilling();
        if (billing == null || billing.getPatient() == null
                || !patient.getId().equals(billing.getPatient().getId())) {
            throw new AccessDeniedException("This receipt does not belong to you");
        }
        return toReceiptDTO(receipt);
    }

    /** Find the Patient row for this user, or create a stub one (with auto-assigned MRN). */
    private Patient getOrCreatePatient(Long userId) {
        return patientRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Users user = userCredRepo.findById(userId)
                            .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
                    return patientRepository.save(Patient.builder().user(user).build());
                });
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private PatientProfileResponse toPatientResponse(Patient p) {
        ensureMrn(p);
        Users user = p.getUser();
        return PatientProfileResponse.builder()
                .id(p.getId())
                .userId(user != null ? user.getId() : null)
                .medicalRecordNumber(p.getMedicalRecordNumber())
                .name(p.getName())
                .age(p.getAge())
                .gender(p.getGender())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .build();
    }

    /** Backfill MRN for patients created before the column existed. */
    private void ensureMrn(Patient p) {
        if (p.getMedicalRecordNumber() == null || p.getMedicalRecordNumber().isBlank()) {
            p.assignMrnIfMissing();
            patientRepository.save(p);
        }
    }

    private MedicalRecordResponse toRecordResponse(MedicalRecord record) {
        var doctor = record.getDoctor();
        List<DrugDTO> drugs = record.getDrugs() == null ? List.of()
                : record.getDrugs().stream().map(this::toDrugDTO).toList();
        return MedicalRecordResponse.builder()
                .id(record.getId())
                .diagnosis(record.getDiagnosis())
                .treatmentPlan(record.getTreatmentPlan())
                .doctorName(doctor != null ? doctor.getName() : null)
                .doctorSpecialization(doctor != null ? doctor.getSpecialization() : null)
                .drugs(drugs)
                .createdAt(record.getCreatedAt())
                .build();
    }

    private BillingResponse toBillingResponse(Billing b, Receipt receipt) {
        MedicalRecord record = b.getRecord();
        Patient patient = b.getPatient();
        BillingStatus status = b.getStatus() == null ? BillingStatus.PENDING : b.getStatus();
        return BillingResponse.builder()
                .id(b.getId())
                .totalCost(b.getTotalCost())
                .description(b.getDescription())
                .createdAt(b.getCreatedAt())
                .recordId(record != null ? record.getId() : null)
                .recordDiagnosis(record != null ? record.getDiagnosis() : null)
                .patientName(patient != null ? patient.getName() : null)
                .status(status)
                .paymentMethod(b.getPaymentMethod())
                .paidAt(b.getPaidAt())
                .receipt(receipt != null ? toReceiptDTO(receipt) : null)
                .build();
    }

    private ReceiptDTO toReceiptDTO(Receipt r) {
        return ReceiptDTO.builder()
                .id(r.getId())
                .billingId(r.getBilling() != null ? r.getBilling().getId() : null)
                .receiptNumber(r.getReceiptNumber())
                .transactionRef(r.getTransactionRef())
                .paymentMethod(r.getPaymentMethod())
                .amount(r.getAmount())
                .patientName(r.getPatientName())
                .description(r.getDescription())
                .issuedAt(r.getIssuedAt())
                .build();
    }

    private DrugDTO toDrugDTO(Drug d) {
        return DrugDTO.builder()
                .id(d.getId())
                .name(d.getName())
                .dosage(d.getDosage())
                .purpose(d.getPurpose())
                .build();
    }
}
