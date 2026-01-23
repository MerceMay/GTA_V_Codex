package com.mercemay.aiagent.manager;

import cn.hutool.core.io.FileUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class WorkspaceManagerTest {

    private WorkspaceManager workspaceManager;

    @BeforeEach
    void setUp() {
        workspaceManager = new WorkspaceManager();
        workspaceManager.init();
    }

    @AfterEach
    void tearDown() {
        workspaceManager.destroy();
    }

    @Test
    void testInitCreatesWorkspaceDirectory() {
        assertNotNull(workspaceManager.getWorkspaceRoot());
        assertTrue(workspaceManager.getWorkspaceRoot().exists());
        assertTrue(workspaceManager.getWorkspaceRoot().isDirectory());
        assertTrue(workspaceManager.getWorkspaceRoot().getName().startsWith("aiagent-shared-"));
    }

    @Test
    void testResolveValidFilename() {
        File resolved = workspaceManager.resolve("test.txt");
        assertNotNull(resolved);
        assertTrue(resolved.getAbsolutePath().startsWith(
                workspaceManager.getWorkspaceRoot().getAbsolutePath()));
    }

    @Test
    void testResolveNestedFilename() {
        File resolved = workspaceManager.resolve("subdir/test.txt");
        assertNotNull(resolved);
        assertTrue(resolved.getAbsolutePath().contains("subdir"));
    }

    @Test
    void testResolvePathTraversalAttack() {
        assertThrows(IllegalArgumentException.class, () -> {
            workspaceManager.resolve("../../../etc/passwd");
        });
    }

    @Test
    void testResolveAbsolutePathOutsideWorkspace() {
        String systemTemp = System.getProperty("java.io.tmpdir");
        String outsidePath = new File(systemTemp).getParent() + File.separator + "outside.txt";

        assertThrows(IllegalArgumentException.class, () -> {
            workspaceManager.resolve(outsidePath);
        });
    }

    @Test
    void testDestroyDeletesWorkspace() {
        File workspaceRoot = workspaceManager.getWorkspaceRoot();

        // Create a test file in workspace
        File testFile = new File(workspaceRoot, "test.txt");
        FileUtil.writeUtf8String("test content", testFile);

        assertTrue(workspaceRoot.exists());
        assertTrue(testFile.exists());

        workspaceManager.destroy();

        assertFalse(workspaceRoot.exists());
    }

    @Test
    void testDestroyWithNullWorkspace() {
        WorkspaceManager manager = new WorkspaceManager();
        // Don't call init(), workspace should be null
        assertDoesNotThrow(() -> manager.destroy());
    }
}