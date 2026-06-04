package com.example.Backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class GroqFormularyService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;

    public GroqFormularyService(
            @Value("${groq.formulary.api-key}") String apiKey,
            @Value("${groq.formulary.model}") String model,
            @Value("${groq.formulary.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public String search(String drugName, String policyContext) {
        String system = """
                You are a clinical drug formulary assistant. Given a drug name (and optional
                insurance-policy snippets), return CONCISE, factual information in clearly
                labelled sections. Do not invent coverage details that are not in the policy
                snippets — if absent, say so. Avoid generic disclaimers unless you are giving
                clinical advice (which you should not).

                Output sections (use these exact headings):
                - Drug: <generic name> (<brand names, if known>)
                - Class / Category:
                - Typical Use:
                - Common Dosage Forms:
                - Coverage (from policy):
                - Generic Alternatives:
                - Notes / Cautions:
                """;

        String user = policyContext == null || policyContext.isBlank()
                ? "Drug: " + drugName
                : "Drug: " + drugName + "\n\nPolicy snippets:\n---\n" + policyContext + "\n---";

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0.2);
        body.put("max_tokens", 600);
        body.set("messages", objectMapper.valueToTree(List.of(
                Map.of("role", "system", "content", system),
                Map.of("role", "user", "content", user)
        )));

        try {
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                return "No response from formulary service.";
            }
            JsonNode content = response.path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.isNull()) {
                log.warn("Groq formulary response missing content: {}", response);
                return "Formulary service returned an empty response.";
            }
            return content.asText();
        } catch (Exception e) {
            log.error("Groq formulary call failed for drug='{}'", drugName, e);
            throw new RuntimeException("Formulary lookup failed: " + e.getMessage(), e);
        }
    }
}
