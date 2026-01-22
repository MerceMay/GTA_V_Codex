package com.mercemay.aiagent.constant.tag;

/**
 * Base interface for game-specific document tags.
 * Extend this interface to create tag constants for different games.
 *
 * <p>Example implementations:
 * <ul>
 *   <li>{@link GtaVTag} - Grand Theft Auto V tags</li>
 * </ul>
 *
 * <p>To add support for a new game, create a new interface extending GameTag:
 * <pre>
 * public interface Rdr2Tag extends GameTag {
 *     String GAME_ID = "rdr2";
 *     String STORY_MAIN = "story_main";
 *     // ...
 * }
 * </pre>
 *
 * <p>Note: The GAME_ID field should be overridden in each implementation.
 * The default value is provided only as a placeholder.
 */
public interface GameTag {

    /**
     * The unique identifier for the game.
     * Each game implementation must define its own GAME_ID value.
     * This default value serves as a placeholder and should never be used directly.
     */
    String GAME_ID = "unspecified";
}
