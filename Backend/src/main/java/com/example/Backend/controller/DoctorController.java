package com.example.Backend.controller;

import com.example.Backend.dto.BillingRequest;
import com.example.Backend.dto.BillingResponse;
import com.example.Backend.dto.DoctorProfileRequest;
import com.example.Backend.dto.DoctorProfileResponse;
import com.example.Backend.dto.MedicalRecordRequest;
import com.example.Backend.dto.MedicalRecordResponse;
import com.example.Backend.dto.PatientProfileResponse;
import com.example.Backend.model.Doctor;
import com.example.Backend.model.Users;
import com.example.Backend.repository.UserCredRepo;
import com.example.Backend.service.DoctorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/doctor")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('DOCTOR')")
public class DoctorController {

    private final DoctorService doctorService;
    private final UserCredRepo userCredRepo;

    // ── Profile ──────────────────────────────────────────────────────────────

    @PostMapping("/profile")
    public ResponseEntity<DoctorProfileResponse> createOrUpdateProfile(
            @Valid @RequestBody DoctorProfileRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.createOrUpdateProfile(userId, request));
    }

    @GetMapping("/profile")
    public ResponseEntity<DoctorProfileResponse> getProfile(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        Doctor doctor = doctorService.getProfile(userId);
        return ResponseEntity.ok(doctorService.toDoctorResponse(doctor));
    }

    // ── Patients ─────────────────────────────────────────────────────────────

    @GetMapping("/patients")
    public ResponseEntity<List<PatientProfileResponse>> getMyPatients(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.getMyPatients(userId));
    }

    @GetMapping("/patients/lookup")
    public ResponseEntity<PatientProfileResponse> lookupPatientByMrn(@RequestParam String mrn) {
        return ResponseEntity.ok(doctorService.lookupPatientByMrn(mrn));
    }

    @GetMapping("/patients/{userId}/records")
    public ResponseEntity<List<MedicalRecordResponse>> getPatientHistory(@PathVariable Long userId) {
        return ResponseEntity.ok(doctorService.getPatientHistory(userId));
    }

    // ── Medical records ──────────────────────────────────────────────────────

    @PostMapping("/records")
    public ResponseEntity<MedicalRecordResponse> createMedicalRecord(
            @Valid @RequestBody MedicalRecordRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.createMedicalRecord(userId, request));
    }

    @GetMapping("/records/{id}")
    public ResponseEntity<MedicalRecordResponse> getMedicalRecord(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.getMedicalRecord(id, userId));
    }

    @GetMapping("/records")
    public ResponseEntity<List<MedicalRecordResponse>> getMyRecords(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.getMyRecords(userId));
    }

    @PutMapping("/records/{id}")
    public ResponseEntity<MedicalRecordResponse> updateMedicalRecord(
            @PathVariable Long id,
            @Valid @RequestBody MedicalRecordRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.updateMedicalRecord(id, userId, request));
    }

    @DeleteMapping("/records/{id}")
    public ResponseEntity<Void> deleteMedicalRecord(
            @PathVariable Long id,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        doctorService.deleteMedicalRecord(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ── Billing ──────────────────────────────────────────────────────────────

    @PostMapping("/billing")
    public ResponseEntity<BillingResponse> createBilling(
            @Valid @RequestBody BillingRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.createBilling(userId, request));
    }

    @GetMapping("/billing")
    public ResponseEntity<List<BillingResponse>> getMyBilling(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(doctorService.getMyBilling(userId));
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private Long resolveUserId(Authentication authentication) {
        Users user = userCredRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
        return user.getId();
    }
}