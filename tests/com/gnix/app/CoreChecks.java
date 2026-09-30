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
            return Collections.singletonList(fresh);
        });
        NewsRepository.RefreshResult partial = repo.refresh(Arrays.asList(good, bad), Collections.singletonList(old));
        check(partial.articles.size() == 2, "Partial failure should preserve cache from failed source");
        check(partial.failedSourceIds.equals(Collections.singletonList("bad")), "Failure should identify source");
        check(partial.articles.get(0).url.equals(fresh.url), "New article should lead edition");
        NewsRepository.RefreshResult offline = repo.refresh(Collections.singletonList(bad), Collections.singletonList(old));
        check(offline.articles.get(0).url.equals(old.url), "Offline should preserve prior edition");
        check(repo.refresh(Collections.emptyList(), Collections.singletonList(old)).articles.isEmpty(), "Empty selection should not restore default sources");
        NewsRepository emptyRepo = new NewsRepository(source -> Collections.emptyList());
        check(emptyRepo.refresh(Collections.singletonList(bad), Collections.singletonList(old)).articles.isEmpty(), "Successful empty feed should replace stale cache");
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
        check(DisplayPolicy.refreshTimestamp(100000, false) == 0, "Partial failure must invalidate freshness");
        check(DisplayPolicy.refreshTimestamp(100000, true) == 100000, "Full success should record freshness");
        System.out.println("PASS: " + checks + " core checks");
    }
    public static void main(String[] args) throws Exception { run(); }
}
