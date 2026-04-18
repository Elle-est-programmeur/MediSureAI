package com.example.Backend.controller;

import com.example.Backend.dto.*;
import com.example.Backend.model.Users;
import com.example.Backend.repository.UserCredRepo;
import com.example.Backend.service.PatientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    private Users resolveUser(Authentication authentication) {
        return userCredRepo.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Authenticated user not found in database"));
    }
}
