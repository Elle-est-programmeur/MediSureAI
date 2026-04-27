package com.example.Backend.controller;

import com.example.Backend.dto.*;
import com.example.Backend.model.Users;
import com.example.Backend.repository.UserCredRepo;
import com.example.Backend.service.PatientService;
import com.example.Backend.service.agent.AgentOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patient")
@RequiredArgsConstructor
@Slf4j
public class PatientController {

    private final PatientService patientService;
    private final UserCredRepo userCredRepo;
    private final AgentOrchestrator agentOrchestrator;


    /**
     * Feature 2: Health Timeline
     */
    @GetMapping("/timeline")
    public ResponseEntity<List<DocumentMetadataDTO>> getTimeline(Authentication authentication) {
        Users user = resolveUser(authentication);
        return ResponseEntity.ok(patientService.getTimeline(user));
    }

    /**
     * Feature 4: Smart Formulary Search
     */
    @GetMapping("/formulary-search")
    public ResponseEntity<SRLMResponse> searchFormulary(
            @RequestParam String drug,
            Authentication authentication) {

        Users user = resolveUser(authentication);
        return ResponseEntity.ok(patientService.searchFormulary(drug, user));
    }

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileResponse> getProfile(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(patientService.getProfile(userId));
    }

    @PostMapping("/profile")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<PatientProfileResponse> createOrUpdateProfile(
            @Valid @RequestBody PatientProfileRequest request,
            Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(patientService.createOrUpdateProfile(userId, request));
    }

    // ── Records & billing (read-only) ────────────────────────────────────────

    @GetMapping("/records")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<MedicalRecordResponse>> getMyRecords(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(patientService.getMyRecords(userId));
    }

    @GetMapping("/billing")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<List<BillingResponse>> getMyBilling(Authentication authentication) {
        Long userId = resolveUserId(authentication);
        return ResponseEntity.ok(patientService.getMyBilling(userId));
    }

    // ── Patient AI chat ──────────────────────────────────────────────────────

    @PostMapping("/chat")
    @PreAuthorize("hasRole('PATIENT')")
    public ResponseEntity<QueryResponse> chat(
            @Valid @RequestBody PatientChatRequest request,
            Authentication authentication) {

        Long userId = resolveUserId(authentication);

        QueryRequest queryRequest = new QueryRequest();
        queryRequest.setQuery(request.getQuery());
        queryRequest.setSessionId(request.getSessionId());
        queryRequest.setPatientUserId(userId);

        log.info("Patient chat from [userId={}]: '{}'", userId, request.getQuery());
        return ResponseEntity.ok(agentOrchestrator.processQuery(queryRequest));
    }

    // ── Internals ────────────────────────────────────────────────────────────

    private Users resolveUser(Authentication authentication) {
        return userCredRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }

    private Long resolveUserId(Authentication authentication) {
        return resolveUser(authentication).getId();
    }
}