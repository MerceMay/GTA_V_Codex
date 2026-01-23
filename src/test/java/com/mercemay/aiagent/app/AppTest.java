package com.mercemay.aiagent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

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
    void chatWithRAG() {
        String chaId = UUID.randomUUID().toString();

        String message = "西红柿炒鸡蛋的做法是什么？";
        String response = app.chatWithRAG(message, chaId);
        Assertions.assertNotNull(response);
    }
}