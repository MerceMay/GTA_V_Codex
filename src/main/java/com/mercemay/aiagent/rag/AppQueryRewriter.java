package com.mercemay.aiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.transformation.QueryTransformer;
import org.springframework.ai.rag.preretrieval.query.transformation.RewriteQueryTransformer;
import org.springframework.stereotype.Component;

@Component
public class AppQueryRewriter {
    private final QueryTransformer queryTransformer;

    public AppQueryRewriter(ChatModel chatModel) {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel);
        queryTransformer = RewriteQueryTransformer.builder()
                .chatClientBuilder(chatClientBuilder)
                .build();
    }

    public String rewriteQuery(String originalQuery) {
        return queryTransformer.transform(new Query(originalQuery)).text();
    }
}
