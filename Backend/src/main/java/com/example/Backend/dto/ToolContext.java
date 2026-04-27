package com.example.Backend.dto;

import com.example.Backend.model.QueryIntent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolContext {
    private String query;
    private QueryIntent intent;
    private Long documentId;
    private Map<String, Object> parameters;
    private String sessionId;
    /** Authenticated user's id (Users.id). Used by tools that need per-user data. */
    private Long patientUserId;
}