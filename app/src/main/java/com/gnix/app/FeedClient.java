package com.gnix.app;

import java.io.*;
import java.net.*;
import java.nio.charset.*;
import java.util.*;
import java.util.regex.*;
import javax.net.ssl.HttpsURLConnection;

public final class FeedClient implements NewsRepository.Loader {
    private static final int LIMIT = 2 * 1024 * 1024;
    public List<Article> load(Source source) throws Exception {
        String endpoint = source.url;
        long deadline = System.nanoTime() + 60_000_000_000L;
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
            try {
                int status = connection.getResponseCode();
                if (status == 301 || status == 302 || status == 303 || status == 307 || status == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Redirecionamento inválido");
                    endpoint = new URI(safe).resolve(location).toString();
                    continue;
                }
                if (status != 200) throw new IOException("Fonte indisponível (" + status + ")");
                if (connection.getContentLengthLong() > LIMIT) throw new IOException("Feed muito grande");
                ByteArrayOutputStream data = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
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
                Matcher charsetMatch = Pattern.compile("(?i)charset\\s*=\\s*[\"']?([a-z0-9_-]+)").matcher(header == null ? "" : header);
                Matcher xmlMatch = Pattern.compile("(?i)encoding\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(probe);
                try {
                    if (charsetMatch.find()) charset = Charset.forName(charsetMatch.group(1));
                    else if (xmlMatch.find()) charset = Charset.forName(xmlMatch.group(1));
                } catch (Exception ignored) { }
                String xml = new String(bytes, charset);
                if (!xml.isEmpty() && xml.charAt(0) == '\ufeff') xml = xml.substring(1);
                return FeedParser.parse(xml, new Source(source.id, source.name, safe, source.category));
            } finally { connection.disconnect(); }
        }
        throw new IOException("Muitos redirecionamentos");
    }
}
