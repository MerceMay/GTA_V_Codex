package com.mercemay.aiagent.agent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;


/**
 * An abstract agent that follows the ReAct (Reasoning and Acting) paradigm.
 */
@Slf4j
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ReActAgent extends BaseAgent {
    /**
     * Handle the current situation and decide whether to act further.
     *
     * @return true if further action is needed, false otherwise.
     */
    public abstract boolean think();

    /**
     * Perform the action determined by the agent.
     *
     * @return The result of the action.
     */
    public abstract String act();


    @Override
    public String step() {
        try {
            boolean shouldAct = think();
            if (!shouldAct) {
                return "Thinking Finished, No Action Needed.";
            }
            return act();
        } catch (Exception e) {
            log.error("Error during agent step", e);
            return "Error during agent step: " + e.getMessage();
        }
    }
}
