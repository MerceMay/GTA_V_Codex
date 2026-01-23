package com.mercemay.aiagent.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppCustomTextSplitter {
    public List<Document> customizedSplitter(List<Document> documents) {
        TokenTextSplitter splitter = new TokenTextSplitter(
                1000, // chunkSize: max tokens per chunk
                100, // minChunkSizeChars: min characters per chunk
                50,   // minChunkLengthToEmbed: min tokens to consider for embedding
                10000, // maxNumChunks: max number of chunks to generate
                true // keepSeparator: whether to keep separator tokens
        );
        return splitter.split(documents);
    }
}
