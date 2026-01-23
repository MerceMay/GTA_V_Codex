package com.mercemay.aiagent.rag;

import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.DocumentRetriever;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;

public class AppRagCustomAdvisorFactory {
    /**
     * Create a custom Retrieval Augmentation Advisor.
     *
     * @param vectorStore Vector store to use
     * @return Custom Retrieval Augmentation Advisor
     */
    public static Advisor createAppRagCustomAdvisor(VectorStore vectorStore, AppMultiQueryExpander expander) {
        Filter.Expression expression = new Filter.Expression(
                Filter.ExpressionType.EQ,
                new Filter.Key("status"),
                new Filter.Value("active")
        ); // Only retrieve documents with status "active"


        DocumentRetriever multiQueryRetriever = (query) -> {
            List<Query> expandedQueries = expander.expandQuery(query.text(), 3); // Expand into 3 queries

            return expandedQueries.stream()
                    .flatMap(q -> vectorStore.similaritySearch(
                                    SearchRequest.builder()
                                            .query(q.text())
                                            .topK(5) // Retrieve top 5 documents per expanded query
                                            .filterExpression(expression)
                                            .similarityThreshold(0.75) // Similarity threshold
                                            .build())
                            .stream()
                    ).distinct() // Remove duplicate documents
                    .toList();
        };

        return RetrievalAugmentationAdvisor.builder()
                .documentRetriever(multiQueryRetriever)
                .queryAugmenter(AppContextualQueryAugmenterFactory.createContextualQueryAugmenter())
                .build();
    }
}