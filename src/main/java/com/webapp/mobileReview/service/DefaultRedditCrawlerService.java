package com.webapp.mobileReview.service;

import net.dean.jraw.RedditClient;
import net.dean.jraw.http.NetworkAdapter;
import net.dean.jraw.http.OkHttpNetworkAdapter;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.models.SearchSort;
import net.dean.jraw.oauth.Credentials;
import net.dean.jraw.oauth.OAuthHelper;
import net.dean.jraw.models.Submission;
import net.dean.jraw.pagination.SearchPaginator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


@Service
public class DefaultRedditCrawlerService implements RedditCrawlerService {

    @Value("${reddit.username}")
    private String username;

    @Value("${reddit.password}")
    private String password;

    @Value("${reddit.client.id}")
    private String clientId;

    @Value("${reddit.client.secret}")
    private String clientSecret;

    private RedditClient redditClient;
    private final RateLimiterService rateLimiterService;
    private static final Logger logger = LoggerFactory.getLogger(DefaultRedditCrawlerService.class);

    @Autowired
    public DefaultRedditCrawlerService(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public List<String> crawlReviews(String mobileMake, String mobileModel, String year) {
        if (redditClient == null) {
            initializeRedditClient();
        }
        String searchQuery = String.format("%s %s %s review", year, mobileMake, mobileModel);
        String[] subreddits = {"mobiles", mobileMake, "Smartphones"};
        List<String> allReviewTexts = new ArrayList<>();

        for (String subredditName : subreddits) {
            if (!rateLimiterService.allowRequest()) {
                break;
            }

            try {
                SearchPaginator paginator = redditClient.subreddit(subredditName)
                        .search()
                        .query(searchQuery)
                        .sorting(SearchSort.RELEVANCE)
                        .limit(10)
                        .build();

                List<Submission> submissions = paginator.accumulate(10)
                        .stream()
                        .flatMap(List::stream)
                        .toList();

                for (Submission submission : submissions) {
                    String reviewText = extractReviewText(submission);
                    if (!reviewText.isEmpty()) {
                        allReviewTexts.add(reviewText);
                    }
                }
            } catch (Exception e) {
                logger.error("Error crawling reviews from subreddit {}", subredditName, e);
            }
        }

        return allReviewTexts;
    }

    private String extractReviewText(Submission submission) {
        StringBuilder reviewText = new StringBuilder();

        if (submission.getTitle().toLowerCase().contains("review")) {
            reviewText.append(submission.getTitle()).append("\n");
        }

        if (submission.getSelfText() != null && !submission.getSelfText().trim().isEmpty()) {
            reviewText.append(submission.getSelfText());
        }

        return reviewText.toString().trim();
    }

    private void initializeRedditClient() {
        NetworkAdapter adapter = new OkHttpNetworkAdapter(
                new UserAgent("bot", "com.mobilereview", "v0.1", username)
        );

        Credentials credentials = Credentials.script(
                username,
                password,
                clientId,
                clientSecret
        );
        redditClient = OAuthHelper.automatic(adapter, credentials);
    }
}

