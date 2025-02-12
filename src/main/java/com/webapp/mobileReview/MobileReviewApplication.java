package com.webapp.mobileReview;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MobileReviewApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobileReviewApplication.class, args);
	}

}
