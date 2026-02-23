package com.example.Backend.controller;

import com.example.Backend.dto.QuestionRequest;
import com.example.Backend.dto.QuestionResponse;
import com.example.Backend.service.RAGService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final RAGService ragService;

    @PostMapping("/ask")
    public ResponseEntity<QuestionResponse> askQuestion(@Valid @RequestBody QuestionRequest request) {
        log.info("Received question: {}", request.getQuestion());
        
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        
        QuestionResponse response = ragService.askQuestion(request);
        return ResponseEntity.ok(response);
    }
}
