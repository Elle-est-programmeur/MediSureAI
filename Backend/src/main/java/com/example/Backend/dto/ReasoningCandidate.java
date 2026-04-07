package com.example.Backend.dto;

import com.example.Backend.model.ReasoningPath;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReasoningCandidate {

    private ReasoningPath path;
    private String answer;
    private String reasoning;
    private String approach;
}