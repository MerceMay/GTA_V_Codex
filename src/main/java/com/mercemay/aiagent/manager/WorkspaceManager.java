package com.mercemay.aiagent.manager;

import cn.hutool.core.io.FileUtil;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.UUID;

@Getter
@Service
@Slf4j
public class WorkspaceManager implements DisposableBean {
    private File workspaceRoot;

    @PostConstruct
    public void init() {
        this.workspaceRoot = new File(System.getProperty("java.io.tmpdir") +
                File.separator + "aiagent-shared-" + UUID.randomUUID());
        FileUtil.mkdir(this.workspaceRoot);
        log.info("Created shared workspace directory at {}", this.workspaceRoot.getAbsolutePath());
    }

    /**
     * Resolve a filename within the shared workspace.
     *
     * @param filename The filename to resolve.
     * @return The resolved File object within the shared workspace.
     */
    public File resolve(String filename) {
        File targetFile = FileUtil.file(this.workspaceRoot, filename);
        // Ensure the resolved file is within the workspace
        if (!FileUtil.isSub(this.workspaceRoot, targetFile)) {
            throw new IllegalArgumentException("Filename resolves outside of workspace: " + filename);
        }
        return targetFile;
    }

    /**
     * Cleans up the shared workspace directory upon bean destruction.
     */
    @Override
    public void destroy() {
        if (workspaceRoot != null && workspaceRoot.exists()) {
            FileUtil.del(workspaceRoot);
            System.out.println("Global AI Workspace cleaned up.");
        }
    }
}
