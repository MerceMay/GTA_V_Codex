package com.mercemay.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import com.mercemay.aiagent.manager.WorkspaceManager;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;

public class FileOperationTool {
    private final WorkspaceManager workspaceManager;

    public FileOperationTool(WorkspaceManager workspaceManager) {
        this.workspaceManager = workspaceManager;
    }

    @Tool(description = "Reads the content of a file.")
    public String readFile(@ToolParam(description = "filename") String filename) {
        try {
            File file = workspaceManager.resolve(filename);
            return file.exists() ? FileUtil.readUtf8String(file) : "Error: File does not exist.";
        } catch (IllegalArgumentException e) {
            return "Invalid filename: " + e.getMessage();
        } catch (IORuntimeException e) {
            return "Error reading file: " + e.getMessage();
        }
    }


    @Tool(description = "Writes content to a file.")
    public String writeFile(@ToolParam(description = "filename") String filename,
                            @ToolParam(description = "content") String content) {
        try {
            File targetFile = workspaceManager.resolve(filename);
            FileUtil.writeUtf8String(content, targetFile);
            return "Success: Content written to " + targetFile.getName();
        } catch (IllegalArgumentException e) {
            return "Invalid filename: " + e.getMessage();
        } catch (IORuntimeException e) {
            return "Error writing to file: " + e.getMessage();
        }
    }
}
