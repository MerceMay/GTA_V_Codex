package com.mercemay.aiagent.tools;

import com.mercemay.aiagent.manager.WorkspaceManager;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolRegistration {
    @Value("${websearch.api-key}")
    private String apiKey;
    @Value("${websearch.search-url}")
    private String searchUrl;

    @Bean
    public ToolCallback[] toolCallbacks(WorkspaceManager workspaceManager) {
        return ToolCallbacks.from(
                new FileOperationTool(workspaceManager),
                new PdfGeneratorTool(workspaceManager),
                new ResourceDownloadTool(workspaceManager),
                new TerminalOperationTool(),
                new TerminateAgentTool(),
                new WebSearchTool(apiKey, searchUrl),
                new WebCrawlerTool()
        );
    }
}
