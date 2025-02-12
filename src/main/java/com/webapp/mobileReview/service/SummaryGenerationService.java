package com.webapp.mobileReview.service;

public interface SummaryGenerationService {
    String generateComprehensiveSummary(String combinedReviewText, String mobileMake, String mobileModel, String year);
}