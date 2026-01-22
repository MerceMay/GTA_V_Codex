package com.mercemay.aiagent.rag;

import com.mercemay.aiagent.constant.AppConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class AppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;

    /**
     * Mapping of document filenames to their corresponding tags.
     * This provides a centralized configuration for document categorization.
     */
    private static final Map<String, String> DOCUMENT_TAG_MAPPING = Map.of(
            "Grand_Theft_Auto_V_Characters_Main_Protagonists_and_Key_NPCs.md", AppConstant.GtaV.CHARACTER_PROTAGONISTS,
            "Grand_Theft_Auto_V_Endings_Choices_and_Outcomes.md", AppConstant.GtaV.STORY_ENDINGS,
            "Grand_Theft_Auto_V_Heists_Plans_Setups_and_Choices.md", AppConstant.GtaV.GAMEPLAY_HEISTS,
            "Grand_Theft_Auto_V_Lesters_Assassinations_and_Stock_Market_Guide.md", AppConstant.GtaV.GAMEPLAY_ASSASSINATIONS,
            "Grand_Theft_Auto_V_Main_Story_Missions_Full_List_and_Walkthrough.md", AppConstant.GtaV.STORY_MAIN_MISSIONS,
            "Grand_Theft_Auto_V_Random_Events_and_Collectibles_Overview.md", AppConstant.GtaV.GAMEPLAY_COLLECTIBLES,
            "Grand_Theft_Auto_V_Strangers_and_Freaks_Side_Missions_Complete_Guide.md", AppConstant.GtaV.SIDE_STRANGERS_FREAKS
    );

    public AppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:/documents/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String tag = resolveTagForDocument(filename);

                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true) // Split on horizontal rules
                        .withIncludeCodeBlock(false) // Ignore code blocks
                        .withIncludeBlockquote(false) // Ignore blockquotes
                        .withAdditionalMetadata(AppConstant.Metadata.KEY_STATUS, AppConstant.Metadata.STATUS_ACTIVE)
                        .withAdditionalMetadata(AppConstant.Metadata.KEY_TAG, tag)
                        .withAdditionalMetadata(AppConstant.Metadata.KEY_GAME, AppConstant.GtaV.GAME_ID)
                        .build();
                MarkdownDocumentReader reader = new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(reader.read());
                log.info("Loaded {} markdown documents from classpath:/documents/", allDocuments.size());
            }
        } catch (IOException e) {
            log.error("Error loading documents: {}", e.getMessage(), e);
        }
        return allDocuments;
    }

    /**
     * Resolves the appropriate tag for a document based on its filename.
     *
     * @param filename the document filename
     * @return the corresponding tag constant, or GENERAL tag if not found
     */
    private String resolveTagForDocument(String filename) {
        if (filename == null) {
            return AppConstant.GtaV.GENERAL;
        }
        return DOCUMENT_TAG_MAPPING.getOrDefault(filename, AppConstant.GtaV.GENERAL);
    }
}
