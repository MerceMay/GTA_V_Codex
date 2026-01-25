package com.mercemay.aiagent.agent;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AIAgentTest {
    @Resource
    private AIAgent aiAgent;

    @Test
    void run() {
        String userPrompt = """
                请你帮我规划从华盛顿特区到旧金山的自驾游路线，途径一些著名的旅游景点，并推荐每个景点的游玩时间和特色活动。
                此外，请帮我安排每天的住宿地点，要求住宿地点舒适且交通便利。最后，请提供一个大致的预算估算，包括油费、住宿费和餐饮费等。
                把所有内容都生成在一个PDF文件中。
                """;
        String result = aiAgent.run(userPrompt);
        System.out.println("Result:\n" + result);
        Assertions.assertNotNull(result);
    }
}
