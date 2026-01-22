package com.mercemay.aiagent.constant.tag;

/**
 * Constants for document metadata keys and status values.
 * These constants are used across the RAG (Retrieval Augmented Generation) system
 * to define and filter document attributes in the vector store.
 */
public interface DocumentMetadata {

    // ==================== Metadata Keys ====================

    /**
     * Metadata key for document tag.
     */
    String KEY_TAG = "tag";

    /**
     * Metadata key for document status.
     */
    String KEY_STATUS = "status";

    /**
     * Metadata key for game identifier.
     */
    String KEY_GAME = "game";

    // ==================== Status Values ====================

    /**
     * Status value indicating document is active and searchable.
     */
    String STATUS_ACTIVE = "active";

    /**
     * Status value indicating document is inactive/archived.
     */
    String STATUS_INACTIVE = "inactive";
}
