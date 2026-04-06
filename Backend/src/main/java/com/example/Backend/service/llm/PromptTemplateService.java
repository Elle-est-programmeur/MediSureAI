package com.example.Backend.service.llm;

import com.example.Backend.model.QueryIntent;
import org.springframework.stereotype.Service;

@Service
public class PromptTemplateService {

    /**
     * Asks the LLM to classify a query into one of the known intents.
     * The response is strictly structured JSON so it can be parsed reliably.
     */
    public String buildIntentDetectionPrompt(String query) {
        return """
                Classify the following user query into EXACTLY ONE of these intents:

                1. COVERAGE_QUESTION      – "What does my insurance cover?"
                2. CLAIM_STATUS           – "What's the status of my claim?"
                3. POLICY_DETAILS         – "What's my deductible / premium / limit?"
                4. MEDICAL_INTERPRETATION – "What does this diagnosis or medical term mean?"
                5. BILLING_INQUIRY        – "Why was I charged this amount?"
                6. GENERAL_INFO           – General healthcare or insurance information
                7. UNKNOWN                – Cannot determine intent

                User Query: "%s"

                Respond ONLY with a valid JSON object — no markdown, no explanation:
                {
                  "intent": "COVERAGE_QUESTION",
                  "confidence": 0.95,
                  "reasoning": "User is asking about dental coverage"
                }
                """.formatted(query);
    }

    /**
     * Builds the grounded-answer prompt given retrieved context and intent.
     * Intent-specific instructions are appended to sharpen the answer style.
     */
    public String buildAnswerPrompt(String query, String context, QueryIntent intent) {
        String intentGuidance = switch (intent) {
            case COVERAGE_QUESTION ->
                    "Clearly state what IS covered and what is NOT covered. Mention any conditions or limits.";
            case POLICY_DETAILS ->
                    "Quote specific numbers, percentages, and date ranges directly from the context.";
            case BILLING_INQUIRY ->
                    "Explain each charge line by line if possible. Mention applicable codes or rates.";
            case MEDICAL_INTERPRETATION ->
                    "Define the term clearly, then explain how it relates to the insurance context.";
            case CLAIM_STATUS ->
                    "Describe the current status and any required next steps.";
            default ->
                    "Provide a clear, factual answer based on the context.";
        };

        return """
                You are a helpful AI assistant specialized in healthcare insurance.

                Context retrieved from relevant policy documents:
                ---
                %s
                ---

                User Question: %s

                Instructions:
                1. Answer ONLY from the context above — do not use prior knowledge.
                2. If the answer is not in the context, say: "I don't have enough information in the uploaded documents to answer that."
                3. Cite the relevant part of the context in your answer (quote briefly).
                4. %s
                5. Be concise, accurate, and professional.

                Answer:""".formatted(context, query, intentGuidance);
    }

    /**
     * Asks the LLM to produce a structured task execution plan.
     * Returns strict JSON so it can be deserialized into a TaskPlan.
     */
    public String buildTaskPlanningPrompt(String query, QueryIntent intent) {
        return """
                Given the following user query and detected intent, produce a minimal task execution plan.

                User Query: "%s"
                Detected Intent: %s

                Available tools:
                - VECTOR_SEARCH  : Retrieves semantically similar document chunks
                - SQL_QUERY      : Queries structured metadata (document info, user data)

                Pipeline steps available (always include CONTEXT_BUILDING and LLM_REASONING at the end):
                - INTENT_DETECTION, TASK_PLANNING, VECTOR_SEARCH, SQL_QUERY, CONTEXT_BUILDING, LLM_REASONING

                Respond ONLY with valid JSON — no markdown, no explanation:
                {
                  "steps": ["VECTOR_SEARCH", "CONTEXT_BUILDING", "LLM_REASONING"],
                  "parameters": { "topK": 5 },
                  "reasoning": "Need to retrieve policy chunks for coverage question"
                }
                """.formatted(query, intent.name());
    }
}
