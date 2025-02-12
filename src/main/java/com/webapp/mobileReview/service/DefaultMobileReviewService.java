package com.webapp.mobileReview.service;

import com.webapp.mobileReview.exception.MobileReviewCrawlerException;
import com.webapp.mobileReview.exception.DuplicateReviewException;
import com.webapp.mobileReview.exception.RateLimitExceededException;
import com.webapp.mobileReview.model.MobileReview;
import com.webapp.mobileReview.repository.MobileReviewRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.slf4j.Logger;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
public class DefaultMobileReviewService implements MobileReviewService {
    private static final Logger logger = LoggerFactory.getLogger(DefaultMobileReviewService.class);
    private static final long MINIMUM_UPDATE_INTERVAL_HOURS = 10;

    private final MobileReviewRepository mobileReviewRepository;
    private final RedditCrawlerService redditCrawlerService;
    private final SummaryGenerationService summaryGenerationService;
    private final RateLimiterService rateLimiterService;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public DefaultMobileReviewService(
            MobileReviewRepository mobileReviewRepository,
            RedditCrawlerService redditCrawlerService,
            SummaryGenerationService summaryGenerationService,
            RateLimiterService rateLimiterService,
            MongoTemplate mongoTemplate
    ) {
        this.mobileReviewRepository = mobileReviewRepository;
        this.redditCrawlerService = redditCrawlerService;
        this.summaryGenerationService = summaryGenerationService;
        this.rateLimiterService = rateLimiterService;
        this.mongoTemplate = mongoTemplate;
    }

    @PostConstruct
    public void initializeIndexes() {
        mongoTemplate.indexOps(MobileReview.class)
                .ensureIndex(new Index().on("title", Sort.Direction.ASC).unique());
        logger.info("Initialized unique index on title field");
    }

    private String normalizeTitle(String year, String mobileMake, String mobileModel) {
        return String.format("%s %s %s Reviews",
                year.trim(),
                mobileMake.trim(),
                mobileModel.trim()
        ).toLowerCase();
    }

    @Override
    public String getOrCreateMobileReview(String mobileMake, String mobileModel, String year) {
        String normalizedTitle = normalizeTitle(year, mobileMake, mobileModel);

        try {
            Optional<MobileReview> existingReview = mobileReviewRepository.findByTitleIgnoreCase(normalizedTitle);

            if (existingReview.isPresent()) {
                MobileReview review = existingReview.get();

                // Generate new summary if content has changed or summary doesn't exist
                if (review.isContentChanged() ||
                        review.getSummarizedText() == null ||
                        review.getSummarizedText().isEmpty()) {

                    String newSummary = summaryGenerationService.generateComprehensiveSummary(
                            review.getOriginalText(), mobileMake, mobileModel, year
                    );
                    review.setSummarizedText(newSummary);
                    review.setContentChanged(false);  // Reset the flag
                    mobileReviewRepository.save(review);
                    return newSummary;
                }

                return review.getSummarizedText();
            }

            // If no review exists, create a new one
            return crawlAndCreateNewReview(mobileMake, mobileModel, year);

        } catch (DuplicateKeyException e) {
            logger.error("Duplicate review title detected: {}", normalizedTitle);
            throw new DuplicateReviewException("A review with this title already exists");
        } catch (Exception e) {
            logger.error("Error retrieving or creating mobile review for {}", normalizedTitle, e);
            throw new MobileReviewCrawlerException("Failed to process mobile review", e);
        }
    }

    @Override
    @Scheduled(fixedRate = 3600000) // 1 hour
    public void updateExistingMobileReviews() {
        List<MobileReview> existingReviews = mobileReviewRepository.findAll();

        for (MobileReview existingReview : existingReviews) {
            String[] titleParts = existingReview.getTitle().split(" ");
            if (titleParts.length < 3) {
                logger.warn("Invalid review title format: {}", existingReview.getTitle());
                continue;
            }

            String year = titleParts[0];
            String mobileMake = titleParts[1];
            String mobileModel = titleParts[2];

            if (!isReviewRecent(existingReview)) {
                try {
                    synchronized(this) {
                        updateOriginalText(existingReview, mobileMake, mobileModel, year);
                    }
                } catch (Exception e) {
                    logger.error("Error updating review for {}: {}",
                            existingReview.getTitle(), e.getMessage());
                }
            }
        }
    }

    private void updateOriginalText(MobileReview review, String mobileMake, String mobileModel, String year) {
        if (!rateLimiterService.allowRequest()) {
            throw new RateLimitExceededException("Rate limit exceeded for mobile review crawling");
        }

        List<String> newReviewTexts = redditCrawlerService.crawlReviews(mobileMake, mobileModel, year);
        String newCombinedText = String.join("\n\n", newReviewTexts);

        // Normalize both texts for comparison (remove extra whitespace, trim, etc.)
        String normalizedNewText = normalizeText(newCombinedText);
        String normalizedOldText = normalizeText(review.getOriginalText());

        // Compare normalized texts
        if (!normalizedNewText.equals(normalizedOldText)) {
            review.setOriginalText(newCombinedText);
            review.setLastUpdated(LocalDateTime.now());
            // Set flag to indicate content has changed and summary needs update
            review.setContentChanged(true);
            mobileReviewRepository.save(review);
            logger.info("Updated original text for {} due to content change", review.getTitle());
        } else {
            // Update only the timestamp if content hasn't changed
            review.setLastUpdated(LocalDateTime.now());
            mobileReviewRepository.save(review);
            logger.debug("Updated timestamp only for {} as content unchanged", review.getTitle());
        }
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        // Remove extra whitespace, normalize line endings, and trim
        return text.replaceAll("\\s+", " ")
                .replaceAll("\\R", "\n")
                .trim();
    }

    private String crawlAndCreateNewReview(String mobileMake, String mobileModel, String year) {
        if (!rateLimiterService.allowRequest()) {
            throw new RateLimitExceededException("Rate limit exceeded for mobile review crawling");
        }

        String normalizedTitle = normalizeTitle(year, mobileMake, mobileModel);
        List<String> reviewTexts = redditCrawlerService.crawlReviews(mobileMake, mobileModel, year);
        String combinedReviewText = String.join("\n\n", reviewTexts);

        synchronized(this) {
            MobileReview mobileReview = new MobileReview();
            mobileReview.setTitle(normalizedTitle);
            mobileReview.setOriginalText(combinedReviewText);
            mobileReview.setLastUpdated(LocalDateTime.now());

            String summarizedReview = summaryGenerationService.generateComprehensiveSummary(
                    combinedReviewText, mobileMake, mobileModel, year
            );
            mobileReview.setSummarizedText(summarizedReview);

            try {
                mobileReviewRepository.save(mobileReview);
            } catch (DuplicateKeyException e) {
                logger.error("Duplicate review title detected during save: {}", normalizedTitle);
                throw new DuplicateReviewException("A review with this title already exists");
            }

            return summarizedReview;
        }
    }

    private boolean isReviewRecent(MobileReview review) {
        if (review.getLastUpdated() == null) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        long hoursSinceUpdate = ChronoUnit.HOURS.between(review.getLastUpdated(), now);
        return hoursSinceUpdate < MINIMUM_UPDATE_INTERVAL_HOURS;
    }
}