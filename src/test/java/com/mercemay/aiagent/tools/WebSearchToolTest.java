package com.mercemay.aiagent.tools;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class WebSearchToolTest {

    @Value("${websearch.api-key}")
    private String apiKey;
    @Value("${websearch.search-url}")
    private String searchUrl;

    @Test
    @DisplayName("Test web search")
    void testWebSearchRealCall() {
        WebSearchTool webSearchTool = new WebSearchTool(apiKey, searchUrl);
        String query = "Spring AI framework latest news";
        String result = webSearchTool.webSearch(query);

        System.out.println(result);

        assertNotNull(result, "Result should not be null");
        assertFalse(result.startsWith("Error"), "Result should not be an error message");
        assertNotEquals("No relevant results found.", result, "Result should contain relevant search results");

        assertTrue(result.contains("Source [1]") || result.contains("Knowledge Graph"),
                "Result should contain at least one source or knowledge graph information");
    }
}