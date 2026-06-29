package com.annakuzniak.code_review_agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CodeReviewAgentApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodeReviewAgentApplication.class, args);
	}

}
