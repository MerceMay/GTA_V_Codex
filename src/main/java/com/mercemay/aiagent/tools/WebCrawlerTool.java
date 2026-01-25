package com.mercemay.aiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

public class WebCrawlerTool {
    @Tool(description = """
            Extracts the visible text content from a given web page URL.
            
            When to use:
            - You need to read the content of a blog post, documentation page, or article.
            - The user asks for a summary of a specific web page.
            
            Limitations:
            - Uses Jsoup, so it handles static HTML best.
            - May not capture content rendered dynamically by JavaScript (SPA).
            - Returns raw text; formatting might be lost.
            """)
    public String crawlWebsite(@ToolParam(description = "The target website URL to crawl.") String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.text();
        } catch (IOException e) {
            return "Failed to crawl the website: " + e.getMessage();
        }
    }
}
