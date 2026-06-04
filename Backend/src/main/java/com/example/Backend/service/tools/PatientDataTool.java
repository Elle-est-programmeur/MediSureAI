package com.example.Backend.service.tools;

import com.example.Backend.dto.BillingResponse;
import com.example.Backend.dto.DrugDTO;
import com.example.Backend.dto.MedicalRecordResponse;
import com.example.Backend.dto.PatientDataContext;
import com.example.Backend.dto.ToolContext;
import com.example.Backend.dto.ToolResult;
import com.example.Backend.model.Billing;
import com.example.Backend.model.Doctor;
import com.example.Backend.model.Drug;
import com.example.Backend.model.MedicalRecord;
import com.example.Backend.model.Patient;
import com.example.Backend.model.ToolType;
import com.example.Backend.repository.BillingRepository;
import com.example.Backend.repository.MedicalRecordRepository;
import com.example.Backend.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Read-only tool: pulls structured clinical and billing data for the authenticated
 * patient and serializes it as plain text for the LLM prompt.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PatientDataTool implements Tool {

    private static final int MAX_RECORDS = 3;
    private static final int MAX_BILLING = 5;

    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BillingRepository billingRepository;

    @Override
    public ToolType getType() {
        return ToolType.PATIENT_DATA;
    }

    @Override
    @Transactional(readOnly = true)
    public ToolResult execute(ToolContext context) {
        long start = System.currentTimeMillis();
        try {
            Long userId = context.getPatientUserId();
            if (userId == null) {
                return ToolResult.builder()
                        .toolType(ToolType.PATIENT_DATA)
                        .success(false)
                        .content("No authenticated user — patient data unavailable.")
                        .confidence(0.0)
                        .executionTimeMs(System.currentTimeMillis() - start)
                        .build();
            }

            Optional<Patient> patientOpt = patientRepository.findByUserId(userId);
            if (patientOpt.isEmpty()) {
                return ToolResult.builder()
                        .toolType(ToolType.PATIENT_DATA)
                        .success(true)
                        .content("No patient profile found for this user.")
                        .confidence(0.0)
                        .executionTimeMs(System.currentTimeMillis() - start)
                        .build();
            }

            Patient patient = patientOpt.get();

            List<MedicalRecord> records =
                    medicalRecordRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                            .stream()
                            .limit(MAX_RECORDS)
                            .toList();

            List<Billing> billing =
                    billingRepository.findByPatientIdOrderByCreatedAtDesc(patient.getId())
                            .stream()
                            .limit(MAX_BILLING)
                            .toList();

            PatientDataContext data = PatientDataContext.builder()
                    .patientName(patient.getName())
                    .age(patient.getAge())
                    .gender(patient.getGender())
                    .records(records.stream().map(this::toRecordResponse).toList())
                    .billingHistory(billing.stream().map(this::toBillingResponse).toList())
                    .build();

            return ToolResult.builder()
                    .toolType(ToolType.PATIENT_DATA)
                    .success(true)
                    .content(data.toContextString())
                    .confidence(1.0)
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();

        } catch (Exception ex) {
            log.error("Patient data tool failed: {}", ex.getMessage(), ex);
            return ToolResult.builder()
                    .toolType(ToolType.PATIENT_DATA)
                    .success(false)
                    .errorMessage(ex.getMessage())
                    .executionTimeMs(System.currentTimeMillis() - start)
                    .build();
        }
    }

    @Override
    public boolean isApplicable(ToolContext context) {
        return context.getPatientUserId() != null;
    }

    private MedicalRecordResponse toRecordResponse(MedicalRecord record) {
        Doctor doctor = record.getDoctor();
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
                .status(b.getStatus())
                .paymentMethod(b.getPaymentMethod())
                .paidAt(b.getPaidAt())
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