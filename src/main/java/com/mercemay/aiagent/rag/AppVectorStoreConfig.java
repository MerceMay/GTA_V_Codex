package com.mercemay.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Slf4j
public class AppVectorStoreConfig {
    @Resource
    private AppDocumentLoader appDocumentLoader;

    @Resource
    private AppCustomTextSplitter appCustomTextSplitter;

    @Resource
    private AppDocumentKeywordEnricher appDocumentKeywordEnricher;

    @Bean
    @ConditionalOnProperty(name = "rag.initialize-vector-store", havingValue = "true", matchIfMissing = false)
    public SmartInitializingSingleton databaseInitializer(VectorStore vectorStore) {
        return () -> {
            // step 1: Load documents
            List<Document> documents = appDocumentLoader.loadDocuments();
            // step 2: Split documents into chunks
            List<Document> splitDocuments = appCustomTextSplitter.customizedSplitter(documents);
            // step 3: Enrich documents with keywords
            List<Document> enrichedDocuments = appDocumentKeywordEnricher.enrichDocuments(splitDocuments);
            if (enrichedDocuments != null && !enrichedDocuments.isEmpty()) {
                vectorStore.add(enrichedDocuments);
                log.info("Vector store initialized with {} documents", enrichedDocuments.size());
            } else {
                log.warn("No documents found to add to vector store");
            }
        };
    }
}
