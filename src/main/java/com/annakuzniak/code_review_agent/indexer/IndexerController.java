package com.annakuzniak.code_review_agent.indexer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/indexer")
public class IndexerController {

    private static final Logger log = LoggerFactory.getLogger(IndexerController.class);
    private final CodebaseIndexer codebaseIndexer;

    public IndexerController(CodebaseIndexer codebaseIndexer) {
        this.codebaseIndexer = codebaseIndexer;
    }

    //This is a manual endpoint to index the codebase. It's used to index the codebase when the application is started.
    @PostMapping("/index")
    public ResponseEntity<String> indexCodebase() {
        log.info("Manual indexing triggered");
        new Thread(codebaseIndexer::indexCodebase).start();
        return ResponseEntity.ok("Indexing started");
    }
}