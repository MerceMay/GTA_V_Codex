package com.mercemay.aiagent.constant;

/**
 * Constants for document resource tags used in the RAG (Retrieval Augmented Generation) system.
 * These tags are used to categorize and filter documents in the vector store.
 *
 * <p>Tag naming convention: {category}_{subcategory}
 * <ul>
 *   <li>Category: High-level topic (e.g., story, gameplay, character)</li>
 *   <li>Subcategory: Specific topic within the category</li>
 * </ul>
 */
public interface TagConstant {

    // ==================== Game Identifier ====================
    /**
     * Game identifier tag for GTA V documents.
     */
    String GAME_GTA_V = "gta_v";

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

    // ==================== Document Metadata Keys ====================
    /**
     * Metadata key for document tag.
     */
    String METADATA_KEY_TAG = "tag";

    /**
     * Metadata key for document status.
     */
    String METADATA_KEY_STATUS = "status";

    /**
     * Metadata key for game identifier.
     */
    String METADATA_KEY_GAME = "game";

    // ==================== Document Status Values ====================
    /**
     * Status value indicating document is active and searchable.
     */
    String STATUS_ACTIVE = "active";

    /**
     * Status value indicating document is inactive/archived.
     */
    String STATUS_INACTIVE = "inactive";
}
