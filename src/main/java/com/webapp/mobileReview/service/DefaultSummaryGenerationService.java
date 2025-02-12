package com.webapp.mobileReview.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DefaultSummaryGenerationService implements SummaryGenerationService {
    private final AnthropicChatModel chatModel;
    private final RateLimiterService rateLimiterService;
    private static final Logger logger = LoggerFactory.getLogger(DefaultSummaryGenerationService.class);

    @Autowired
    public DefaultSummaryGenerationService(
            AnthropicChatModel chatModel,
            RateLimiterService rateLimiterService
    ) {
        this.chatModel = chatModel;
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    public String generateComprehensiveSummary(
            String combinedReviewText,
            String mobileMake,
            String mobileModel,
            String year
    ) {
        if (combinedReviewText.isEmpty()) {
            return "No reviews found for the specified vehicle.";
        }

        // Truncate input text to ensure we don't exceed input token limits
        String truncatedReviewText = truncateText(combinedReviewText, 15000);

        // Construct summarization prompt (similar to original implementation)
        String prompt = String.format("""
        Provide a comprehensive analysis of %s %s %s reviews from multiple Reddit sources. 
        Analyze and synthesize the following key aspects:

        1. Overall Performance
        - Speed and responsiveness
        - Processor and RAM capabilities
        - Gaming and multitasking performance

        2. Display and Design
        - Screen quality (brightness, resolution, and refresh rate)
        - Build quality and durability
        - Ergonomics and aesthetics

        3. Camera and Photography
        - Photo and video quality (daylight and low-light performance)
        - Camera features (zoom, stabilization, and modes)
        - Comparison with competitors' camera performance

        4. Battery Life and Charging
        - Battery longevity in typical use
        - Charging speed and compatibility with fast charging
        - Power efficiency during intensive tasks

        5. Software and Features
        - User interface and software experience
        - Frequency and quality of software updates
        - Unique features or standout functionalities

        6. Connectivity and Network
        - 5G and LTE performance
        - Wi-Fi and Bluetooth connectivity
        - Dual SIM or eSIM functionality

        7. Value Proposition
        - Price vs. features
        - Comparison with similar models from competitors
        - Longevity and resale value

        8. Pros and Cons Summary
        - Top 3 strengths
        - Top 3 potential drawbacks

        9. Overall Sentiment
        - Aggregate user satisfaction
        - Recommendation rating

        Review Texts:
        %s
        """, year, mobileMake, mobileModel, truncatedReviewText);

        // Ensure we're within rate limits
        if (!rateLimiterService.allowRequest()) {
            return "Rate limit exceeded. Unable to generate summary at this time.";
        }

        try {
            return chatModel.call(prompt);
        } catch (Exception e) {
            logger.error("Error generating summary", e);
            return "Unable to generate comprehensive summary due to processing error.";
        }
    }

    private String truncateText(String text, int maxTokens) {
        // Same implementation as in the original service
        if (text.length() <= maxTokens * 4) {
            return text;
        }

        String truncated = text.substring(0, maxTokens * 4);
        int lastSentenceEnd = truncated.lastIndexOf('.');

        return lastSentenceEnd > 0
                ? truncated.substring(0, lastSentenceEnd + 1)
                : truncated;
    }
}
