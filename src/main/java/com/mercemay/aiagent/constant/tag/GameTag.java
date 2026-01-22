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
 */
public interface GameTag {

    /**
     * The unique identifier for the game.
     * Each game implementation should define its own GAME_ID.
     */
    String GAME_ID = "unknown";
}
