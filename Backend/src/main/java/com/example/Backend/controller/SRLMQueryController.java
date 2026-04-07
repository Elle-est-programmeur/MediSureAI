package com.example.Backend.controller;

import com.example.Backend.dto.QueryRequest;
import com.example.Backend.dto.SRLMResponse;
import com.example.Backend.service.srlm.SRLMOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/srlm")
@RequiredArgsConstructor
@Slf4j
public class SRLMQueryController {

    private final SRLMOrchestrator srlmOrchestrator;

    /**
     * Main SRLM endpoint.
     * Runs the full Phase 5 pipeline:
     *   IntentDetection → VectorSearch → MultiReasoning → SelfReflection → Scoring → Synthesis
     */
    @PostMapping
    public ResponseEntity<SRLMResponse> query(
            @Valid @RequestBody QueryRequest request,
            Authentication authentication) {

        log.info("SRLM query from [{}]: '{}'", authentication.getName(), request.getQuery());
        return ResponseEntity.ok(srlmOrchestrator.processQuery(request));
    }

    /**
     * Debug endpoint — includes all reasoning candidates, reflections, and scores in the response.
     */
    @PostMapping("/debug")
    public ResponseEntity<SRLMResponse> queryWithTrace(
            @Valid @RequestBody QueryRequest request,
            Authentication authentication) {

        log.info("SRLM debug query from [{}]: '{}'", authentication.getName(), request.getQuery());
        request.setIncludeSteps(true);
        return ResponseEntity.ok(srlmOrchestrator.processQuery(request));
    }
}