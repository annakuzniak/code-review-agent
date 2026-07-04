package com.annakuzniak.code_review_agent.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Component
public class CodebaseIndexer {

    private static final Logger log = LoggerFactory.getLogger(CodebaseIndexer.class);
    private static final int CHUNK_SIZE = 1500;
    private static final int CHUNK_OVERLAP = 200;

    private final VectorStore vectorStore;
    private final JdbcTemplate jdbcTemplate;
    private final String codebasePath;

    public CodebaseIndexer(VectorStore vectorStore,
                           JdbcTemplate jdbcTemplate,
                           @Value("${indexer.codebase-path:src/main/java}") String codebasePath) {
        this.vectorStore = vectorStore;
        this.jdbcTemplate = jdbcTemplate;
        this.codebasePath = codebasePath;
    }

    public void indexCodebase() {
        log.info("Clearing existing vectors...");
        jdbcTemplate.execute("DELETE FROM vector_store");

        log.info("Starting codebase indexing from: {}", codebasePath);
        int totalChunks = 0;

        try (Stream<Path> paths = Files.walk(Paths.get(codebasePath))) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .toList();

            for (Path file : javaFiles) {
                try {
                    String content = Files.readString(file);
                    List<Document> chunks = chunkFile(content, file.toString());
                    vectorStore.add(chunks);
                    totalChunks += chunks.size();
                    log.info("Indexed: {} ({} chunks)", file, chunks.size());
                } catch (Exception e) {
                    log.warn("Could not index file: {}", file, e);
                }
            }
        } catch (IOException e) {
            log.error("Error walking codebase path: {}", codebasePath, e);
            return;
        }

        log.info("Codebase indexing complete! Total chunks: {}", totalChunks);
    }

    private List<Document> chunkFile(String content, String filePath) {
        List<Document> chunks = new ArrayList<>();

        if (content.length() <= CHUNK_SIZE) {
            chunks.add(Document.builder().text(content).build());
            return chunks;
        }

        int start = 0;
        while (start < content.length()) {
            int end = Math.min(start + CHUNK_SIZE, content.length());

            if (end < content.length()) {
                int lastNewline = content.lastIndexOf('\n', end);
                if (lastNewline > start) {
                    end = lastNewline;
                }
            }

            String chunk = content.substring(start, end);
            chunks.add(Document.builder().text(chunk).build());

            int nextChunkStart = end - CHUNK_OVERLAP;
            if (nextChunkStart <= start) {
                nextChunkStart = start + CHUNK_SIZE;
            }
            start = nextChunkStart;
        }

        return chunks;
    }
}