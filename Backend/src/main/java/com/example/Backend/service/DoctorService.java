package com.example.Backend.service;

import com.example.Backend.dto.BillingRequest;
import com.example.Backend.dto.BillingResponse;
import com.example.Backend.dto.DoctorProfileRequest;
import com.example.Backend.dto.DoctorProfileResponse;
import com.example.Backend.dto.DrugDTO;
import com.example.Backend.dto.MedicalRecordRequest;
import com.example.Backend.dto.MedicalRecordResponse;
import com.example.Backend.dto.PatientProfileResponse;
import com.example.Backend.model.Billing;
import com.example.Backend.model.Doctor;
import com.example.Backend.model.Drug;
import com.example.Backend.model.MedicalRecord;
import com.example.Backend.model.Patient;
import com.example.Backend.model.Users;
import com.example.Backend.repository.BillingRepository;
import com.example.Backend.repository.DoctorRepository;
import com.example.Backend.repository.MedicalRecordRepository;
import com.example.Backend.repository.PatientRepository;
import com.example.Backend.repository.UserCredRepo;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure-CRUD doctor-side workflows. Intentionally has zero dependencies on
 * SRLM / agent / LLM / vector code — DB-only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final BillingRepository billingRepository;
    private final UserCredRepo userCredRepo;

    // ── Profile ───────────────────────────────────────────────────────────────

    @Transactional
    public DoctorProfileResponse createOrUpdateProfile(Long userId, DoctorProfileRequest request) {
        Users user = userCredRepo.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        Doctor doctor = doctorRepository.findByUserId(userId)
                .orElseGet(() -> Doctor.builder().user(user).build());

        doctor.setName(request.getName());
        doctor.setSpecialization(request.getSpecialization());
        doctor.setHospitalId(request.getHospitalId());

        Doctor saved = doctorRepository.save(doctor);
        return toDoctorResponse(saved);
    }

    @Transactional(readOnly = true)
    public Doctor getProfile(Long userId) {
        return doctorRepository.findByUserId(userId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found for user " + userId));
    }

    public DoctorProfileResponse toDoctorResponse(Doctor doctor) {
        Users user = doctor.getUser();
        return DoctorProfileResponse.builder()
                .id(doctor.getId())
                .name(doctor.getName())
                .specialization(doctor.getSpecialization())
                .hospitalId(doctor.getHospitalId())
                .username(user != null ? user.getUsername() : null)
                .email(user != null ? user.getEmail() : null)
                .build();
    }

    // ── Medical records ───────────────────────────────────────────────────────

    @Transactional
    public MedicalRecordResponse createMedicalRecord(Long doctorUserId, MedicalRecordRequest request) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found for user " + doctorUserId));

        Patient patient = patientRepository.findByUserId(request.getPatientUserId())
                .orElseThrow(() -> new EntityNotFoundException(
                        "Patient profile not found for user " + request.getPatientUserId()));

        MedicalRecord record = MedicalRecord.builder()
                .patient(patient)
                .doctor(doctor)
                .diagnosis(request.getDiagnosis())
                .treatmentPlan(request.getTreatmentPlan())
                .drugs(new ArrayList<>())
                .build();

        if (request.getDrugs() != null) {
            for (DrugDTO d : request.getDrugs()) {
                Drug drug = Drug.builder()
                        .record(record)
                        .name(d.getName())
                        .dosage(d.getDosage())
                        .purpose(d.getPurpose())
                        .build();
                record.getDrugs().add(drug);
            }
        }

        MedicalRecord saved = medicalRecordRepository.save(record);
        log.info("Doctor[{}] created MedicalRecord[{}] for Patient[{}]",
                doctorUserId, saved.getId(), patient.getId());
        return toRecordResponse(saved);
    }

    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecord(Long recordId, Long doctorUserId) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Medical record not found: " + recordId));
        assertOwnership(record, doctorUserId);
        return toRecordResponse(record);
    }

    @Transactional
    public MedicalRecordResponse updateMedicalRecord(Long recordId, Long doctorUserId, MedicalRecordRequest request) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Medical record not found: " + recordId));
        assertOwnership(record, doctorUserId);

        record.setDiagnosis(request.getDiagnosis());
        record.setTreatmentPlan(request.getTreatmentPlan());

        // Replace drugs wholesale (orphanRemoval=true on the relationship cleans up the old set)
        record.getDrugs().clear();
        if (request.getDrugs() != null) {
            for (DrugDTO d : request.getDrugs()) {
                Drug drug = Drug.builder()
                        .record(record)
                        .name(d.getName())
                        .dosage(d.getDosage())
                        .purpose(d.getPurpose())
                        .build();
                record.getDrugs().add(drug);
            }
        }

        MedicalRecord saved = medicalRecordRepository.save(record);
        return toRecordResponse(saved);
    }

    @Transactional
    public void deleteMedicalRecord(Long recordId, Long doctorUserId) {
        MedicalRecord record = medicalRecordRepository.findById(recordId)
                .orElseThrow(() -> new EntityNotFoundException("Medical record not found: " + recordId));
        assertOwnership(record, doctorUserId);
        medicalRecordRepository.delete(record);
    }

    // ── Patients seen by this doctor ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<PatientProfileResponse> getMyPatients(Long doctorUserId) {
        Doctor doctor = doctorRepository.findByUserId(doctorUserId)
                .orElseThrow(() -> new EntityNotFoundException("Doctor profile not found for user " + doctorUserId));

        return medicalRecordRepository.findByDoctorId(doctor.getId()).stream()
                .map(MedicalRecord::getPatient)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toMap(
                        Patient::getId, p -> p, (a, b) -> a, java.util.LinkedHashMap::new))
                .values().stream()
                .map(this::toPatientResponse)
                .toList();
    }

    // ── Billing ───────────────────────────────────────────────────────────────

    @Transactional
    public BillingResponse createBilling(Long doctorUserId, BillingRequest request) {
        // Resolve patient via the linked record (records carry the doctor → patient link).
        if (request.getRecordId() == null) {
            throw new EntityNotFoundException("Billing must be tied to a medical record (recordId is required)");
        }

        MedicalRecord record = medicalRecordRepository.findById(request.getRecordId())
                .orElseThrow(() -> new EntityNotFoundException("Medical record not found: " + request.getRecordId()));
        assertOwnership(record, doctorUserId);

        Billing billing = Billing.builder()
                .patient(record.getPatient())
                .record(record)
                .totalCost(request.getTotalCost())
                .description(request.getDescription())
                .build();

        Billing saved = billingRepository.save(billing);
        log.info("Doctor[{}] created Billing[{}] for Patient[{}] on Record[{}]",
                doctorUserId, saved.getId(), record.getPatient().getId(), record.getId());
        return toBillingResponse(saved);
    }

    // ── Internals ─────────────────────────────────────────────────────────────

    private void assertOwnership(MedicalRecord record, Long doctorUserId) {
        Doctor doctor = record.getDoctor();
        if (doctor == null || doctor.getUser() == null
                || !doctorUserId.equals(doctor.getUser().getId())) {
            throw new AccessDeniedException("You do not own this medical record");
        }
    }

    private MedicalRecordResponse toRecordResponse(MedicalRecord record) {
        Doctor doctor = record.getDoctor();
        List<DrugDTO> drugs = record.getDrugs() == null ? List.of()
                : record.getDrugs().stream()
                        .map(d -> DrugDTO.builder()
                                .id(d.getId())
                                .name(d.getName())
                                .dosage(d.getDosage())
                                .purpose(d.getPurpose())
                                .build())
                        .toList();
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
}