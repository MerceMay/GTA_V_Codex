package com.mercemay.aiagent.tools;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
public class WebCrawlerToolTest {
    @Test
    public void crawlWebsite() {
        WebCrawlerTool webCrawlerTool = new WebCrawlerTool();
        String url = "https://www.example.com";
        String content = webCrawlerTool.crawlWebsite(url);
        System.out.println("Crawled content: " + content);
        assertNotNull(content);
    }
}
