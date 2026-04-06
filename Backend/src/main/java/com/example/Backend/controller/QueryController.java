package com.example.Backend.controller;

import com.example.Backend.dto.QueryRequest;
import com.example.Backend.dto.QueryResponse;
import com.example.Backend.service.agent.AgentOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/query")
@RequiredArgsConstructor
@Slf4j
public class QueryController {

    private final AgentOrchestrator agentOrchestrator;

    /**
     * Main Q&A endpoint.
     * Runs the full agentic pipeline: intent → plan → search → LLM → answer.
     */
    @PostMapping
    public ResponseEntity<QueryResponse> query(
            @Valid @RequestBody QueryRequest request,
            Authentication authentication) {

        log.info("Query from [{}]: '{}'", authentication.getName(), request.getQuery());
        return ResponseEntity.ok(agentOrchestrator.processQuery(request));
    }

    /**
     * Debug endpoint — identical to /api/query but forces includeSteps=true
     * so the full agent execution trace is included in the response.
     */
    @PostMapping("/debug")
    public ResponseEntity<QueryResponse> queryWithTrace(
            @Valid @RequestBody QueryRequest request,
            Authentication authentication) {

        log.info("Debug query from [{}]: '{}'", authentication.getName(), request.getQuery());
        request.setIncludeSteps(true);
        return ResponseEntity.ok(agentOrchestrator.processQuery(request));
    }
}