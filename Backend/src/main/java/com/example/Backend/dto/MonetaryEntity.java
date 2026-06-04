package com.example.Backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonetaryEntity {
    private BigDecimal amount;
    private String currency;
    /** "per day", "per visit", "per claim", "total", or null. */
    private String unit;
    /** Short text immediately around the amount — used to associate it with a topic. */
    private String surroundingContext;
    /** True if the surrounding text identifies this as a policy cap/limit. */
    private boolean isLimit;
}
