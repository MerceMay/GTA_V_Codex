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

    @Tool(description = """
            Read the content of a specific file from the local file system.
            
            When to use:
            - You need to examine the code, configuration, or text within a file.
            - You want to verify the content of a file you just wrote or modified.
            
            Behaviors:
            - Returns the full content as a string.
            - Returns an error message if the file does not exist or cannot be read.
            """)
    public String readFile(@ToolParam(description = "The path of the file to read (e.g., 'src/Main.java' or 'config.json').") String filename) {
        try {
            File file = workspaceManager.resolve(filename);
            return file.exists() ? FileUtil.readUtf8String(file) : "Error: File does not exist.";
        } catch (IllegalArgumentException e) {
            return "Invalid filename: " + e.getMessage();
        } catch (IORuntimeException e) {
            return "Error reading file: " + e.getMessage();
        }
    }


    @Tool(description = """
            Write text content to a specific file in the local file system.
            
            When to use:
            - You need to create a new file with specific content (code, config, documentation).
            - You need to overwrite an existing file with new content.
            
            IMPORTANT:
            - This will OVERWRITE existing files without warning. Check if file exists first if needed.
            - Ensure the content is complete and correctly formatted.
            """)
    public String writeFile(@ToolParam(description = "The target file path where content will be written.") String filename,
                            @ToolParam(description = "The complete text content to write to the file.") String content) {
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
