package com.gnix.app;

public final class Article {
    public final String title, summary, url, sourceId;
    public final long publishedAt;
    public Article(String title, String summary, String url, String sourceId, long publishedAt) {
        this.title = title; this.summary = summary; this.url = url;
        this.sourceId = sourceId; this.publishedAt = publishedAt;
    }
}
