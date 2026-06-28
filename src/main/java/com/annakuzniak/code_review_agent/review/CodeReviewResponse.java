package com.annakuzniak.code_review_agent.review;

import java.util.List;

public record CodeReviewResponse(
        String summary,
        List<String> bugs,
        List<String> securityIssues,
        List<String> suggestions,
        Severity severity
) {
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public String toMarkdown() {
        StringBuilder sb = new StringBuilder();
        sb.append("## 🤖 AI Code Review\n\n");
        sb.append("**Summary:** ").append(summary).append("\n\n");
        sb.append("**Severity:** ").append(severity).append("\n\n");

        if (!bugs.isEmpty()) {
            sb.append("### 🐛 Bugs\n");
            bugs.forEach(bug -> sb.append("- ").append(bug).append("\n"));
            sb.append("\n");
        }

        if (!securityIssues.isEmpty()) {
            sb.append("### 🔒 Security Issues\n");
            securityIssues.forEach(issue -> sb.append("- ").append(issue).append("\n"));
            sb.append("\n");
        }

        if (!suggestions.isEmpty()) {
            sb.append("### 💡 Suggestions\n");
            suggestions.forEach(suggestion -> sb.append("- ").append(suggestion).append("\n"));
        }

        return sb.toString();
    }
}