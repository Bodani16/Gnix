package com.gnix.app;

import java.util.*;
import java.util.concurrent.*;

/** One failed publisher must not discard the rest of the user's edition. */
public final class NewsRepository {
    public interface Loader { LoadResult load(Source source) throws Exception; }

    public static final class LoadResult {
        public final List<Article> articles;
        public final boolean notModified;
        public final String etag;
        public final String lastModified;
        public LoadResult(List<Article> articles, boolean notModified, String etag, String lastModified) {
            this.articles = articles == null ? Collections.emptyList() : articles;
            this.notModified = notModified;
            this.etag = etag;
            this.lastModified = lastModified;
        }
        public static LoadResult of(List<Article> articles) { return new LoadResult(articles, false, null, null); }
    }

    public static final class RefreshResult {
        public final List<Article> articles;
        public final List<String> failedSourceIds;
        public RefreshResult(List<Article> articles, List<String> failed) { this.articles = articles; this.failedSourceIds = failed; }
    }

    private static final ExecutorService POOL = Executors.newFixedThreadPool(4, runnable -> {
        Thread thread = new Thread(runnable, "gnix-feed");
        thread.setDaemon(true);
        return thread;
    });

    private final Loader loader;
    public NewsRepository(Loader loader) { this.loader = loader; }

    public RefreshResult refresh(List<Source> sources, List<Article> cache) {
        List<Article> result = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        Map<Source, Future<LoadResult>> futures = new LinkedHashMap<>();
        try {
            for (Source source : sources) futures.put(source, POOL.submit(() -> loader.load(source)));
            for (Map.Entry<Source, Future<LoadResult>> entry : futures.entrySet()) {
                Source source = entry.getKey();
                try {
                    LoadResult loaded = entry.getValue().get(65, TimeUnit.SECONDS);
                    if (loaded.notModified) {
                        for (Article a : cache) if (a.sourceId.equals(source.id)) result.add(a);
                    } else {
                        result.addAll(loaded.articles);
                    }
                } catch (Exception e) {
                    entry.getValue().cancel(true);
                    failed.add(source.id);
                    for (Article a : cache) if (a.sourceId.equals(source.id)) result.add(a);
                }
            }
        } finally {
            for (Future<LoadResult> future : futures.values()) {
                if (!future.isDone()) future.cancel(true);
            }
        }
        result = FeedParser.normalizeArticles(result);
        return new RefreshResult(new ArrayList<>(result.subList(0, Math.min(result.size(), 300))), failed);
    }
}
