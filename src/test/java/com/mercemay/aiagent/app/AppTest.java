package com.mercemay.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.function.Consumer;

@SpringBootTest
public class AppTest {
    @Resource
    private App app;

    @Test
    void chat() {
        String chaId = UUID.randomUUID().toString();

        String message = "你好，我是MerceMay";
        String response = app.chat(message, chaId);
        Assertions.assertNotNull(response);

        message = "请推荐几款游戏给我";
        response = app.chat(message, chaId);
        Assertions.assertNotNull(response);

        message = "你还记得我之前说的话吗？";
        response = app.chat(message, chaId);
        Assertions.assertNotNull(response);
    }

    @Test
    void getGameRecommendation() {
        String chaId = UUID.randomUUID().toString();

        String message = "你好，我是MerceMay，请你推荐几款游戏给我";
        App.GameRecommendation gameRecommendation = app.getGameRecommendation(message, chaId);
        Assertions.assertNotNull(gameRecommendation);
    }

    @Test
    void chatUsingRAG() {
        String chaId = UUID.randomUUID().toString();

        String message = "GTA5的主角有哪些？";
        String response = app.chatUsingRAG(message, chaId);
        Assertions.assertNotNull(response);
    }

    @Test
    void chatUsingTools() {
        Consumer<String> chatWithTools = (String msg) -> {
            String chatId = UUID.randomUUID().toString();
            String response = app.chatUsingTools(msg, chatId);
            System.out.println(response);
            Assertions.assertNotNull(response);
        };

        chatWithTools.accept("请帮我创建一个名为test.txt的文件，并写入内容：Hello, World!");
        chatWithTools.accept("请帮我下载这个链接的内容：https://placehold.co/600x400.png");
        chatWithTools.accept("请帮我执行命令：echo Hello from terminal");
        chatWithTools.accept("请帮我搜索Spring AI框架的最新消息");
        chatWithTools.accept("请帮我爬取这个网站的内容：https://www.example.com");
    }

    @Test
    void chatUsingMCP() {
        String chatId = UUID.randomUUID().toString();
        String message = "规划从华盛顿特区到洛杉矶的自驾游路线，途经国家公园，并推荐沿途的餐厅和住宿地点。";
        String response = app.chatUsingMCP(message, chatId);
        Assertions.assertNotNull(response);
    }
}