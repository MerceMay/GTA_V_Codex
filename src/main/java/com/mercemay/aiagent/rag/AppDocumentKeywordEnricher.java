package com.mercemay.aiagent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.model.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppDocumentKeywordEnricher {

    @Resource
    private ChatModel chatModel;

    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(chatModel, 5); // Extract top 5 keywords
        return keywordMetadataEnricher.apply(documents);
    }

}
