package com.aegorov.knowledgeplatform.indexingservice.application;

import com.aegorov.knowledgeplatform.indexingservice.config.ChunkingProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final ChunkingProperties chunkingProperties;

    public TextChunker(ChunkingProperties chunkingProperties) {
        this.chunkingProperties = chunkingProperties;
    }

    public List<String> chunk(String text) {
        String normalized = text == null ? "" : text.trim();
        if (normalized.isEmpty()) {
            return List.of();
        }

        int chunkSize = Math.max(200, chunkingProperties.maxChars());
        int overlap = Math.max(0, Math.min(chunkingProperties.overlapChars(), chunkSize - 1));
        int step = chunkSize - overlap;

        List<String> chunks = new ArrayList<>();
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + chunkSize, normalized.length());
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end == normalized.length()) {
                break;
            }
        }
        return chunks;
    }
}
