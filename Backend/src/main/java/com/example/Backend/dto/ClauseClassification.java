package com.example.Backend.dto;

import com.example.Backend.model.ClauseCategory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClauseClassification {
    private String chunkId;
    private ClauseCategory category;
    /** [0,1] confidence of the classification (higher = more deterministic match). */
    private double confidence;
    /** Lowercase cues that triggered this classification — for debug/observability. */
    private List<String> matchedCues;
    private String summary;
}
