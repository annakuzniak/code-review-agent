package com.annakuzniak.code_review_agent.webhook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.annakuzniak.code_review_agent.review.CodeReviewService;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    private final CodeReviewService codeReviewService;

    public WebhookController(CodeReviewService codeReviewService) {
        this.codeReviewService = codeReviewService;
    }

    @PostMapping("/github")
    public ResponseEntity<String> handleGithubWebhook(
            @RequestHeader("X-GitHub-Event") String eventType,
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody PullRequestEvent event) {

        log.info("Received GitHub event: {}", eventType);

        if (!"pull_request".equals(eventType)) {
            log.info("Ignoring event type: {}", eventType);
            return ResponseEntity.ok("Event ignored");
        }

        log.info("Pull request event received, triggering review...");
        new Thread(() -> codeReviewService.handlePullRequestEvent(event)).start();
        return ResponseEntity.ok("Review triggered");
    }
    
    //Let's see if this magic works
}