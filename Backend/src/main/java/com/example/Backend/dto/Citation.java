package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Citation linking a generated claim back to the supporting evidence chunk.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Citation {
    private String chunkId;
    private String quote;
    private String sourceLabel;
    private double evidenceScore;
}
