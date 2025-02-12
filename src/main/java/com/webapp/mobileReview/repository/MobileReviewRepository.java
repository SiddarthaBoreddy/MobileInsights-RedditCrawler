package com.webapp.mobileReview.repository;

import com.webapp.mobileReview.model.MobileReview;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MobileReviewRepository extends MongoRepository<MobileReview, String> {
    // Additional custom query methods can be added here if needed
    Optional<MobileReview> findByTitle(String title);
    Optional<MobileReview> findByTitleIgnoreCase(String title);
}
