package com.mercemay.aiagent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

@Configuration
@Slf4j
public class AppVectorStoreConfig {
    @Resource
    private AppDocumentLoader appDocumentLoader;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Bean
    public SmartInitializingSingleton databaseInitializer(VectorStore vectorStore) {
        return () -> {
            Boolean alreadyInitialized = stringRedisTemplate.hasKey("app:vector:initialized");
            if (Boolean.FALSE.equals(alreadyInitialized)) {
                List<Document> documents = appDocumentLoader.loadDocuments();
                if (documents != null && !documents.isEmpty()) {
                    vectorStore.add(documents);
                    log.info("Vector store initialized with {} documents", documents.size());
                } else {
                    log.warn("No documents found to add to vector store");
                }
                stringRedisTemplate.opsForValue().set("app:vector:initialized", "true");
            } else {
                log.info("Vector store already initialized, skipping loading");
            }
        };
    }
}
