package com.mercemay.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * A tool for terminating an agent's operation.
 */
public class TerminateAgentTool {

    @Tool(description = """
            Call this tool ONLY when you are 100% confident that you have fully answered the user's question 
            OR when you have completed all necessary actions and there is nothing more you can do.
            
            IMPORTANT RULES:
            - This is the ONLY correct way to end the agent's loop.
            - Do NOT wait until max steps are reached.
            - Do NOT continue thinking or calling other tools after deciding to terminate.
            - After calling this tool, the agent will stop immediately.
            
            When to call:
            - You have provided a complete, satisfactory Final Answer in your previous response.
            - Further actions would be redundant, unnecessary, or impossible.
            - The task is clearly finished (success or reasonable failure).
            
            Return value will be shown as Observation, but the agent session ends right after.
            """)
    public String terminateAgent() {
        return "Agent terminated successfully. Task completed or no further action possible.";
    }
}
