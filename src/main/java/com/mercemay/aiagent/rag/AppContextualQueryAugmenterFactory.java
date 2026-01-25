package com.mercemay.aiagent.rag;


import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.rag.generation.augmentation.ContextualQueryAugmenter;

public class AppContextualQueryAugmenterFactory {
    public static ContextualQueryAugmenter createContextualQueryAugmenter() {
        PromptTemplate emptyPromptTemplate = PromptTemplate.builder()
                .template("""
                        As an AI assistant specialized only in Grand Theft Auto V (GTA V), please notify the user that no relevant information was found in the official knowledge base.
                        Explicitly state that you are prohibited from answering non-GTA V topics or using external knowledge (unless explicitly authorized).
                        Please maintain a polite, professional, and enthusiastic tone.
                        """)
                .build(); // When no context is found, use this prompt template.
        return ContextualQueryAugmenter.builder()
                .allowEmptyContext(false)
                .emptyContextPromptTemplate(emptyPromptTemplate)
                .build();
    }
}
