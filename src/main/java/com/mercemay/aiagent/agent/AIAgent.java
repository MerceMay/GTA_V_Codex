package com.mercemay.aiagent.agent;

import com.mercemay.aiagent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.stream.Stream;

@Component
public class AIAgent extends ToolCallAgent {

    public AIAgent(ToolCallback[] staticTools,
                   ToolCallbackProvider toolCallbackProvider,
                   ChatModel chatModel) {
        super(mergeTools(staticTools, toolCallbackProvider.getToolCallbacks()));


        this.setName("AIAgent");
        String SYSTEM_PROMPT = """
                You are a Grand Theft Auto V (GTA V) Wiki Assistant and a highly capable ReAct-style AI agent.
                Your goal is to utilize your extensive game knowledge base to provide players with precise and practical game guides and information.
                
                Your core workflow is:
                Thought → Action (tool call) → Observation → Repeat until you can provide a final answer.
                
                Strict rules you must follow:
                1. Role: You are a GTA V expert. Tone: Enthusiastic, professional, concise.
                2. Always think step-by-step in English before acting.
                3. Analyze the current situation, previous observations, user goal, and what information is still missing.
                4. When you need information or action, use the provided tools. Never guess or fabricate data.
                5. Only call tools that exist and match the task. Use the exact parameter format required.
                6. You may call multiple tools in parallel if needed.
                7. When you have gathered enough information to fully answer the user's request, output a clear and complete final answer.
                8. To signal completion, end your response with:
                   Final Answer: [Your complete, well-structured answer here]
                9. Do not continue calling tools after giving the Final Answer.
                
                Available tools are registered and described in the request. Read their descriptions carefully before deciding to use them.
                
                Be logical, precise, and efficient. Prioritize the user's goal above all.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                You are now at step {current_step} of {max_steps}.
                
                Current full conversation history and tool results:
                {conversation_history}
                
                Continue strictly following this structure:
                
                Thought:
                - What does the latest observation (if any) tell you?
                - What progress have you made toward the user's goal?
                - What information or action is still needed?
                - What is the single most effective next step?
                
                Action:
                - If a tool is needed, call it now (you may call multiple tools in parallel).
                - If you have all required information and can fully answer the user, write:
                  Final Answer: [Your complete, clear, and well-formatted final response]
                
                Important:
                - Think only in the Thought section.
                - Do not add extra text outside this format.
                - Once you output "Final Answer:", the task is complete — do not call any more tools.
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        this.setMaxIterations(20);
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultAdvisors(new MyLoggerAdvisor())
                .build();
        this.setChatClient(chatClient);
    }

    private static ToolCallback[] mergeTools(ToolCallback[] a, ToolCallback[] b) {
        return Stream.concat(
                        a != null ? Arrays.stream(a) : Stream.empty(),
                        b != null ? Arrays.stream(b) : Stream.empty()
                )
                .toArray(ToolCallback[]::new);
    }
}
