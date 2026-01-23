package com.mercemay.aiagent.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.rag.Query;
import org.springframework.ai.rag.preretrieval.query.expansion.MultiQueryExpander;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppMultiQueryExpander {
    private final ChatClient.Builder chatClientBuilder;

    public AppMultiQueryExpander(ChatModel chatModel) {
        this.chatClientBuilder = ChatClient.builder(chatModel);

    }

    public List<Query> expandQuery(String userQuery, int numQueries) {
        MultiQueryExpander multiQueryExpander = MultiQueryExpander.builder()
                .chatClientBuilder(chatClientBuilder)
                .numberOfQueries(numQueries)
                .build();
        return multiQueryExpander.expand(new Query(userQuery));
    }
}
