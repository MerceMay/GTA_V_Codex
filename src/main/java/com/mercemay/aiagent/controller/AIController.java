package com.mercemay.aiagent.controller;

import com.mercemay.aiagent.agent.AIAgent;
import com.mercemay.aiagent.app.App;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AIController {

    @Resource
    private App app;

    @Resource
    private ToolCallback[] toolCallbacks;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Resource
    private ChatModel chatModel;

    /**
     * Chat with AI asynchronously
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response
     */
    @GetMapping("/chat/sync")
    public String chatSync(String message, String chatId) {
        return app.chat(message, chatId);
    }

    /**
     * Stream chat responses from the AI model.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return A Flux stream of the AI model's responses
     */
    @GetMapping(value = "/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatSse(String message, String chatId) {
        return app.chatStream(message, chatId);
    }

    /**
     * Stream chat responses from the AI model.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return A Flux stream of the AI model's responses
     */
    @GetMapping(value = "/chat/sse/generic")
    public Flux<ServerSentEvent<String>> chatSseGeneric(String message, String chatId) {
        return app.chatStream(message, chatId)
                .map(content -> ServerSentEvent.<String>builder()
                        .data(content)
                        .build());
    }

    /**
     * Stream chat responses from the AI model using SseEmitter.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return An SseEmitter for streaming the AI model's responses
     */
    @GetMapping("/chat/sse/emitter")
    public SseEmitter chatSseEmitter(String message, String chatId) {
        SseEmitter emitter = new SseEmitter(5 * 60 * 1000L); // 5 minutes timeout
        app.chatStream(message, chatId)
                .subscribe(
                        content -> {
                            try {
                                emitter.send(content);
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        }, emitter::completeWithError, emitter::complete);
        return emitter;
    }

    /**
     * Chat with AI Agent using tool calls.
     *
     * @param message The user's message
     * @return An SseEmitter for streaming the AI Agent's responses
     */
    @GetMapping("/chat/agent")
    public SseEmitter chatAgent(String message) {
        AIAgent aiAgent = new AIAgent(toolCallbacks, toolCallbackProvider, chatModel);
        return aiAgent.runStream(message);
    }

    /**
     * Chat with AI using RAG (Retrieval Augmented Generation).
     * Uses query rewriting and multi-query expansion for high-recall vector retrieval.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return The AI model's response augmented with relevant document content
     */
    @GetMapping("/chat/rag")
    public String chatRag(String message, String chatId) {
        return app.chatUsingRAG(message, chatId);
    }

    /**
     * Stream chat responses using RAG (Retrieval Augmented Generation).
     * Combines query rewriting with multi-query expansion and streams the result via SSE.
     *
     * @param message The user's message
     * @param chatId  The chat conversation ID
     * @return A Flux stream of the AI model's RAG-augmented responses
     */
    @GetMapping(value = "/chat/rag/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatRagSse(String message, String chatId) {
        return app.chatStreamUsingRAG(message, chatId);
    }
}
