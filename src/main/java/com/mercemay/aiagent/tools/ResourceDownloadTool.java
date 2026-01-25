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

    @Tool(description = """
            Downloads a file/resource from a public HTTP/HTTPS URL to the local workspace.
            
            When to use:
            - You need to fetch external resources like images, PDFs, libraries, or datasets.
            - The user provides a direct link to a file they want saved.
            
            Inputs:
            - url: A valid HTTP/HTTPS URL.
            - filename: The local destination path/name to save the file.
            """)
    public String downloadResource(
            @ToolParam(description = "The valid HTTP/HTTPS URL of the resource to download.") String url,
            @ToolParam(description = "The local path/filename where the resource will be saved.") String filename) {
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
