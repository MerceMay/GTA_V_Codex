package com.mercemay.aiagent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class AppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;


    public AppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:/documents/*.md");
            for (Resource resource : resources) {
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(false) // Do not create new documents on horizontal rules
                        .withIncludeCodeBlock(false) // Ignore code blocks
                        .withIncludeBlockquote(false) // Ignore blockquotes
                        .withAdditionalMetadata("status", "active") // Set status metadata
                        .withAdditionalMetadata("game", "gta_v") // Set game metadata
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.read());
                log.info("Loaded {} markdown documents from classpath:/documents/", allDocuments.size());
            }
        } catch (IOException e) {
            log.error("Error loading documents: {}", e.getMessage(), e);
        }
        return allDocuments;
    }
}