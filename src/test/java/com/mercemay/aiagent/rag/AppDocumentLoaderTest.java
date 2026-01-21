package com.mercemay.aiagent.rag;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class AppDocumentLoaderTest {
    @Resource
    private AppDocumentLoader appDocumentLoader;

    @Test
    void testLoadDocuments() {
        var documents = appDocumentLoader.loadDocuments();
        assert !documents.isEmpty();
    }
}
