package com.mercemay.aiagent.model;

import com.mercemay.aiagent.constant.AppConstant;

/**
 * Structured output model for AI-based question classification.
 * Used by the LLM to extract the appropriate document tag
 * based on the user's question content.
 *
 * @param tag The document tag that best matches the user's question.
 *            Should be one of the values from GtaVTag constants.
 * @param confidence Confidence level of the classification (0.0 to 1.0).
 * @param reasoning Brief explanation of why this tag was chosen.
 */
public record QuestionClassification(
        String tag,
        double confidence,
        String reasoning
) {
    /**
     * Creates a classification with the general fallback tag.
     *
     * @param reasoning The reason for using the general tag
     * @return A QuestionClassification with the general tag
     */
    public static QuestionClassification general(String reasoning) {
        return new QuestionClassification(AppConstant.GtaV.GENERAL, 0.5, reasoning);
    }
}
