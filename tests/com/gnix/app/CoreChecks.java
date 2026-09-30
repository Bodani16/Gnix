package com.gnix.app;
import java.util.*;
public final class CoreChecks {
    static int checks;
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    public static void run() throws Exception {
        checks = 0;
        Source src = new Source("test", "Teste", "https://example.com/feed", "Geral");
        String rss = "<rss><channel><item><title>Notícia &amp; ciência</title><description><![CDATA[<p>Texto <b>seguro</b></p>]]></description><link>https://example.com/a</link><pubDate>Wed, 30 Sep 2026 10:00:00 GMT</pubDate></item></channel></rss>";
        List<Article> articles = FeedParser.parse(rss, src);
        check(articles.size() == 1, "RSS should yield one article");
        check(articles.get(0).title.equals("Notícia & ciência"), "Title entities should decode");
        check(articles.get(0).summary.equals("Texto seguro"), "HTML should become plain text");
        check(articles.get(0).sourceId.equals("test"), "Source attribution should persist");
        check(articles.get(0).publishedAt > 0, "RSS date should parse");
        String atom = "<feed xmlns='http://www.w3.org/2005/Atom'><entry><title>Mundo</title><summary>Resumo</summary><link rel='self' href='https://example.com/self'/><link rel='alternate' href='/b'/><updated>2026-09-30T12:00:00Z</updated></entry></feed>";
        Article a = FeedParser.parse(atom, src).get(0);
        check(a.url.equals("https://example.com/b"), "Atom alternate link should resolve");
        check(a.summary.equals("Resumo"), "Atom summary should be read");
        check(a.publishedAt > articles.get(0).publishedAt, "Atom timestamp should parse");
        check(FeedParser.parse("<rss><channel><item><title>Ruim</title><link>javascript:alert(1)</link></item></channel></rss>", src).isEmpty(), "Unsafe URL should be discarded");
        check(FeedParser.parse("<rss><channel><item><title>Ruim</title><link>https://user:pass@example.com/a</link></item></channel></rss>", src).isEmpty(), "Credential URL should be discarded");
        check(FeedParser.parse("<rss><channel><item><title>Sem link</title></item></channel></rss>", src).isEmpty(), "Missing RSS link should not resolve to feed directory");
        check(FeedParser.parse("<feed xmlns='http://www.w3.org/2005/Atom'><entry><title>Sem link</title><link rel='self' href='https://example.com/self'/></entry></feed>", src).isEmpty(), "Atom without alternate link should be discarded");
        Article noDate = FeedParser.parse("<rss><channel><item><title>Sem data</title><link>https://example.com/c</link><pubDate>invalid</pubDate></item></channel></rss>", src).get(0);
        check(noDate.publishedAt == 0, "Invalid date should use fallback");
        boolean malformed = false;
        try { FeedParser.parse("<rss><broken>", src); } catch (Exception e) { malformed = true; }
        check(malformed, "Malformed XML should fail");
        boolean entities = false;
        try { FeedParser.parse("<!DOCTYPE rss [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><rss><channel/></rss>", src); } catch (Exception e) { entities = true; }
        check(entities, "DOCTYPE should be rejected");
        boolean lowerEntity = false;
        try { FeedParser.parse("<!doctype rss [<!entity x 'y'>]><rss><channel/></rss>", src); } catch (Exception e) { lowerEntity = true; }
        check(lowerEntity, "Lowercase DOCTYPE should be rejected");
        String home = "<html><head><link href='/noticias.xml?x=1&amp;y=2' type='application/rss+xml' rel='alternate' title='Notícias'></head></html>";
        check(FeedParser.discoverFeedUrl(home, "https://example.com/").equals("https://example.com/noticias.xml?x=1&y=2"), "Site should reveal its RSS feed");
        check(FeedParser.discoverFeedUrl("<link rel=\"alternate\" type=\"application/atom+xml\" href=\"https://example.com/atom\">", "https://example.com/").equals("https://example.com/atom"), "Atom discovery should work");
        check(FeedParser.discoverFeedUrl("<link rel='alternate' type='application/rss+xml' href='http://example.com/rss'>", "https://example.com/").isEmpty(), "Discovery must require HTTPS");
        List<Article> combined = FeedParser.normalizeArticles(Arrays.asList(noDate, articles.get(0), a, articles.get(0)));
        check(combined.size() == 3, "Duplicate URL should appear once");
        check(combined.get(0).url.equals(a.url), "Newest should sort first");
        check(combined.get(2).publishedAt == 0, "Unknown date should sort last");
        Source good = new Source("good", "Boa", "https://example.com/good", "Geral");
        Source bad = new Source("bad", "Falha", "https://example.com/bad", "Geral");
        Article old = new Article("Anterior", "Cache", "https://example.com/old", "bad", 1);
        Article fresh = new Article("Nova", "Feed", "https://example.com/new", "good", 2);
        NewsRepository repo = new NewsRepository(source -> {
            if (source.id.equals("bad")) throw new java.io.IOException("offline");
            return NewsRepository.LoadResult.of(Collections.singletonList(fresh));
        });
        NewsRepository.RefreshResult partial = repo.refresh(Arrays.asList(good, bad), Collections.singletonList(old));
        check(partial.articles.size() == 2, "Partial failure should preserve cache from failed source");
        check(partial.failedSourceIds.equals(Collections.singletonList("bad")), "Failure should identify source");
        check(partial.articles.get(0).url.equals(fresh.url), "New article should lead edition");
        NewsRepository.RefreshResult offline = repo.refresh(Collections.singletonList(bad), Collections.singletonList(old));
        check(offline.articles.get(0).url.equals(old.url), "Offline should preserve prior edition");
        check(repo.refresh(Collections.emptyList(), Collections.singletonList(old)).articles.isEmpty(), "Empty selection should not restore default sources");
        NewsRepository emptyRepo = new NewsRepository(source -> NewsRepository.LoadResult.of(Collections.emptyList()));
        check(emptyRepo.refresh(Collections.singletonList(bad), Collections.singletonList(old)).articles.isEmpty(), "Successful empty feed should replace stale cache");
        NewsRepository notModifiedRepo = new NewsRepository(source -> new NewsRepository.LoadResult(Collections.emptyList(), true, "etag-1", "Mon, 01 Jan 2026 00:00:00 GMT"));
        Article cachedGood = new Article("Cacheada", "Antiga", "https://example.com/old-good", "good", 5);
        NewsRepository.RefreshResult notModified = notModifiedRepo.refresh(Collections.singletonList(good), Collections.singletonList(cachedGood));
        check(notModified.failedSourceIds.isEmpty(), "304 should not mark source as failed");
        check(notModified.articles.size() == 1 && notModified.articles.get(0).url.equals(cachedGood.url), "304 should keep cache articles");
        Source general = new Source("general", "Geral", "https://example.com/general", "Geral");
        Source tech = new Source("tech", "Tech", "https://example.com/tech", "Tecnologia");
        List<Source> catalog = Arrays.asList(general, tech);
        Set<String> reconciled = SelectionPolicy.reconcile(catalog, Collections.singleton("general"), Collections.singleton("Tecnologia"), false);
        check(reconciled.isEmpty(), "Changing topics must remove sources hidden from picker");
        check(SelectionPolicy.reconcile(catalog, Collections.singleton("general"), Collections.singleton("Tecnologia"), true).equals(Collections.singleton("tech")), "Initial setup may seed only a visible source");
        check(SelectionPolicy.reconcile(catalog, Collections.singleton("general"), Collections.emptySet(), false).isEmpty(), "No topics must not keep hidden sources");
        check(SelectionPolicy.reconcile(catalog, Collections.singleton("general"), Collections.singleton("Geral"), false).equals(Collections.singleton("general")), "Matching choice should persist");
        check(DisplayPolicy.topicColumns(320, 1f) == 1, "Narrow phone needs one topic column");
        check(DisplayPolicy.topicColumns(412, 1f) == 2, "Typical phone needs two columns");
        check(DisplayPolicy.topicColumns(412, 1.6f) == 1, "Large system font needs one column");
        check(DisplayPolicy.topicColumns(700, 1f) == 3, "Wide display supports three columns");
        check(DisplayPolicy.horizontalGutter(1000, 720) == 140, "Wide content should be centered");
        check(DisplayPolicy.horizontalGutter(412, 720) == 0, "Phone should use full safe width");
        check(!DisplayPolicy.needsRefresh(100000, 99000, false), "Recent cache should avoid duplicate launch request");
        check(DisplayPolicy.needsRefresh(1000000, 1, false), "Stale cache should refresh");
        check(DisplayPolicy.needsRefresh(100000, 99000, true), "Empty cache should refresh even when recent");
        check(DisplayPolicy.needsRefresh(100000, 0, false), "Unknown refresh date should refresh");
        check(DisplayPolicy.refreshTimestamp(100000, 0, 2) == 0, "Total failure must invalidate freshness");
        check(DisplayPolicy.refreshTimestamp(100000, 2, 2) == 100000, "Full success should record freshness");
        long now = 1_000_000_000_000L;
        long partialStamp = DisplayPolicy.refreshTimestamp(now, 1, 2);
        check(partialStamp == now - 480_000L, "Partial failure should backdate freshness");
        check(!DisplayPolicy.needsRefresh(now, partialStamp, false), "Partial failure should not force immediate refresh");
        check(DisplayPolicy.needsRefresh(now + 300_000, partialStamp, false), "Partial failure should refresh after TTL gap");
        check(DisplayPolicy.refreshTimestamp(100000, false) == 0, "Legacy boolean partial failure still invalidates freshness");
        check(DisplayPolicy.refreshTimestamp(100000, true) == 100000, "Legacy boolean full success still records freshness");
        System.out.println("PASS: " + checks + " core checks");
    }
    public static void main(String[] args) throws Exception { run(); }
}
