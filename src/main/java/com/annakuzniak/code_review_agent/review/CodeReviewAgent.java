package com.annakuzniak.code_review_agent.review;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

@Component
public class CodeReviewAgent {

    private static final Logger log = LoggerFactory.getLogger(CodeReviewAgent.class);

    private final ChatClient chatClient;

    public CodeReviewAgent(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    public CodeReviewResponse review(String diff, String codebaseContext) {
        log.info("Sending PR diff to Claude for review...");

        var outputConverter = new BeanOutputConverter<>(CodeReviewResponse.class);

        String promptText = """
                You are an expert Java and Spring Boot code reviewer with deep knowledge of:
                - Spring Boot best practices and patterns
                - Domain Driven Design (DDD)
                - Microservice architecture
                - Security vulnerabilities in Java applications
                - Common Java anti-patterns
                
                Review the following Pull Request diff carefully.
                
                CODEBASE CONTEXT (similar code from the existing codebase):
                {codebaseContext}
                
                PULL REQUEST DIFF:
                {diff}
                
                Provide a thorough review focusing on:
                1. Bugs and logical errors
                2. Security vulnerabilities (SQL injection, missing validation, exposed secrets etc.)
                3. Spring Boot specific issues (missing transactions, improper exception handling etc.)
                4. DDD violations or microservice anti-patterns
                5. Code quality improvements
                
                {format}
                """;

        var template = new PromptTemplate(promptText);
        Prompt prompt = template.create(java.util.Map.of(
                "diff", diff,
                "codebaseContext", codebaseContext.isEmpty() ? "No context available" : codebaseContext,
                "format", outputConverter.getFormat()
        ));

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        log.info("Received review from Claude");
        return outputConverter.convert(response);
    }
}