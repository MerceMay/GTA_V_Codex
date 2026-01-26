package com.mercemay.aiagent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.mercemay.aiagent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

@Data
@Slf4j
@EqualsAndHashCode(callSuper = true)
public class ToolCallAgent extends ReActAgent {

    /**
     * The available tools that the agent can utilize.
     */
    private final ToolCallback[] availableTools;

    /**
     * The tool call response from the chat model.
     */
    private ChatResponse toolCallChatResponse;

    /**
     * The manager responsible for handling tool calls.
     */
    private final ToolCallingManager toolCallingManager;


    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super();
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        this.chatOptions = ToolCallingChatOptions.builder()
                .toolCallbacks(availableTools)
                .internalToolExecutionEnabled(false)
                .build();
    }


    /**
     * Handle the current situation and decide whether to act further.
     *
     * @return true if further action is needed, false otherwise.
     */
    @Override
    public boolean think() {
        //
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            UserMessage userMessage = new UserMessage(getNextStepPrompt());
            getMessageList().add(userMessage);
        }
        try {
            ChatResponse chatResponse = getChatClient()
                    .prompt(new Prompt(getMessageList(), chatOptions))
                    .system(getSystemPrompt())
                    .call()
                    .chatResponse();
            // Save the response of the thinking step for the use in the acting step
            this.toolCallChatResponse = chatResponse;

            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // Get the tool calls made by the agent
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // Get the thinking result text
            String result = assistantMessage.getText();
            log.info("Agent {}'s thinking result: {}", getName(), result);
            log.info("Agent {}'s chosen tools: {}", getName(), toolCallList);

            // Log detailed tool call information
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall -> String.format("Tool Name: %s, Tool Arguments: %s",
                            toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info("Agent {}'s tool call info: \n{}", getName(), toolCallInfo);

            // Determine if there are any tool calls to act upon
            if (toolCallList.isEmpty()) {
                // No tool calls to act upon, thinking is finished, record the assistant message
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // There are tool calls to act upon, no need to record the assistant message yet, as the tool call results will be added later
                return true;
            }
        } catch (Exception e) {
            log.error("Agent {} encountered an error during thinking: ", getName(), e);
            getMessageList().add(new AssistantMessage("Error during thinking: " + e.getMessage()));
            return false;
        }
    }

    /**
     * Perform the action determined by the agent.
     *
     * @return The result of the action.
     */
    @Override
    public String act() {
        if (!this.toolCallChatResponse.hasToolCalls()) {
            return "No tool calls to act upon";
        }
        // Execute the tool calls using the ToolCallingManager which will handle invoking the appropriate tools
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(
                new Prompt(getMessageList(), chatOptions),
                this.toolCallChatResponse
        );
        // conversationHistory already includes the tool call results, so we just need to update the message list
        setMessageList(toolExecutionResult.conversationHistory());
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        boolean terminateAgentToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(toolResponse -> toolResponse.name().equals("terminateAgent"));
        if (terminateAgentToolCalled) {
            setState(AgentState.COMPLETED);
        }
        String results = toolResponseMessage.getResponses().stream()
                .map(toolResponse -> "Tool Name: " + toolResponse.name() + ", Result: " + toolResponse.responseData())
                .collect(Collectors.joining("\n"));
        log.info("Agent {}'s action results: \n{}", getName(), results);
        return results;
    }

        @Override
        public String step() {
            try {
                boolean shouldAct = think();
                
                            // Retrieve the text content (Thought) from the LLM response
                            String thought = "";
                            if (this.toolCallChatResponse != null && this.toolCallChatResponse.getResult() != null) {
                                thought = this.toolCallChatResponse.getResult().getOutput().getText();
                            }
                
                            // Clean up formatting: remove "Thought:" and "Action:" markers
                            if (StrUtil.isNotBlank(thought)) {
                                // Remove "Thought:" header
                                thought = thought.replaceAll("(?i)^Thought:\\s*", "");
                                // Remove trailing "Action:" marker (and any empty lines before it)
                                thought = thought.replaceAll("(?i)\\n+\\s*Action:\\s*$", "");
                                thought = thought.trim();
                            }
                
                            if (!shouldAct) {                    // Final Answer case.
                    return StrUtil.isBlank(thought) ? "Thinking Finished." : thought;
                }
    
                // Action case. Execute the action to update history/state, but ignore the return string.
                act();
    
                // Return ONLY the thought
                return thought;
    
            } catch (Exception e) {
                log.error("Error during agent step", e);
                return "Error during agent step: " + e.getMessage();
            }
        }}
