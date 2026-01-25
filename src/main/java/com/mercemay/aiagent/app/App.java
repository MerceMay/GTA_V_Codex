package com.mercemay.aiagent.app;

import com.mercemay.aiagent.advisor.MyLoggerAdvisor;
import com.mercemay.aiagent.rag.AppMultiQueryExpander;
import com.mercemay.aiagent.rag.AppQueryRewriter;
import com.mercemay.aiagent.rag.AppRagCustomAdvisorFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;


@Slf4j
@Component
public class App {
    private final ChatClient chatClient;
    private final String systemPromptContent;
    private final VectorStore vectorStore;
    private final AppMultiQueryExpander appMultiQueryExpander;
    private final AppQueryRewriter appQueryRewriter;
    private final ToolCallback[] toolCallbacks;
    private final ToolCallbackProvider toolCallbackProvider;

    /**
     * Initialize the ChatClient with the specified ChatModel and system prompt.
     *
     * @param chatModel Chat model to use
     */
    public App(ChatModel chatModel,
               ChatMemory chatMemory,
               VectorStore vectorStore,
               AppMultiQueryExpander appMultiQueryExpander,
               AppQueryRewriter appQueryRewriter,
               ToolCallback[] toolCallbacks,
               ToolCallbackProvider toolCallbackProvider,
               @Value("classpath:/prompts/system_prompt.st") Resource systemPrompt) {
        try {
            this.systemPromptContent = StreamUtils.copyToString(systemPrompt.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read system prompt", e);
        }
        this.vectorStore = vectorStore;
        this.appMultiQueryExpander = appMultiQueryExpander;
        this.appQueryRewriter = appQueryRewriter;
        this.toolCallbacks = toolCallbacks;
        this.toolCallbackProvider = toolCallbackProvider;
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(this.systemPromptContent)
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
                .system(systemPromptContent + " Always provide game recommendations after each conversation, titled '{username}'s Game Recommendations', containing a list of recommended games.")
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .entity(GameRecommendation.class);
        log.info("gameRecommendation: {}", gameRecommendation);
        return gameRecommendation;
    }

    /**
     * Chat using RAG (Retrieval Augmented Generation).
     * First classifies the user's question to determine the appropriate document tag,
     * then uses that tag to filter relevant documents for the response.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response augmented with relevant document content
     */
    public String chatUsingRAG(String message, String chatId) {
        String rewrittenMessage = appQueryRewriter.rewriteQuery(message);
        ChatResponse chatResponse = chatClient.prompt()
                .user(rewrittenMessage)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(
                        AppRagCustomAdvisorFactory.createAppRagCustomAdvisor(vectorStore, appMultiQueryExpander)
                )
                .call()
                .chatResponse();
        String responseText = chatResponse.getResult().getOutput().getText();
        log.info("chatUsingRAG responseText: {}", responseText);
        return responseText;
    }

    /**
     * Chat using external tools.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response using tools
     */
    public String chatUsingTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(Arrays.stream(toolCallbacks).toList())
                .call()
                .chatResponse();
        String responseText = chatResponse.getResult().getOutput().getText();
        log.info("chatUsingTools responseText: {}", responseText);
        return responseText;
    }

    /**
     * Chat using MCP (Model Context Protocol).
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response using MCP
     */
    public String chatUsingMCP(String message, String chatId) {
        ChatResponse chatresponse = chatClient.prompt()
                .user(message)
                .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, chatId))
                .toolCallbacks(toolCallbackProvider.getToolCallbacks())
                .call()
                .chatResponse();
        String responseText = chatresponse.getResult().getOutput().getText();
        log.info("chatUsingMCP responseText: {}", responseText);
        return responseText;
    }
}