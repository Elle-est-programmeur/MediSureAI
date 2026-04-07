package com.example.Backend.service.tools;

import com.example.Backend.dto.ToolContext;
import com.example.Backend.dto.ToolResult;
import com.example.Backend.model.ToolType;

public interface Tool {
    ToolType getType();
    ToolResult execute(ToolContext context);
    boolean isApplicable(ToolContext context);
}