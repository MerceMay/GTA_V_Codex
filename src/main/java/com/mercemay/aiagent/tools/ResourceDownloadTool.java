package com.mercemay.aiagent.tools;


import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.mercemay.aiagent.manager.WorkspaceManager;
import org.jsoup.internal.StringUtil;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

public class ResourceDownloadTool {
    private final WorkspaceManager workspaceManager;

    public ResourceDownloadTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Tool(description = "Download a resource from a given URL and save it with the specified filename.")
    public String downloadResource(
            @ToolParam(description = "The URL of the resource to download.") String url,
            @ToolParam(description = "The filename to save the downloaded resource as.") String filename) {
        if (StringUtil.isBlank(url) || !(url.startsWith("http://") || url.startsWith("https://"))) {
            return "Error: Invalid URL.";
        }

        try {
            File targetFile = workspaceManager.resolve(filename);

            long size = HttpUtil.downloadFile(url, targetFile);
            return String.format("Success: Downloaded '%s' to '%s' (%s).",
                    url, filename, FileUtil.readableFileSize(size));
        } catch (IllegalArgumentException e) {
            return "Invalid filename: " + e.getMessage();
        } catch (Exception e) {
            return "Error downloading resource: " + e.getMessage();
        }
    }
}
