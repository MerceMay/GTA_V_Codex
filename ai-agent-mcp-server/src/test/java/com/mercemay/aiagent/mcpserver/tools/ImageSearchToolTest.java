package com.mercemay.aiagent.mcpserver.tools;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class ImageSearchToolTest {

    @Resource
    private ImageSearchTool imageSearchTool;

    @Test
    public void searchImages() {
        String result = imageSearchTool.searchImages("nature", 3, 1);
        System.out.println(result);
    }
}
