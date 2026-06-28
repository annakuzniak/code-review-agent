package com.annakuzniak.code_review_agent.review;

import com.annakuzniak.code_review_agent.github.GitHubClient;
import com.annakuzniak.code_review_agent.webhook.PullRequestEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class CodeReviewService {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewService.class);

    private final GitHubClient gitHubClient;
    private final CodeReviewAgent codeReviewAgent;
    private final VectorStore vectorStore;

    public CodeReviewService(GitHubClient gitHubClient,
                             CodeReviewAgent codeReviewAgent,
                             VectorStore vectorStore) {
        this.gitHubClient = gitHubClient;
        this.codeReviewAgent = codeReviewAgent;
        this.vectorStore = vectorStore;
    }

    public void handlePullRequestEvent(PullRequestEvent event) {
        String action = event.action();

        // Only review when PR is opened or new commits are pushed
        if (!"opened".equals(action) && !"synchronize".equals(action)) {
            log.info("Ignoring PR action: {}", action);
            return;
        }

        String owner = event.repository().owner().login();
        String repo = event.repository().name();
        int prNumber = event.pullRequest().number();
        String title = event.pullRequest().title();

        log.info("Reviewing PR #{}: {} in {}/{}", prNumber, title, owner, repo);

        // Step 1: Fetch the PR diff
        String diff = gitHubClient.getPullRequestDiff(owner, repo, prNumber);
        log.info("Fetched diff ({} characters)", diff.length());

        // Step 2: Search pgvector for similar code context
        String codebaseContext = getRelevantContext(diff);
        log.info("Retrieved codebase context ({} characters)", codebaseContext.length());

        // Step 3: Ask Claude to review
        CodeReviewResponse review = codeReviewAgent.review(diff, codebaseContext);
        log.info("Review completed with severity: {}", review.severity());

        // Step 4: Post review comment to GitHub
        String comment = review.toMarkdown();
        gitHubClient.postReviewComment(owner, repo, prNumber, comment);
        log.info("Review posted to PR #{}", prNumber);
    }

    private String getRelevantContext(String diff) {
        try {
            var results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(diff)
                            .topK(3)
                            .build()
            );

            if (results.isEmpty()) {
                return "";
            }

            StringBuilder context = new StringBuilder();
            results.forEach(doc -> {
                context.append("--- Relevant code from codebase ---\n");
                context.append(doc.getText());
                context.append("\n\n");
            });

            return context.toString();
        } catch (Exception e) {
            log.warn("Could not retrieve codebase context: {}", e.getMessage());
            return "";
        }
    }
}