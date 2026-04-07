package com.example.Backend.service.tools;

import com.example.Backend.dto.ToolContext;
import com.example.Backend.dto.ToolResult;
import com.example.Backend.model.ToolType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ToolOrchestrator {

    private final List<Tool> tools;

    public List<ToolResult> executeTools(ToolContext context, List<ToolType> requestedTools) {
        log.info("Executing {} tools for query", requestedTools.size());

        Map<ToolType, Tool> toolMap = tools.stream()
                .collect(Collectors.toMap(Tool::getType, Function.identity()));

        List<ToolResult> results = new ArrayList<>();

        for (ToolType toolType : requestedTools) {
            Tool tool = toolMap.get(toolType);
            if (tool == null) {
                log.warn("Tool not found: {}", toolType);
                continue;
            }
            if (!tool.isApplicable(context)) {
                log.debug("Tool {} not applicable, skipping", toolType);
                continue;
            }
            ToolResult result = tool.execute(context);
            results.add(result);
            log.info("Tool {} executed — success={}, confidence={}",
                    toolType, result.isSuccess(), result.getConfidence());
        }

        return results;
    }

    public String combineToolResults(List<ToolResult> results) {
        return results.stream()
                .filter(ToolResult::isSuccess)
                .filter(r -> r.getContent() != null && !r.getContent().isBlank())
                .map(ToolResult::getContent)
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}