package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimelineEventDTO {
    private String id;
    private String type;
    private String title;
    private String details;
    private String doctorName;
    private LocalDateTime date;
    private List<String> tags;
}
