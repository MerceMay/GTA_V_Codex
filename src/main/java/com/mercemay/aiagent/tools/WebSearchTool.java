package com.mercemay.aiagent.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;


public class WebSearchTool {
    private final String apiKey;
    private final String searchUrl;

    public WebSearchTool(String apiKey, String searchUrl) {
        this.apiKey = apiKey;
        this.searchUrl = searchUrl;
    }

    @Tool(description = "Use Google to search for real-time information, which is suitable for fact-checking, news inquiries or obtaining the latest knowledge.")
    public String webSearch(@ToolParam(description = "The search query") String query) {
        Map<String, Object> body = new HashMap<>();
        body.put("q", query);
        body.put("gl", "us");
        body.put("hl", "en-US");
        try {
            String rawJson = HttpRequest.post(searchUrl)
                    .header("X-API-KEY", apiKey)
                    .header("Content-Type", "application/json")
                    .body(JSONUtil.toJsonStr(body))
                    .execute()
                    .body();

            JSONObject jsonObject = JSONUtil.parseObj(rawJson);
            StringJoiner resultContext = new StringJoiner("\n\n");

            // 1. Process Knowledge Graph
            if (jsonObject.containsKey("knowledgeGraph")) {
                JSONObject kg = jsonObject.getJSONObject("knowledgeGraph");
                StringBuilder kgText = new StringBuilder("Knowledge Graph:\n");
                kgText.append("Title: ").append(kg.getStr("title")).append("\n");
                kgText.append("Description: ").append(kg.getStr("description")).append("\n");
                if (kg.containsKey("attributes")) {
                    kg.getJSONObject("attributes").forEach((k, v) -> kgText.append(k).append(": ").append(v).append("\n"));
                }
                resultContext.add(kgText.toString());
            }

            // 2. Process Organic Results
            if (jsonObject.containsKey("organic")) {
                JSONArray organic = jsonObject.getJSONArray("organic");
                for (int i = 0; i < Math.min(organic.size(), 5); i++) {
                    JSONObject item = organic.getJSONObject(i);
                    String entry = String.format("Source [%d]: %s\nLink: %s\nSnippet: %s",
                            i + 1, item.getStr("title"), item.getStr("link"), item.getStr("snippet"));
                    resultContext.add(entry);
                }
            }

            // 3. Process People Also Ask
            if (jsonObject.containsKey("peopleAlsoAsk")) {
                JSONArray paa = jsonObject.getJSONArray("peopleAlsoAsk");
                StringBuilder paaText = new StringBuilder("Related Questions:\n");
                for (int i = 0; i < paa.size(); i++) {
                    JSONObject q = paa.getJSONObject(i);
                    paaText.append("Q: ").append(q.getStr("question")).append("\n");
                    paaText.append("A: ").append(q.getStr("snippet")).append("\n");
                }
                resultContext.add(paaText.toString());
            }

            return resultContext.length() > 0 ? resultContext.toString() : "No relevant results found.";

        } catch (Exception e) {
            return "Error during web search: " + e.getMessage();
        }
    }
}

