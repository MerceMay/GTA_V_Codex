package com.mercemay.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.io.File;
import java.util.UUID;

public class FileOperationTool implements AutoCloseable {
    private final String FILE_DIR;

    public FileOperationTool() {
        this.FILE_DIR = System.getProperty("java.io.tmpdir") + File.separator + "aiagent-" + UUID.randomUUID().toString();
        try {
            FileUtil.mkdir(FILE_DIR);
            // register shutdown hook to clean up temporary directory
            Runtime.getRuntime().addShutdownHook(new Thread(this::cleanup));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary directory: " + FILE_DIR, e);
        }
    }

    @Tool(description = "Reads the content of a file given its filename.")
    public String readFile(@ToolParam(description = "The name of the file to read.") String filename) {
        File targetFile = FileUtil.file(FILE_DIR, filename);
        try {
            return FileUtil.readUtf8String(targetFile);
        } catch (IORuntimeException e) {
            return "Error reading file: " + e.getMessage();
        }
    }

    @Tool(description = "Writes content to a file with the given filename.")
    public String writeFile(@ToolParam(description = "The name of the file to write to") String filename,
                            @ToolParam(description = "The content to write into the file") String content) {
        File targetFile = FileUtil.file(FILE_DIR, filename);
        try {
            FileUtil.writeUtf8String(content, targetFile);
            return "File written successfully to " + targetFile.getAbsolutePath();
        } catch (IORuntimeException e) {
            return "Error writing file: " + e.getMessage();
        }
    }

    /**
     * Cleans up the temporary directory and its contents.
     */
    private void cleanup() {
        try {
            File dir = new File(FILE_DIR);
            if (dir.exists()) {
                FileUtil.del(dir);
                System.out.println("Cleaned up temporary directory: " + FILE_DIR);
            }
        } catch (Exception e) {
            System.err.println("Failed to clean up temporary directory: " + e.getMessage());
        }
    }

    /**
     * Closes the tool and performs cleanup.
     */
    @Override
    public void close() {
        cleanup();
    }

    /**
     *  Gets the path of the temporary directory.
     */
    public String getTempDirectory() {
        return FILE_DIR;
    }
}
