package com.mercemay.aiagent.mcpserver.tools;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class ImageSearchTool {
    @Value("${pexels.api-key}")
    private String apiKey;
    @Value("${pexels.search-url}")
    private String searchUrl;

    @Tool(description = "Search for images with given query, number of results per page, and page number")
    public String searchImages(@ToolParam(description = "The search query for images") String query,
                               @ToolParam(description = "The number of results to return") int perPage,
                               @ToolParam(description = "The page number for pagination") int page) {
        try {
            HttpResponse response = HttpRequest.get(searchUrl)
                    .header("Authorization", apiKey)
                    .form("query", query)
                    .form("per_page", String.valueOf(perPage))
                    .form("page", String.valueOf(page))
                    .execute();
            if (!response.isOk()) {
                return "Error: API request failed with status " + response.getStatus();
            }

            JSONObject jsonResponse = JSONUtil.parseObj(response.body());
            JSONArray photos = jsonResponse.getJSONArray("photos");

            if (photos == null || photos.isEmpty()) {
                return "No images found for query: " + query;
            }

            // extract medium image URLs
            String imageUrls = photos.stream()
                    .map(obj -> {
                        JSONObject photo = (JSONObject) obj;
                        JSONObject src = photo.getJSONObject("src");
                        return src.getStr("medium");
                    })
                    .collect(Collectors.joining("\n"));

            return "Image URLs:\n" + imageUrls;

        } catch (Exception e) {
            return "Error occurred while searching for images: " + e.getMessage();
        }
    }
}
