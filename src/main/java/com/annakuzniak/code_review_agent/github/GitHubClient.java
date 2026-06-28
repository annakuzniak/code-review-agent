package com.annakuzniak.code_review_agent.github;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GitHubClient {

    private static final Logger log = LoggerFactory.getLogger(GitHubClient.class);

    private final RestClient restClient;

    public GitHubClient(@Value("${github.token}") String githubToken) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.github.com")
                .defaultHeader("Authorization", "Bearer " + githubToken)
                .defaultHeader("Accept", "application/vnd.github+json")
                .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
                .build();
    }

    public String getPullRequestDiff(String owner, String repo, int prNumber) {
        log.info("Fetching diff for PR #{} in {}/{}", prNumber, owner, repo);

        return restClient.get()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}", owner, repo, prNumber)
                .header("Accept", "application/vnd.github.diff")
                .retrieve()
                .body(String.class);
    }

    public void postReviewComment(String owner, String repo, int prNumber, String body) {
        log.info("Posting review comment to PR #{} in {}/{}", prNumber, owner, repo);

        var request = new ReviewCommentRequest("COMMENT", body);

        restClient.post()
                .uri("/repos/{owner}/{repo}/pulls/{prNumber}/reviews", owner, repo, prNumber)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    record ReviewCommentRequest(String event, String body) {}
}