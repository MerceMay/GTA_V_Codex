package com.mercemay.aiagent.tools;


import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FileOperationTool Test Suite")
class FileOperationToolTest {

    private FileOperationTool fileOperationTool;
    private String tempDir;

    @BeforeEach
    void setUp() {
        fileOperationTool = new FileOperationTool();
        tempDir = fileOperationTool.getTempDirectory();
        System.out.println("Create temporary directory " + tempDir);
    }

    @AfterEach
    void tearDown() {
        if (fileOperationTool != null) {
            fileOperationTool.close();
            System.out.println("Clean up temporary directory " + tempDir);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Test temporary directory creation")
    void testTempDirectoryCreation() {
        // Given & When
        File dir = new File(tempDir);

        // Then
        assertTrue(dir.exists(), "Temporary directory should exist");
        assertTrue(dir.isDirectory(), "Should be a directory");
        assertTrue(tempDir.contains("aiagent-"), "Directory name should contain the 'aiagent-' prefix");
    }

    @Test
    @Order(2)
    @DisplayName("Test successful file write")
    void testWriteFileSuccess() {
        // Given
        String filename = "test.txt";
        String content = "Hello, Spring Boot Test!";

        // When
        String result = fileOperationTool.writeFile(filename, content);

        // Then
        assertTrue(result.contains("File written successfully"), "Should return a success message");
        File file = new File(tempDir, filename);
        assertTrue(file.exists(), "File should be created");

        String actualContent = FileUtil.readUtf8String(file);
        assertEquals(content, actualContent, "File content should match");
    }

    @Test
    @Order(3)
    @DisplayName("Test successful file read")
    void testReadFileSuccess() {
        // Given
        String filename = "read_test.txt";
        String expectedContent = "Test file read content";
        fileOperationTool.writeFile(filename, expectedContent);

        // When
        String actualContent = fileOperationTool.readFile(filename);

        // Then
        assertEquals(expectedContent, actualContent, "Read content should match written content");
    }

    @Test
    @Order(4)
    @DisplayName("Test reading non-existent file")
    void testReadNonExistentFile() {
        // Given
        String filename = "non_existent.txt";

        // When
        String result = fileOperationTool.readFile(filename);

        // Then
        assertTrue(result.startsWith("Error reading file:"), "Should return an error message");
    }

    @Test
    @Order(5)
    @DisplayName("Test writing Chinese content")
    void testWriteChineseContent() {
        // Given
        String filename = "chinese.txt";
        String content = "This is a test with Chinese characters: 你好，世界！";

        // When
        String writeResult = fileOperationTool.writeFile(filename, content);
        String readResult = fileOperationTool.readFile(filename);

        // Then
        assertTrue(writeResult.contains("File written successfully"));
        assertEquals(content, readResult, "Chinese content should be read and written correctly");
    }

    @Test
    @Order(6)
    @DisplayName("Test writing special characters")
    void testWriteSpecialCharacters() {
        // Given
        String filename = "special.txt";
        String content = "Special chars: @#$%^&*()_+-={}[]|\\:\";<>?,./\nNewline\tTab";

        // When
        fileOperationTool.writeFile(filename, content);
        String result = fileOperationTool.readFile(filename);

        // Then
        assertEquals(content, result, "Special characters should be handled correctly");
    }

    @Test
    @Order(7)
    @DisplayName("Test writing empty content")
    void testWriteEmptyContent() {
        // Given
        String filename = "empty.txt";
        String content = "";

        // When
        fileOperationTool.writeFile(filename, content);
        String result = fileOperationTool.readFile(filename);

        // Then
        assertEquals("", result, "Empty content should be handled correctly");
    }

    @Test
    @Order(8)
    @DisplayName("Test file overwrite")
    void testOverwriteFile() {
        // Given
        String filename = "overwrite.txt";
        String originalContent = "Original content";
        String newContent = "New content";

        // When
        fileOperationTool.writeFile(filename, originalContent);
        String firstRead = fileOperationTool.readFile(filename);

        fileOperationTool.writeFile(filename, newContent);
        String secondRead = fileOperationTool.readFile(filename);

        // Then
        assertEquals(originalContent, firstRead);
        assertEquals(newContent, secondRead, "File should be overwritten");
    }

    @Test
    @Order(9)
    @DisplayName("Test multiple file operations")
    void testMultipleFiles() {
        // Given
        int fileCount = 5;

        // When
        for (int i = 0; i < fileCount; i++) {
            String filename = "file_" + i + ".txt";
            String content = "Content for file " + i;
            fileOperationTool.writeFile(filename, content);
        }

        // Then
        File dir = new File(tempDir);
        File[] files = dir.listFiles();
        assertNotNull(files);
        assertEquals(fileCount, files.length, "Should create the specified number of files");
    }

    @Test
    @Order(10)
    @DisplayName("Test resource cleanup")
    void testResourceCleanup() throws InterruptedException {
        // Given
        String testDir = tempDir;
        String filename = "cleanup_test.txt";
        fileOperationTool.writeFile(filename, "Test cleanup");

        File dir = new File(testDir);
        assertTrue(dir.exists(), "Directory should exist before cleanup");

        // When
        fileOperationTool.close();
        Thread.sleep(100); // Wait for cleanup to complete

        // Then
        assertFalse(dir.exists(), "Directory should be deleted after cleanup");
    }

    @Test
    @Order(11)
    @DisplayName("Test try-with-resources auto cleanup")
    void testAutoCloseableCleanup() throws Exception {
        // Given
        String testDir;

        // When
        try (FileOperationTool tool = new FileOperationTool()) {
            testDir = tool.getTempDirectory();
            tool.writeFile("auto_close.txt", "Test auto close");
            assertTrue(new File(testDir).exists(), "Directory should exist during use");
        }

        // Then
        Thread.sleep(100); // Wait for cleanup to complete
        assertFalse(new File(testDir).exists(), "Directory should be deleted after exiting try block");
    }

    @Test
    @Order(12)
    @DisplayName("Test large file write")
    void testLargeFileWrite() {
        // Given
        String filename = "large_file.txt";
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" content\n");
        }

        // When
        long startTime = System.currentTimeMillis();
        String writeResult = fileOperationTool.writeFile(filename, largeContent.toString());
        String readResult = fileOperationTool.readFile(filename);
        long endTime = System.currentTimeMillis();

        // Then
        assertTrue(writeResult.contains("File written successfully"));
        assertEquals(largeContent.toString(), readResult);
        System.out.println("Large file read/write time: " + (endTime - startTime) + "ms");
    }

    @Test
    @Order(13)
    @DisplayName("Test subdirectory file creation")
    void testSubdirectoryFileCreation() {
        // Given
        String filename = "subdir/nested/test.txt";
        String content = "File in nested directory";

        // When
        String result = fileOperationTool.writeFile(filename, content);
        String readContent = fileOperationTool.readFile(filename);

        // Then
        assertTrue(result.contains("File written successfully"), "Should successfully create file in nested directory");
        assertEquals(content, readContent, "Nested file content should be correct");
    }

    @Test
    @Order(14)
    @DisplayName("Test JSON content read/write")
    void testJsonContent() {
        // Given
        String filename = "data.json";
        String jsonContent = """
                {
                    "name": "Test User",
                    "age": 25,
                    "skills": ["Java", "Spring Boot", "Testing"],
                    "active": true
                }
                """;

        // When
        fileOperationTool.writeFile(filename, jsonContent);
        String result = fileOperationTool.readFile(filename);

        // Then
        assertEquals(jsonContent, result, "JSON content should be saved and read correctly");
    }

    @Test
    @Order(15)
    @DisplayName("Test concurrent file operations")
    void testConcurrentFileOperations() throws InterruptedException {
        // Given
        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        // When
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                String filename = "concurrent_" + index + ".txt";
                String content = "Thread " + index + " content";
                fileOperationTool.writeFile(filename, content);
                String result = fileOperationTool.readFile(filename);
                assertEquals(content, result);
            });
            threads[i].start();
        }

        // Then
        for (Thread thread : threads) {
            thread.join();
        }

        File dir = new File(tempDir);
        File[] files = dir.listFiles();
        assertNotNull(files);
        assertTrue(files.length >= threadCount, "All concurrent files should be created");
    }
}

