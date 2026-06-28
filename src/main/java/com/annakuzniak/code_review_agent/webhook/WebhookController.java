package com.annakuzniak.code_review_agent.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody String payload) {

        log.info("Received GitHub event: {}", eventType);

        if (!"pull_request".equals(eventType)) {
            log.info("Ignoring event type: {}", eventType);
            return ResponseEntity.ok("Event ignored");
        }

        log.info("Pull request event received, triggering review...");
        // CodeReviewService will be called here once we build it
        return ResponseEntity.ok("Review triggered");
    }
}