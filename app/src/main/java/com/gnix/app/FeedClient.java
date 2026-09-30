package com.gnix.app;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.GZIPInputStream;
import javax.net.ssl.HttpsURLConnection;

public final class FeedClient implements NewsRepository.Loader {
    private static final int LIMIT = 2 * 1024 * 1024;

    public interface MetaStore {
        String etag(String sourceId);
        String lastModified(String sourceId);
        void save(String sourceId, String etag, String lastModified);
    }

    private final MetaStore metas;
    public FeedClient() { this(null); }
    public FeedClient(MetaStore metas) { this.metas = metas; }

    private static final class Fetched {
        final String url;
        final NewsRepository.LoadResult result;
        Fetched(String url, NewsRepository.LoadResult result) { this.url = url; this.result = result; }
    }

    public String verifiedFeedUrl(Source source) throws Exception { return fetch(source).url; }

    @Override
    public NewsRepository.LoadResult load(Source source) throws Exception { return fetch(source).result; }

    private Fetched fetch(Source source) throws Exception {
        String endpoint = source.url;
        long deadline = System.nanoTime() + 60_000_000_000L;
        String previousEtag = metas == null ? null : metas.etag(source.id);
        String previousModified = metas == null ? null : metas.lastModified(source.id);
        for (int redirects = 0; redirects < 5; redirects++) {
            String safe = FeedParser.safeUrl(endpoint, endpoint);
            if (safe.isEmpty() || !new URI(safe).getScheme().equalsIgnoreCase("https"))
                throw new IOException("A fonte precisa usar HTTPS");
            HttpsURLConnection connection = (HttpsURLConnection) new URI(safe).toURL().openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "Gnix/1.0 Android RSS reader");
            connection.setRequestProperty("Accept", "application/rss+xml, application/atom+xml, application/xml, text/xml;q=0.9");
            connection.setRequestProperty("Accept-Encoding", "gzip");
            if (previousEtag != null && !previousEtag.isEmpty()) connection.setRequestProperty("If-None-Match", previousEtag);
            if (previousModified != null && !previousModified.isEmpty()) connection.setRequestProperty("If-Modified-Since", previousModified);
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirecionamento inválido");
                    endpoint = new URI(safe).resolve(location).toString();
                    continue;
                }
                if (status == 304) {
                    return new Fetched(safe, new NewsRepository.LoadResult(Collections.emptyList(), true, previousEtag, previousModified));
                }
                if (status != 200) throw new IOException("Fonte indisponível (" + status + ")");
                String encoding = connection.getContentEncoding();
                boolean gzip = encoding != null && encoding.toLowerCase(Locale.ROOT).contains("gzip");
                long declared = connection.getContentLengthLong();
                if (!gzip && declared > LIMIT) throw new IOException("Feed muito grande");
                ByteArrayOutputStream data = gzip || declared < 0 ? new ByteArrayOutputStream() : new ByteArrayOutputStream((int) Math.min(declared, LIMIT));
                try (InputStream raw = connection.getInputStream()) {
                    InputStream input = gzip ? new GZIPInputStream(raw) : raw;
                    byte[] buffer = new byte[8192];
                    int count;
                    while ((count = input.read(buffer)) != -1) {
                        if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline) throw new IOException("Tempo de atualização esgotado");
                        if (data.size() + count > LIMIT) throw new IOException("Feed muito grande");
                        data.write(buffer, 0, count);
                    }
                }
                byte[] bytes = data.toByteArray();
                Charset charset = StandardCharsets.UTF_8;
                String header = connection.getContentType();
                String probe = new String(bytes, 0, Math.min(bytes.length, 300), StandardCharsets.ISO_8859_1);
                Matcher charsetMatch = FeedParser.charsetMatcher(header);
                Matcher xmlMatch = FeedParser.xmlEncodingMatcher(probe);
                try {
                    if (charsetMatch.find()) charset = Charset.forName(charsetMatch.group(1));
                    else if (xmlMatch.find()) charset = Charset.forName(xmlMatch.group(1));
                } catch (Exception ignored) { }
                String xml = new String(bytes, charset);
                if (!xml.isEmpty() && xml.charAt(0) == '\ufeff') xml = xml.substring(1);
                String trimmed = xml.trim().toLowerCase(Locale.ROOT);
                if ((header != null && header.toLowerCase(Locale.ROOT).contains("text/html")) ||
                    trimmed.startsWith("<!doctype html") || trimmed.startsWith("<html")) {
                    String discovered = FeedParser.discoverFeedUrl(xml, safe);
                    if (discovered.isEmpty() || discovered.equals(safe))
                        throw new IOException("Este site não informa um feed RSS/Atom. Cole o endereço do feed.");
                    endpoint = discovered;
                    continue;
                }
                List<Article> parsed = FeedParser.parse(xml, new Source(source.id, source.name, safe, source.category));
                String etag = connection.getHeaderField("ETag");
                String lastModified = connection.getHeaderField("Last-Modified");
                if (metas != null) metas.save(source.id, etag, lastModified);
                return new Fetched(safe, new NewsRepository.LoadResult(parsed, false, etag, lastModified));
            } finally { connection.disconnect(); }
        }
        throw new IOException("Muitos redirecionamentos");
    }
}
