package com.mercemay.aiagent.app;

import com.mercemay.aiagent.rag.AppRagCustomAdvisorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


@Slf4j
@Component
public class App {
    private final ChatClient chatClient;
    private final ChatModel chatModel;
    private final Resource systemPromptResource;
    private final VectorStore vectorStore;

    /**
     * Initialize the ChatClient with the specified ChatModel and system prompt.
     *
     * @param chatModel Chat model to use
     */
    public App(ChatModel chatModel,
               ChatMemory chatMemory,
               VectorStore vectorStore,
               @Value("classpath:/prompts/system_prompt.st") Resource systemPrompt) {
        this.chatModel = chatModel;
        this.systemPromptResource = systemPrompt;
        this.vectorStore = vectorStore;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(this.systemPromptResource)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(), // Chat memory advisor
                        new SimpleLoggerAdvisor()
                        // new MyLoggerAdvisor() // Logging advisor
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

    /**
     * Chat with RAG (Retrieval Augmented Generation) support.
     * First classifies the user's question to determine the appropriate document tag,
     * then uses that tag to filter relevant documents for the response.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response augmented with relevant document content
     */
    public String chatWithRAG(String message, String chatId) {
        // Step 2: Use the classified tag to filter documents and generate response
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(
                        AppRagCustomAdvisorFactory.createAppRagCustomAdvisor(vectorStore)
                )
                .call()
                .chatResponse();
        String responseText = chatResponse.getResult().getOutput().getText();
        log.info("chatWithRAG responseText: {}", responseText);
        return responseText;
    }

}
