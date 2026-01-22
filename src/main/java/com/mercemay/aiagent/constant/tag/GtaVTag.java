package com.mercemay.aiagent.constant.tag;

import java.util.Arrays;
import java.util.List;

/**
 * Document tags specific to Grand Theft Auto V content.
 * These tags are used to categorize and filter GTA V documents in the vector store.
 *
 * <p>Tag naming convention: {category}_{subcategory}
 * <ul>
 *   <li>Category: High-level topic (e.g., story, gameplay, character)</li>
 *   <li>Subcategory: Specific topic within the category</li>
 * </ul>
 *
 * <p>To extend GTA V content, add new tag constants following the naming convention.
 * For example, to add GTA Online content:
 * <pre>
 * String ONLINE_HEISTS = "online_heists";
 * String ONLINE_BUSINESSES = "online_businesses";
 * </pre>
 */
public interface GtaVTag extends GameTag {

    // ==================== Game Identifier ====================

    /**
     * Game identifier for GTA V documents.
     */
    String GAME_ID = "gta_v";

    // ==================== General Tags ====================

    /**
     * Default/fallback tag for general GTA V content.
     */
    String GENERAL = "gta_v_general";

    // ==================== Story & Narrative Tags ====================

    /**
     * Tag for main story missions content.
     */
    String STORY_MAIN_MISSIONS = "story_main_missions";

    /**
     * Tag for endings, choices and outcomes content.
     */
    String STORY_ENDINGS = "story_endings";

    // ==================== Character Tags ====================

    /**
     * Tag for main protagonists and key NPCs content.
     */
    String CHARACTER_PROTAGONISTS = "character_protagonists";

    // ==================== Gameplay Tags ====================

    /**
     * Tag for heists, plans, setups and choices content.
     */
    String GAMEPLAY_HEISTS = "gameplay_heists";

    /**
     * Tag for Lester's assassinations and stock market guide.
     */
    String GAMEPLAY_ASSASSINATIONS = "gameplay_assassinations";

    /**
     * Tag for random events and collectibles content.
     */
    String GAMEPLAY_COLLECTIBLES = "gameplay_collectibles";

    // ==================== Side Content Tags ====================

    /**
     * Tag for Strangers and Freaks side missions content.
     */
    String SIDE_STRANGERS_FREAKS = "side_strangers_freaks";

    // ==================== Utility Methods ====================

    /**
     * Returns all available GTA V tags (excluding GAME_ID and GENERAL).
     * Useful for AI classification and validation.
     *
     * @return List of all specific GTA V content tags
     */
    static List<String> getAllTags() {
        return Arrays.asList(
                STORY_MAIN_MISSIONS,
                STORY_ENDINGS,
                CHARACTER_PROTAGONISTS,
                GAMEPLAY_HEISTS,
                GAMEPLAY_ASSASSINATIONS,
                GAMEPLAY_COLLECTIBLES,
                SIDE_STRANGERS_FREAKS
        );
    }

    /**
     * Returns all tags including the general fallback tag.
     *
     * @return List of all GTA V tags including GENERAL
     */
    static List<String> getAllTagsWithGeneral() {
        return Arrays.asList(
                GENERAL,
                STORY_MAIN_MISSIONS,
                STORY_ENDINGS,
                CHARACTER_PROTAGONISTS,
                GAMEPLAY_HEISTS,
                GAMEPLAY_ASSASSINATIONS,
                GAMEPLAY_COLLECTIBLES,
                SIDE_STRANGERS_FREAKS
        );
    }

    /**
     * Returns a formatted string describing all tags for AI prompts.
     *
     * @return Formatted tag descriptions for LLM context
     */
    static String getTagDescriptions() {
        return """
                Available tags for GTA V content:
                - story_main_missions: Questions about main story missions, walkthrough, mission order
                - story_endings: Questions about game endings, choices, and their outcomes
                - character_protagonists: Questions about main characters (Michael, Franklin, Trevor) and key NPCs
                - gameplay_heists: Questions about heists, heist planning, crew selection, and approaches
                - gameplay_assassinations: Questions about Lester's assassination missions and stock market tips
                - gameplay_collectibles: Questions about random events, collectibles, and exploration content
                - side_strangers_freaks: Questions about side missions, Strangers and Freaks missions
                - gta_v_general: General GTA V questions that don't fit other categories
                """;
    }
}
