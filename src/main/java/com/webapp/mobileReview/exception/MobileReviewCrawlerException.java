package com.webapp.mobileReview.exception;

public class MobileReviewCrawlerException extends RuntimeException {
    public MobileReviewCrawlerException(String message) {
        super(message);
    }
    public MobileReviewCrawlerException(String message, Throwable cause) {
        super(message, cause);
    }
}
