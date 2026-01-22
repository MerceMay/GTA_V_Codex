package com.mercemay.aiagent.app;

import com.mercemay.aiagent.advisor.MyLoggerAdvisor;
import com.mercemay.aiagent.rag.AppRagCustomAdvisorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class App {
    private final ChatClient chatClient;
    private final Resource systemPromptResource;
    private final VectorStore vectorStore;
    private final AppRagCustomAdvisorFactory appRagCustomAdvisorFactory;

    /**
     * Initialize the ChatClient with the specified ChatModel and system prompt.
     *
     * @param chatModel Chat model to use
     */
    public App(ChatModel chatModel,
               ChatMemory chatMemory,
               VectorStore vectorStore,
               AppRagCustomAdvisorFactory appRagCustomAdvisorFactory,
               @Value("classpath:/prompts/system_prompt.st") Resource systemPrompt) {
        this.systemPromptResource = systemPrompt;
        this.vectorStore = vectorStore;
        this.appRagCustomAdvisorFactory = appRagCustomAdvisorFactory;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(this.systemPromptResource)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // Chat memory advisor
                        new MyLoggerAdvisor() // Logging advisor
                        // , new ReReadingAdvisor() // Re-reading advisor
                )
                .build();
    }


    /**
     * Chat with the AI model.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response
     */
    public String chat(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId)) // Specify conversation ID in current chat
                .call()
                .chatResponse();
        String responseText = chatResponse.getResult().getOutput().getText();
        log.info("responseText: {}", responseText);
        return responseText;
    }

    record GameRecommendation(String title, List<String> recommendations) {
    }

    /**
     * Get game recommendations from the AI model.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The game recommendations
     */
    public GameRecommendation getGameRecommendation(String message, String chatId) {
        GameRecommendation gameRecommendation = chatClient.prompt()
                .system(systemPromptResource + "每次对话后都要给出游戏推荐，标题为{用户名}的游戏推荐，内容为推荐的游戏列表。")
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(GameRecommendation.class);
        log.info("gameRecommendation: {}", gameRecommendation);
        return gameRecommendation;
    }

    public String chatWithRAG(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(
                        QuestionAnswerAdvisor.builder(vectorStore)
                                .searchRequest(SearchRequest.builder().topK(5).build())
                                .build(),
                        appRagCustomAdvisorFactory
                        )
                .call()
                .chatResponse();
        String responseText = chatResponse.getResult().getOutput().getText();
        log.info("chatWithRAG responseText: {}", responseText);
        return responseText;
    }
}
