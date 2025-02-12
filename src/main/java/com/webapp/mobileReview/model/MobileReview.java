package com.webapp.mobileReview.model;


import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Date;

@Document(collection = "mobile")
public class MobileReview {
    @Id
    private String id;
    @Indexed(unique = true)
    private String title;
    private String url;
    private String originalText;
    private String summarizedText;
    private String subreddit;
    private Date createdAt;
    private LocalDateTime lastUpdated;
    private boolean contentChanged;


    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOriginalText() {
        return originalText;
    }

    public void setOriginalText(String originalText) {
        this.originalText = originalText;
    }

    public String getSummarizedText() {
        return summarizedText;
    }

    public void setSummarizedText(String summarizedText) {
        this.summarizedText = summarizedText;
    }

    public String getSubreddit() {
        return subreddit;
    }

    public void setSubreddit(String subreddit) {
        this.subreddit = subreddit;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastUpdated(){
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated){
        this.lastUpdated = lastUpdated;
    }

    public boolean isContentChanged() {
        return contentChanged;
    }

    public void setContentChanged(boolean contentChanged) {
        this.contentChanged = contentChanged;
    }

}