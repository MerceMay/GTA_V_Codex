package com.mercemay.aiagent.rag;

import com.mercemay.aiagent.constant.TagConstant;
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
     * @param tag         Tag to filter documents by (should be a TagConstant value)
     * @return Custom Retrieval Augmentation Advisor
     */
    public static Advisor createAppRagCustomAdvisor(VectorStore vectorStore, String tag) {
        Filter.Expression expression = new Filter.Expression(
                Filter.ExpressionType.AND,
                new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(TagConstant.METADATA_KEY_STATUS), new Filter.Value(TagConstant.STATUS_ACTIVE)),
                new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key(TagConstant.METADATA_KEY_TAG), new Filter.Value(tag))
        );
        VectorStoreDocumentRetriever documentRetriever = VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .filterExpression(expression)
                .topK(5)
                .similarityThreshold(0.75)
                .build();

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(documentRetriever)
                .build();
    }
}
