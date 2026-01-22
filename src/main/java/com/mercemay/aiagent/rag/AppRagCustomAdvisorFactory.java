package com.mercemay.aiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

public class AppRagCustomAdvisorFactory {
    /**
     * Create a custom Retrieval Augmentation Advisor.
     *
     * @param vectorStore Vector store to use
     * @return Custom Retrieval Augmentation Advisor
     */
    public static Advisor createAppRagCustomAdvisor(VectorStore vectorStore) {
        Filter.Expression expression = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("status"),
                new Filter.Value("active")
        );

        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .topK(5)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
}