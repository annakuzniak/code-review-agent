# Autonomous Code Review Agent

A production-grade AI agent that automatically reviews GitHub Pull Requests, identifies bugs and security vulnerabilities, and posts structured feedback — with context-aware analysis powered by RAG over your existing codebase.

Built with **Spring Boot 4.1**, **Spring AI 2.0**, and **Claude Sonnet** by Anthropic.

---

## What It Does

When a Pull Request is opened or updated, the agent:

1. Receives a GitHub webhook event
2. Validates the request signature (HMAC-SHA256) to prevent spoofing
3. Fetches the PR diff from the GitHub API
4. Searches the indexed codebase for similar patterns using vector similarity (RAG)
5. Sends the diff + codebase context to Claude for structured analysis
6. Posts a formatted review comment directly to the PR

The result is a review that understands your project's conventions, not just generic best practices.

---

## Architecture

```
GitHub PR Event
      │
      ▼
WebhookSignatureFilter          ← HMAC-SHA256 signature validation
      │
      ▼
WebhookController               ← Receives and parses PR webhook payload
      │
      ▼ (async - @Async + ThreadPoolTaskExecutor)
CodeReviewService               ← Orchestrates the full review pipeline
      │
      ├──► GitHubClient         ← Fetches PR diff from GitHub API
      │
      ├──► VectorStore          ← Searches pgvector for similar codebase context (RAG)
      │         ▲
      │         │ (populated by)
      │    CodebaseIndexer      ← Reads Java files, chunks them, stores embeddings
      │
      ├──► CodeReviewAgent      ← Sends diff + context to Claude, parses structured response
      │
      └──► GitHubClient         ← Posts review comment back to GitHub PR
```

### Package Structure

```
com.annakuzniak.code_review_agent
├── config/
│   └── AppConfig.java               # Thread pool configuration
├── github/
│   └── GitHubClient.java            # GitHub API client (diffs + comments)
├── indexer/
│   ├── CodebaseIndexer.java         # Codebase chunking and vector storage
│   └── IndexerController.java       # REST endpoint to trigger indexing
├── review/
│   ├── CodeReviewAgent.java         # Spring AI + Claude integration
│   ├── CodeReviewResponse.java      # Structured review record (bugs, security, suggestions)
│   └── CodeReviewService.java       # Review pipeline orchestrator
└── webhook/
    ├── PullRequestEvent.java        # GitHub webhook payload model
    ├── WebhookController.java       # Webhook entry point
    └── WebhookSignatureFilter.java  # Security filter
```

---

## Tech Stack

| Component | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1.0 |
| AI Integration | Spring AI 2.0, Anthropic Claude Sonnet |
| Vector Store | pgvector (PostgreSQL extension) |
| Embeddings | Ollama (`nomic-embed-text`, local) |
| GitHub Integration | GitHub Webhooks API, GitHub REST API |
| Security | HMAC-SHA256 webhook signature validation |
| Async Processing | Spring `@Async`, `ThreadPoolTaskExecutor` |
| Infrastructure | Docker (pgvector container) |

---

## Running Locally

### Prerequisites

- Java 25 (Temurin)
- Docker Desktop
- Ollama with `nomic-embed-text` model
- Anthropic API key
- GitHub Personal Access Token (repo scope)
- ngrok (for local webhook testing)

### Setup

**1. Start pgvector:**
```bash
docker run --name pgvector \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=secret \
  -e POSTGRES_DB=codereviewer \
  -p 5432:5432 \
  -d pgvector/pgvector:pg17
```

**2. Pull the embedding model:**
```bash
ollama pull nomic-embed-text
```

**3. Configure secrets** — create `src/main/resources/application-local.yaml`:
```yaml
spring:
  ai:
    anthropic:
      api-key: YOUR_ANTHROPIC_API_KEY

github:
  token: YOUR_GITHUB_TOKEN
  webhook-secret: YOUR_WEBHOOK_SECRET
```

**4. Start the application:**
```bash
./dev-start.sh
```

**5. Index your codebase:**
```bash
curl -X POST http://localhost:8080/indexer/index
```

**6. Expose locally via ngrok:**
```bash
ngrok http 8080
```

**7. Configure GitHub webhook** in your repository settings:
- Payload URL: `https://YOUR_NGROK_URL/webhook/github`
- Content type: `application/json`
- Secret: your webhook secret
- Events: Pull requests only

---

## Example Review Output

```markdown
## 🤖 AI Code Review

**Summary:** This PR introduces a payment service with a potential null pointer
exception and missing transaction boundary.

**Severity:** HIGH

### 🐛 Bugs
- Line 47: `payment.getAmount()` can return null if the payment object is
  constructed without an amount — add null check before processing
- The existing codebase uses Optional for nullable returns (see CustomerService),
  this method should follow the same convention

### 🔒 Security Issues
- The payment amount is logged at INFO level on line 52, which may expose
  sensitive financial data in log aggregation systems

### 💡 Suggestions
- Add `@Transactional` to `processPayment()` — without it, a failure midway
  through will leave the database in an inconsistent state
- Consider extracting the validation logic into a dedicated PaymentValidator
  following the pattern used in OrderService
```

---

## What's Next

- Persist review history to PostgreSQL for analytics and trend tracking
- Re-index codebase automatically on push events (not just manual trigger)
- Add severity thresholds — only post comments for HIGH and CRITICAL findings
- Support multi-repository configurations
