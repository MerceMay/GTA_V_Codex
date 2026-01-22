package com.mercemay.aiagent.constant;

import com.mercemay.aiagent.constant.tag.DocumentMetadata;
import com.mercemay.aiagent.constant.tag.GtaVTag;

/**
 * Central constants interface for the AI Agent application.
 * This interface serves as the root for all application constants,
 * organizing them into logical hierarchical groups.
 *
 * <p>Structure:
 * <ul>
 *   <li>{@link DocumentMetadata} - Document metadata keys and status values</li>
 *   <li>{@link GtaVTag} - GTA V specific document tags</li>
 * </ul>
 *
 * <p>This design allows for easy extension to support:
 * <ul>
 *   <li>Additional game content within GTA V</li>
 *   <li>Other games (e.g., RDR2Tag, GtaOnlineTag)</li>
 *   <li>Other constant categories (e.g., API endpoints, error codes)</li>
 * </ul>
 */
public interface AppConstant {

    /**
     * Document metadata constants.
     * @see DocumentMetadata
     */
    interface Metadata extends DocumentMetadata {}

    /**
     * GTA V document tag constants.
     * @see GtaVTag
     */
    interface GtaV extends GtaVTag {}
}
