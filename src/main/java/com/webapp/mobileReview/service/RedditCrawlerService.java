package com.webapp.mobileReview.service;

import java.util.List;


public interface RedditCrawlerService {
    List<String> crawlReviews(String mobileMake, String mobileModel, String year);
}
