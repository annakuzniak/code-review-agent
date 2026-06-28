package com.annakuzniak.code_review_agent.webhook;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PullRequestEvent(
        String action,
        @JsonProperty("pull_request") PullRequest pullRequest,
        Repository repository
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PullRequest(
            int number,
            String title,
            String state
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Repository(
            String name,
            @JsonProperty("full_name") String fullName,
            Owner owner
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Owner(
            String login
    ) {}
}