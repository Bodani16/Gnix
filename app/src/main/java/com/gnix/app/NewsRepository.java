package com.gnix.app;

import java.util.*;
import java.util.concurrent.*;

/** One failed publisher must not discard the rest of the user's edition. */
public final class NewsRepository {
    public interface Loader { List<Article> load(Source source) throws Exception; }
    public static final class RefreshResult {
        public final List<Article> articles;
        public final List<String> failedSourceIds;
        public RefreshResult(List<Article> articles, List<String> failed) { this.articles = articles; this.failedSourceIds = failed; }
    }
    private final Loader loader;
    public NewsRepository(Loader loader) { this.loader = loader; }
    public RefreshResult refresh(List<Source> sources, List<Article> cache) {
        if (sources.isEmpty()) return new RefreshResult(new ArrayList<>(), new ArrayList<>());
        List<Article> result = new ArrayList<>();
        List<String> failed = new ArrayList<>();
        Map<String, List<Article>> cacheBySource = new HashMap<>();
        for (Article article : cache)
            cacheBySource.computeIfAbsent(article.sourceId, ignored -> new ArrayList<>()).add(article);
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(4, sources.size())));
        Map<Source, Future<List<Article>>> futures = new LinkedHashMap<>();
        try {
            for (Source source : sources) futures.put(source, pool.submit(() -> loader.load(source)));
            for (Map.Entry<Source, Future<List<Article>>> entry : futures.entrySet()) {
                Source source = entry.getKey();
                try { result.addAll(entry.getValue().get(65, TimeUnit.SECONDS)); }
                catch (Exception e) {
                    entry.getValue().cancel(true);
                    failed.add(source.id);
                    result.addAll(cacheBySource.getOrDefault(source.id, Collections.emptyList()));
                }
            }
        } finally { pool.shutdownNow(); }
        result = FeedParser.normalizeArticles(result);
        return new RefreshResult(new ArrayList<>(result.subList(0, Math.min(result.size(), 300))), failed);
    }
}
