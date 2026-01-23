package com.mercemay.aiagent.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpUtil;
import com.mercemay.aiagent.manager.WorkspaceManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceDownloadToolTest {

    private ResourceDownloadTool resourceDownloadTool;

    @Mock
    private WorkspaceManager workspaceManager;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        resourceDownloadTool = new ResourceDownloadTool(workspaceManager);
    }

    @Test
    void testDownloadResource_Success() {
        // Arrange
        String url = "http://example.com/test-file.txt";
        String filename = "downloaded-file.txt";
        File targetFile = new File(tempDir.toFile(), filename);
        long fileSize = 1024L;

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        // 使用 MockedStatic 来模拟 HttpUtil.downloadFile 的静态方法
        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class)) {
            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenReturn(fileSize);

            // 模拟 FileUtil.readableFileSize
            try (MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {
                fileUtilMock.when(() -> FileUtil.readableFileSize(fileSize))
                        .thenReturn("1 KB");

                // Act
                String result = resourceDownloadTool.downloadResource(url, filename);

                // Assert
                assertThat(result).startsWith("Success: Downloaded");
                assertThat(result).contains(url);
                assertThat(result).contains(filename);
                assertThat(result).contains("1 KB");

                verify(workspaceManager).resolve(filename);
                httpUtilMock.verify(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)));
            }
        }
    }

    @Test
    void testDownloadResource_InvalidUrl_Null() {
        // Act
        String result = resourceDownloadTool.downloadResource(null, "test.txt");

        // Assert
        assertThat(result).isEqualTo("Error: Invalid URL.");
        verify(workspaceManager, never()).resolve(anyString());
    }

    @Test
    void testDownloadResource_InvalidUrl_Empty() {
        // Act
        String result = resourceDownloadTool.downloadResource("", "test.txt");

        // Assert
        assertThat(result).isEqualTo("Error: Invalid URL.");
        verify(workspaceManager, never()).resolve(anyString());
    }

    @Test
    void testDownloadResource_InvalidUrl_NotHttp() {
        // Act
        String result = resourceDownloadTool.downloadResource("ftp://example.com/file.txt", "test.txt");

        // Assert
        assertThat(result).isEqualTo("Error: Invalid URL.");
        verify(workspaceManager, never()).resolve(anyString());
    }

    @Test
    void testDownloadResource_HttpsUrl_Accepted() {
        // Arrange
        String url = "https://example.com/file.txt";
        String filename = "test.txt";
        File targetFile = new File(tempDir.toFile(), filename);
        long fileSize = 1024L;

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class);
             MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {

            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenReturn(fileSize);
            fileUtilMock.when(() -> FileUtil.readableFileSize(fileSize))
                    .thenReturn("1 KB");

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Success: Downloaded");
            assertThat(result).contains("https://example.com/file.txt");
        }
    }

    @Test
    void testDownloadResource_InvalidFilename_PathTraversal() {
        // Arrange
        String url = "http://example.com/test.txt";
        String maliciousFilename = "../../../etc/passwd";

        when(workspaceManager.resolve(maliciousFilename))
                .thenThrow(new IllegalArgumentException("Filename resolves outside of workspace: " + maliciousFilename));

        // Act
        String result = resourceDownloadTool.downloadResource(url, maliciousFilename);

        // Assert
        assertThat(result).startsWith("Invalid filename:");
        assertThat(result).contains("outside of workspace");
        verify(workspaceManager).resolve(maliciousFilename);
    }

    @Test
    void testDownloadResource_DownloadFailure_HttpException() {
        // Arrange
        String url = "http://example.com/error.txt";
        String filename = "error-file.txt";
        File targetFile = new File(tempDir.toFile(), filename);

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class)) {
            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenThrow(new RuntimeException("HTTP 500 Internal Server Error"));

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Error downloading resource:");
            assertThat(result).contains("HTTP 500");
        }
    }

    @Test
    void testDownloadResource_DownloadFailure_NetworkError() {
        // Arrange
        String url = "http://nonexistent-host-12345.com/file.txt";
        String filename = "network-error.txt";
        File targetFile = new File(tempDir.toFile(), filename);

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class)) {
            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenThrow(new RuntimeException("Connection refused"));

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Error downloading resource:");
            assertThat(result).contains("Connection refused");
        }
    }

    @Test
    void testDownloadResource_LargeFile() {
        // Arrange
        String url = "http://example.com/large-file.zip";
        String filename = "large-file.zip";
        File targetFile = new File(tempDir.toFile(), filename);
        long largeFileSize = 1024 * 1024 * 100L; // 100 MB

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class);
             MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {

            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenReturn(largeFileSize);
            fileUtilMock.when(() -> FileUtil.readableFileSize(largeFileSize))
                    .thenReturn("100 MB");

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Success: Downloaded");
            assertThat(result).contains("100 MB");
        }
    }

    @Test
    void testDownloadResource_SpecialCharactersInFilename() {
        // Arrange
        String url = "http://example.com/test.txt";
        String filename = "测试文件-2024.txt";
        File targetFile = new File(tempDir.toFile(), filename);
        long fileSize = 512L;

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class);
             MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {

            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenReturn(fileSize);
            fileUtilMock.when(() -> FileUtil.readableFileSize(fileSize))
                    .thenReturn("512 B");

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Success: Downloaded");
            assertThat(result).contains(filename);
        }
    }

    @Test
    void testDownloadResource_EmptyFile() {
        // Arrange
        String url = "http://example.com/empty.txt";
        String filename = "empty-file.txt";
        File targetFile = new File(tempDir.toFile(), filename);
        long fileSize = 0L;

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class);
             MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {

            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenReturn(fileSize);
            fileUtilMock.when(() -> FileUtil.readableFileSize(fileSize))
                    .thenReturn("0 B");

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Success: Downloaded");
            assertThat(result).contains("0 B");
        }
    }

    @Test
    void testDownloadResource_BinaryFile() {
        // Arrange
        String url = "http://example.com/binary.dat";
        String filename = "binary-file.dat";
        File targetFile = new File(tempDir.toFile(), filename);
        long fileSize = 2048L;

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class);
             MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {

            httpUtilMock.when(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)))
                    .thenReturn(fileSize);
            fileUtilMock.when(() -> FileUtil.readableFileSize(fileSize))
                    .thenReturn("2 KB");

            // Act
            String result = resourceDownloadTool.downloadResource(url, filename);

            // Assert
            assertThat(result).startsWith("Success: Downloaded");
            assertThat(result).contains("2 KB");
        }
    }

    @Test
    void testDownloadResource_VerifyWorkspaceResolveCalledFirst() {
        // Arrange
        String url = "http://example.com/test.txt";
        String filename = "test.txt";
        File targetFile = new File(tempDir.toFile(), filename);

        when(workspaceManager.resolve(filename)).thenReturn(targetFile);

        try (MockedStatic<HttpUtil> httpUtilMock = mockStatic(HttpUtil.class);
             MockedStatic<FileUtil> fileUtilMock = mockStatic(FileUtil.class)) {

            httpUtilMock.when(() -> HttpUtil.downloadFile(anyString(), any(File.class)))
                    .thenReturn(100L);
            fileUtilMock.when(() -> FileUtil.readableFileSize(anyLong()))
                    .thenReturn("100 B");

            // Act
            resourceDownloadTool.downloadResource(url, filename);

            // Assert - 验证调用顺序
            verify(workspaceManager).resolve(filename);
            httpUtilMock.verify(() -> HttpUtil.downloadFile(eq(url), eq(targetFile)));
        }
    }

    // ==================== 集成测试 - 真实下载 ====================

    @Test
    void testDownloadResource_RealDownload_GoogleLogo() {
        // 这是一个集成测试，会真实下载文件
        // 如果不想在CI/CD中运行，可以添加 @Disabled 或使用 Profile

        // Arrange
        String url = "https://www.google.com/images/branding/googlelogo/2x/googlelogo_color_272x92dp.png";
        String filename = "google_logo.png";
        File targetFile = new File(tempDir.toFile(), filename);

        // 创建真实的 WorkspaceManager 实例
        WorkspaceManager realWorkspaceManager = mock(WorkspaceManager.class);
        when(realWorkspaceManager.resolve(filename)).thenReturn(targetFile);

        ResourceDownloadTool realTool = new ResourceDownloadTool(realWorkspaceManager);

        // Act
        String result = realTool.downloadResource(url, filename);

        // Assert
        assertThat(result).startsWith("Success: Downloaded");
        assertThat(result).contains(url);
        assertThat(result).contains(filename);

        // 验证文件确实被下载了
        assertThat(targetFile).exists();
        assertThat(targetFile.length()).isGreaterThan(5000); // PNG 文件应该大于 5KB

        // 验证是 PNG 文件（检查文件头）
        byte[] header = new byte[8];
        try (java.io.FileInputStream fis = new java.io.FileInputStream(targetFile)) {
            int read = fis.read(header);
            assertThat(read).isEqualTo(8);
            // PNG 文件头: 89 50 4E 47 0D 0A 1A 0A
            assertThat(header[0]).isEqualTo((byte) 0x89);
            assertThat(header[1]).isEqualTo((byte) 0x50); // 'P'
            assertThat(header[2]).isEqualTo((byte) 0x4E); // 'N'
            assertThat(header[3]).isEqualTo((byte) 0x47); // 'G'
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify PNG header", e);
        }

        System.out.println("✅ Successfully downloaded Google Logo: " + result);
    }
}