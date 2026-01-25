package com.mercemay.aiagent.agent.model;

/**
 * Represents the state of an AI agent.
 */
public enum AgentState {

    /**
     * The agent is idle and waiting for tasks.
     */
    IDLE,

    /**
     * The agent is currently processing a task.
     */
    PROCESSING,

    /**
     * The agent has completed its task.
     */
    COMPLETED,

    /**
     * The agent has encountered an error.
     */
    ERROR
}
