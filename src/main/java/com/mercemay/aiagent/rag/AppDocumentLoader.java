package com.mercemay.aiagent.rag;

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

@Slf4j
@Component
public class AppDocumentLoader {
    private final ResourcePatternResolver resourcePatternResolver;

    public AppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    public List<Document> loadDocuments() {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources("classpath:/documents/*.md");
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                String tag = "gta_v_general";
                if (filename != null) {
                    tag = switch (filename) {
                        case "Grand_Theft_Auto_V_Characters_Main_Protagonists_and_Key_NPCs.md" ->
                                "characters_protagonists";
                        case "Grand_Theft_Auto_V_Endings_Choices_and_Outcomes.md" -> "endings_choices";
                        case "Grand_Theft_Auto_V_Heists_Plans_Setups_and_Choices.md" -> "heists_plans";
                        case "Grand_Theft_Auto_V_Lesters_Assassinations_and_Stock_Market_Guide.md" ->
                                "lester_assassinations";
                        case "Grand_Theft_Auto_V_Main_Story_Missions_Full_List_and_Walkthrough.md" ->
                                "main_story_missions";
                        case "Grand_Theft_Auto_V_Random_Events_and_Collectibles_Overview.md" ->
                                "random_events_collectibles";
                        case "Grand_Theft_Auto_V_Strangers_and_Freaks_Side_Missions_Complete_Guide.md" ->
                                "strangers_freaks_side";
                        default -> "gta_v_general";
                    };
                }
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true) // Split on horizontal rules
                        .withIncludeCodeBlock(false) // Ignore code blocks
                        .withIncludeBlockquote(false) // Ignore blockquotes
                        .withAdditionalMetadata("status", "active") // Add custom metadata
                        .withAdditionalMetadata("tag", tag) // Add tag metadata
                        .withAdditionalMetadata("game", "gta_v") // Add game metadata
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
}
