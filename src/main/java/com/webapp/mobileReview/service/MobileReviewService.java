package com.webapp.mobileReview.service;

public interface MobileReviewService {
    String getOrCreateMobileReview(String mobileMake, String mobileModel, String year);
    void updateExistingMobileReviews();
}
