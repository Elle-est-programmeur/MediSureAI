# Phase 6 — Production Enhancements

## Overview
Phase 6 adds five production-grade capabilities on top of the Phases 1-5 foundation.

---

## 1. Tool Orchestration Layer

Abstracts all data-access operations behind a `Tool` interface, enabling dynamic selection and
parallel extensibility without touching orchestrator logic.

### Components
| Class | Role |
|-------|------|
| `model/ToolType.java` | Enum of available tool types |
| `service/tools/Tool.java` | Interface: `execute`, `isApplicable`, `getType` |
| `dto/ToolContext.java` | Input carrier: query, intent, documentId, parameters, sessionId |
| `dto/ToolResult.java` | Output carrier: success, content, confidence, executionTimeMs |
| `service/tools/VectorSearchTool.java` | Wraps `SemanticSearchService`; reports avg similarity as confidence |
| `service/tools/MetadataQueryTool.java` | Queries PostgreSQL document metadata; always confidence=1.0 |
| `service/tools/ToolOrchestrator.java` | Iterates requested tools, skips non-applicable ones, joins results |

### How it works
```
AgentOrchestrator.determineTools(plan)  →  [VECTOR_SEARCH, METADATA_QUERY]
ToolOrchestrator.executeTools(ctx, tools)
  → VectorSearchTool.execute(ctx)   → ToolResult(content, confidence=0.82)
  → MetadataQueryTool.execute(ctx)  → ToolResult(content, confidence=1.0)
ToolOrchestrator.combineToolResults()  →  "chunk1\n\n---\n\nchunk2\n\n---\n\nmetadata"
```

---

## 2. Confidence Calibration

Every response now includes a calibrated confidence score so users know how much to trust the answer.

### `service/confidence/ConfidenceCalibrationService.java`

Three factors, each scaled 0-10:

| Factor | Calculation |
|--------|-------------|
| Intent detection | `intentConfidence × 10` |
| Retrieval quality | avg tool confidence × 10 |
| Context availability | `min(contextLength / 500, 1) × 10` |

**Levels:** `HIGH` ≥ 7.0 · `MEDIUM` ≥ 4.0 · `LOW` < 4.0

### Response fields added to `QueryResponse`
```json
{
  "overallConfidence": 7.4,
  "confidenceLevel": "HIGH",
  "confidenceFactors": [
    "Intent detection: 9.0/10",
    "Retrieval quality: 7.5/10",
    "Context availability: 5.6/10"
  ]
}
```

---

## 3. SRLM Retrieval Feedback Loop

When initial retrieval confidence is below `srlm.feedback.confidence-threshold` (default 6.0),
`SRLMOrchestrator` automatically re-queries with a broadened query string and more chunks (topK=8).

```
context = vectorSearch(query)
if confidence(context) < 6.0:
    refinedQuery = query + " policy coverage terms conditions"
    enhancedContext = vectorSearch(refinedQuery, topK=8)
    if len(enhancedContext) > len(context):
        context = enhancedContext  // use richer context
```

Controlled by:
```properties
srlm.feedback.enabled=true
srlm.feedback.confidence-threshold=6.0
```

---

## 4. Safety Guardrails

`service/safety/GuardrailService.java` inspects every LLM answer before it reaches the client.

### Checks
| Check | Action |
|-------|--------|
| Uncertain language (`"I think"`, `"probably"`, …) | Warning added |
| Overall confidence < `safety.min-confidence` (4.0) | Answer replaced with fallback |
| Answer length < 50 chars | Warning + block |
| No source citations (`"according to"` / `"based on"`) | Warning only |

Warnings are surfaced in `QueryResponse.safetyWarnings` (null when empty).

---

## 5. Session Memory

`service/memory/SessionMemoryService.java` maintains an in-process conversation history per session.

### API
```java
saveQueryContext(sessionId, query, answer)   // called automatically at end of every pipeline
getSessionHistory(sessionId)                 // List<QueryRecord>
getSessionContext(sessionId, lastN)          // formatted Q&A string for LLM context
clearSession(sessionId)                      // explicit invalidation
```

### Client usage
Pass `sessionId` in the request body to link follow-up queries:
```json
{
  "query": "What is my deductible?",
  "sessionId": "user-abc-session-1"
}
```
Omit `sessionId` and a new UUID is generated automatically (stateless mode).

Memory is capped at `session.memory.max-queries` (default 10) per session.

---

## Updated Endpoints

All existing endpoints remain unchanged. The Phase 6 enhancements are transparent — the same
`POST /api/query` and `POST /api/srlm` now return richer responses.

### `POST /api/query` — enhanced response
```json
{
  "query": "Does my plan cover physiotherapy?",
  "answer": "Based on your policy, physiotherapy is covered up to 20 sessions per year...",
  "detectedIntent": "COVERAGE_QUESTION",
  "intentConfidence": 0.92,
  "overallConfidence": 7.4,
  "confidenceLevel": "HIGH",
  "confidenceFactors": ["Intent detection: 9.2/10", "Retrieval quality: 8.0/10", "Context availability: 5.0/10"],
  "safetyWarnings": null,
  "processingTimeMs": 1234,
  "model": "gpt-4o-mini"
}
```

---

## Configuration Reference

```properties
# Tool Orchestration
tools.vector-search.enabled=true
tools.metadata-query.enabled=true

# Confidence Calibration
confidence.min-threshold=4.0
confidence.high-threshold=7.0

# SRLM Feedback Loop
srlm.feedback.enabled=true
srlm.feedback.confidence-threshold=6.0

# Guardrails
safety.hallucination-detection=true
safety.min-confidence=4.0

# Session Memory
session.memory.enabled=true
session.memory.max-queries=10
session.memory.ttl-minutes=60
```