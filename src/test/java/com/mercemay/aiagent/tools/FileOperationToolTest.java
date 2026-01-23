package com.mercemay.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import com.mercemay.aiagent.manager.WorkspaceManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class FileOperationToolTest {

    private WorkspaceManager workspaceManager;
    private FileOperationTool fileOperationTool;

    @BeforeEach
    void setUp() {
        workspaceManager = new WorkspaceManager();
        workspaceManager.init();
        fileOperationTool = new FileOperationTool(workspaceManager);
    }

    @AfterEach
    void tearDown() {
        workspaceManager.destroy();
    }

    @Test
    void testReadFileSuccess() {
        // Arrange
        String filename = "test-read.txt";
        String expectedContent = "Hello, World!";
        File file = workspaceManager.resolve(filename);
        FileUtil.writeUtf8String(expectedContent, file);

        // Act
        String result = fileOperationTool.readFile(filename);

        // Assert
        assertEquals(expectedContent, result);
    }

    @Test
    void testReadFileNotExists() {
        // Act
        String result = fileOperationTool.readFile("non-existent.txt");

        // Assert
        assertEquals("Error: File does not exist.", result);
    }

    @Test
    void testReadFileInvalidPath() {
        // Act
        String result = fileOperationTool.readFile("../../../etc/passwd");

        // Assert
        assertTrue(result.startsWith("Invalid filename:"));
    }

    @Test
    void testReadFileWithSubdirectory() {
        // Arrange
        String filename = "subdir/nested.txt";
        String expectedContent = "Nested content";
        File file = workspaceManager.resolve(filename);
        FileUtil.writeUtf8String(expectedContent, file);

        // Act
        String result = fileOperationTool.readFile(filename);

        // Assert
        assertEquals(expectedContent, result);
    }

    @Test
    void testWriteFileSuccess() {
        // Arrange
        String filename = "test-write.txt";
        String content = "Test content to write";

        // Act
        String result = fileOperationTool.writeFile(filename, content);

        // Assert
        assertTrue(result.startsWith("Success:"));
        assertTrue(result.contains(filename));

        // Verify file was actually written
        File file = workspaceManager.resolve(filename);
        assertTrue(file.exists());
        assertEquals(content, FileUtil.readUtf8String(file));
    }

    @Test
    void testWriteFileOverwrite() {
        // Arrange
        String filename = "overwrite.txt";
        String initialContent = "Initial content";
        String newContent = "New content";

        // Write initial content
        fileOperationTool.writeFile(filename, initialContent);

        // Act - overwrite
        String result = fileOperationTool.writeFile(filename, newContent);

        // Assert
        assertTrue(result.startsWith("Success:"));
        File file = workspaceManager.resolve(filename);
        assertEquals(newContent, FileUtil.readUtf8String(file));
    }

    @Test
    void testWriteFileInvalidPath() {
        // Act
        String result = fileOperationTool.writeFile("../../../tmp/malicious.txt", "content");

        // Assert
        assertTrue(result.startsWith("Invalid filename:"));
    }

    @Test
    void testWriteFileEmptyContent() {
        // Arrange
        String filename = "empty.txt";

        // Act
        String result = fileOperationTool.writeFile(filename, "");

        // Assert
        assertTrue(result.startsWith("Success:"));
        File file = workspaceManager.resolve(filename);
        assertTrue(file.exists());
        assertEquals("", FileUtil.readUtf8String(file));
    }

    @Test
    void testWriteFileWithSpecialCharacters() {
        // Arrange
        String filename = "special.txt";
        String content = "Content with special chars: éñ中文🎉\n\t\"quotes\"";

        // Act
        String result = fileOperationTool.writeFile(filename, content);

        // Assert
        assertTrue(result.startsWith("Success:"));
        assertEquals(content, fileOperationTool.readFile(filename));
    }

    @Test
    void testWriteAndReadFileRoundTrip() {
        // Arrange
        String filename = "roundtrip.txt";
        String content = "This is a round trip test\nWith multiple lines\nAnd UTF-8: 你好";

        // Act
        fileOperationTool.writeFile(filename, content);
        String readResult = fileOperationTool.readFile(filename);

        // Assert
        assertEquals(content, readResult);
    }

    @Test
    void testWriteFileCreatesNestedDirectories() {
        // Arrange
        String filename = "deep/nested/path/file.txt";
        String content = "Nested file content";

        // Act
        String result = fileOperationTool.writeFile(filename, content);

        // Assert
        assertTrue(result.startsWith("Success:"));
        File file = workspaceManager.resolve(filename);
        assertTrue(file.exists());
        assertEquals(content, FileUtil.readUtf8String(file));
    }
}