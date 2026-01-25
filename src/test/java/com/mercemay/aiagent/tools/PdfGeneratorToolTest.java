package com.mercemay.aiagent.tools;

import com.mercemay.aiagent.manager.WorkspaceManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class PdfGeneratorToolTest {

    @Autowired
    private WorkspaceManager workspaceManager;

    @Test
    void generatePdf() {
        PdfGeneratorTool pdfGeneratorTool = new PdfGeneratorTool(workspaceManager);

        String filename = "integration_test.pdf";
        String content = "你好，这是一个PDF生成测试。";

        String result = pdfGeneratorTool.generatePdf(filename, content);

        File expectedFile = workspaceManager.resolve(filename);

        assertTrue(result.contains(filename));
        assertTrue(expectedFile.exists());
    }
}