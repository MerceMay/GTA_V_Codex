package com.mercemay.aiagent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * A tool for terminating an agent's operation.
 */
public class TerminateAgentTool {

    @Tool(description = """
            Terminate the interaction when the request is met OR if the assistant cannot proceed further with the task.
            When you have completed all necessary steps to fulfill the user's request, call this tool to end the session.
            """)
    public String terminateAgent() {
        return "Agent terminated successfully.";
    }
}
