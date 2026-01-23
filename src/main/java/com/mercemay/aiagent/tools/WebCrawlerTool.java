package com.mercemay.aiagent.tools;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.IOException;

public class WebCrawlerTool {
    @Tool(description = "Crawl a website and extract information from it.")
    public String crawlWebsite(@ToolParam(description = "The URL of the website to crawl.") String url) {
        try {
            Document doc = Jsoup.connect(url).get();
            return doc.text();
        } catch (IOException e) {
            return "Failed to crawl the website: " + e.getMessage();
        }
    }
}
