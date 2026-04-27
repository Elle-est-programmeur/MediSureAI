package com.example.Backend.service;

import com.example.Backend.dto.*;
import com.example.Backend.model.Billing;
import com.example.Backend.model.Document;
import com.example.Backend.model.DocumentType;
import com.example.Backend.model.Drug;
import com.example.Backend.model.MedicalRecord;
import com.example.Backend.model.Patient;
import com.example.Backend.model.QueryIntent;
import com.example.Backend.model.Users;
import com.example.Backend.repository.BillingRepository;
import com.example.Backend.repository.DocumentRepository;
import com.example.Backend.repository.MedicalRecordRepository;
import com.example.Backend.repository.PatientRepository;
import com.example.Backend.repository.UserCredRepo;
import com.example.Backend.service.llm.LLMService;
import com.example.Backend.service.srlm.MultiReasoningService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PatientService {

    private final VectorStore vectorStore;
    private final DocumentRepository documentRepository;
    private final LLMService llmService;
    private final MultiReasoningService multiReasoningService;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BillingRepository billingRepository;
    private final UserCredRepo userCredRepo;

    /**
     * Feature 2: Health Timeline
     */
    public List<DocumentMetadataDTO> getTimeline(Users user) {
        return documentRepository.findByUser(user).stream()
                .filter(doc -> doc.getEventDate() != null)
                .sorted(Comparator.comparing(Document::getEventDate).reversed())
                .map(this::toDTO)
                .toList();
    }

    /**
     * Feature 4: Smart Formulary Search
     */
    public SRLMResponse searchFormulary(String drugName, Users user) {
        log.info("Searching formulary for drug: {}", drugName);

        List<org.springframework.ai.document.Document> policyDocs = vectorStore.similaritySearch(
                SearchRequest.query(drugName)
                        .withTopK(5)
                        .withFilterExpression("documentType == 'INSURANCE_POLICY'")
        );

        String context = policyDocs.stream().map(org.springframework.ai.document.Document::getContent).collect(Collectors.joining("\n"));

        String prompt = String.format("""
                [CRITICAL INSTRUCTION: USE PROVIDED CONTEXT ONLY]
                You are a technical Medical Insurance Policy Auditor. Your task is to extract exact coverage details for the drug '%s' from the provided policy snippets.
                
                RULES:
                1. Do NOT provide generic medical advice or "Bronze/Silver/Gold" tier guesses.
                2. If the drug is not mentioned in the context, state "Drug not found in policy documents."
                3. Use the exact wording from the text for co-pays and tiers.
                4. Do NOT start with a medical advice disclaimer unless you are actually giving medical advice (which you should not do).
                
                DETERMINE:
                - Coverage Status: Is it mentioned as covered?
                - Cost Tier & Co-pay: (e.g., Tier 1, $10)
                - Generic Alternatives: List any generics mentioned in the same section.
                
                CONTEXT FROM POLICY:
                ---
                %s
                ---
                """, drugName, context);

        String answer = llmService.generateCompletion(prompt);

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

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient profile not found for user " + userId));
        return toPatientResponse(patient);
    }

    // ── Records & billing (read-only patient view) ────────────────────────────

    @Transactional(readOnly = true)
    public List<MedicalRecordResponse> getMyRecords(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient profile not found for user " + userId));
        return medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toRecordResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BillingResponse> getMyBilling(Long userId) {
        Patient patient = patientRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient profile not found for user " + userId));
        return billingRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                .stream()
                .map(this::toBillingResponse)
                .toList();
    }

    // ── Mappers ───────────────────────────────────────────────────────────────

    private PatientProfileResponse toPatientResponse(Patient p) {
        Users user = p.getUser();
        return PatientProfileResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .age(p.getAge())
                .gender(p.getGender())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .build();
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

    private BillingResponse toBillingResponse(Billing b) {
        return BillingResponse.builder()
                .id(b.getId())
                .totalCost(b.getTotalCost())
                .description(b.getDescription())
                .createdAt(b.getCreatedAt())
                .recordId(b.getRecord() != null ? b.getRecord().getId() : null)
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

    private DocumentMetadataDTO toDTO(Document document) {
        return DocumentMetadataDTO.builder()
                .id(document.getId())
                .fileName(document.getOriginalFileName())
                .documentType(document.getDocumentType())
                .status(document.getStatus())
                .eventDate(document.getEventDate())
                .uploadedAt(document.getUploadedAt())
                .build();
    }
}
