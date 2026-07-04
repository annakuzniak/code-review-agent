package com.annakuzniak.code_review_agent.review;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CodeReviewResponseTest {

    @Test
    void shouldIncludeSummaryAndSeverityInMarkdown() {
        CodeReviewResponse response = new CodeReviewResponse(
                "This PR adds a payment service",
                List.of(),
                List.of(),
                List.of(),
                CodeReviewResponse.Severity.LOW
        );

        String markdown = response.toMarkdown();

        assertThat(markdown).contains("This PR adds a payment service");
        assertThat(markdown).contains("LOW");
        assertThat(markdown).contains("🤖 AI Code Review");
    }

    @Test
    void shouldIncludeBugsSection() {
        CodeReviewResponse response = new CodeReviewResponse(
                "Summary",
                List.of("Null pointer on line 42", "Missing transaction"),
                List.of(),
                List.of(),
                CodeReviewResponse.Severity.HIGH
        );

        String markdown = response.toMarkdown();

        assertThat(markdown).contains("🐛 Bugs");
        assertThat(markdown).contains("Null pointer on line 42");
        assertThat(markdown).contains("Missing transaction");
    }

    @Test
    void shouldIncludeSecuritySection() {
        CodeReviewResponse response = new CodeReviewResponse(
                "Summary",
                List.of(),
                List.of("SQL injection risk on line 15"),
                List.of(),
                CodeReviewResponse.Severity.CRITICAL
        );

        String markdown = response.toMarkdown();

        assertThat(markdown).contains("🔒 Security Issues");
        assertThat(markdown).contains("SQL injection risk on line 15");
    }

    @Test
    void shouldOmitEmptySections() {
        CodeReviewResponse response = new CodeReviewResponse(
                "Summary",
                List.of(),
                List.of(),
                List.of(),
                CodeReviewResponse.Severity.LOW
        );

        String markdown = response.toMarkdown();

        assertThat(markdown).doesNotContain("🐛 Bugs");
        assertThat(markdown).doesNotContain("🔒 Security Issues");
        assertThat(markdown).doesNotContain("💡 Suggestions");
    }
}