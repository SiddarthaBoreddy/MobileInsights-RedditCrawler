package com.webapp.mobileReview.controller;

import com.webapp.mobileReview.service.MobileReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mobile-reviews")
@CrossOrigin(origins = {"http://localhost:3000", "http://127.0.0.1:3000"}) // Add both localhost and 127.0.0.1
public class MobileReviewController {
    private final MobileReviewService mobileReviewService;

    // Constructor injection
    @Autowired
    public MobileReviewController(MobileReviewService mobileReviewService) {
        this.mobileReviewService = mobileReviewService;
    }

    @GetMapping("/crawl")
    public String crawlMobileReviews(
            @RequestParam String make,
            @RequestParam String model,
            @RequestParam String year
    ) {
        return mobileReviewService.getOrCreateMobileReview(make, model, year);
    }
}