package com.gnix.app;

import java.io.StringReader;
import java.net.URI;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

/** Parses publisher-provided RSS/Atom; no scripts, AI or remote XML entities. */
public final class FeedParser {
    private static final int MAX_XML = 2 * 1024 * 1024;
    public static List<Article> parse(String xml, Source source) throws Exception {
        if (xml.length() > MAX_XML) throw new IllegalArgumentException("Feed muito grande");
        String upper = xml.toUpperCase(Locale.ROOT);
        if (upper.contains("<!DOCTYPE") || upper.contains("<!ENTITY"))
            throw new IllegalArgumentException("Declaração externa não permitida");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        try { factory.setFeature("http://xml.org/sax/features/external-general-entities", false); } catch (Exception ignored) { }
        try { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); } catch (Exception ignored) { }
        var builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> { throw new SAXException("Entidade externa bloqueada"); });
        builder.setErrorHandler(new DefaultHandler() {
            @Override public void error(SAXParseException e) throws SAXException { throw e; }
            @Override public void fatalError(SAXParseException e) throws SAXException { throw e; }
        });
        Document document = builder.parse(new InputSource(new StringReader(xml)));
        String root = document.getDocumentElement().getLocalName();
        if (root == null) root = document.getDocumentElement().getTagName();
        boolean atom = root.equalsIgnoreCase("feed");
        if (!atom && !root.equalsIgnoreCase("rss") && !root.equalsIgnoreCase("RDF"))
            throw new IllegalArgumentException("Endereço não contém RSS ou Atom");
        NodeList nodes = document.getElementsByTagNameNS("*", atom ? "entry" : "item");
        List<Article> result = new ArrayList<>();
        for (int i = 0; i < Math.min(nodes.getLength(), 200); i++) {
            Element item = (Element) nodes.item(i);
            String title = plainText(field(item, "title"));
            String rawLink = field(item, "link");
            if (atom) {
                rawLink = "";
                for (Node node = item.getFirstChild(); node != null; node = node.getNextSibling()) {
                    if (node instanceof Element && local(node).equals("link")) {
                        Element link = (Element) node;
                        String rel = link.getAttribute("rel");
                        if (rel.isEmpty() || rel.equals("alternate")) { rawLink = link.getAttribute("href"); break; }
                    }
                }
            }
            String url = safeUrl(rawLink, source.url);
            if (url.isEmpty() || title.isEmpty()) continue;
            String summary = first(field(item, "description"), field(item, "summary"), field(item, "encoded"), field(item, "content"));
            String date = first(field(item, "pubDate"), field(item, "published"), field(item, "updated"), field(item, "date"));
            result.add(new Article(truncate(title, 500), truncate(plainText(summary), 3000), url, source.id, parseDate(date)));
        }
        return normalizeArticles(result);
    }
    private static String local(Node node) { return node.getLocalName() == null ? node.getNodeName() : node.getLocalName(); }
    private static String field(Element item, String name) {
        for (Node node = item.getFirstChild(); node != null; node = node.getNextSibling())
            if (node instanceof Element && local(node).equalsIgnoreCase(name)) return node.getTextContent().trim();
        return "";
    }
    private static String first(String... values) { for (String s : values) if (!s.isEmpty()) return s; return ""; }
    private static String truncate(String s, int limit) { return s.length() <= limit ? s : s.substring(0, limit); }
    public static String safeUrl(String raw, String base) {
        if (raw == null || raw.trim().isEmpty() || raw.length() > 4096) return "";
        try {
            URI uri = new URI(base).resolve(raw.trim());
            String scheme = uri.getScheme();
            if (("https".equalsIgnoreCase(scheme) || "http".equalsIgnoreCase(scheme)) && uri.getHost() != null && uri.getUserInfo() == null)
                return uri.toString();
        } catch (Exception ignored) { }
        return "";
    }
    public static String plainText(String html) {
        String text = html.replaceAll("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>", " ")
            .replaceAll("(?is)<[^>]*>", " ");
        String[][] entities = {{"&nbsp;", " "}, {"&lt;", "<"}, {"&gt;", ">"}, {"&quot;", "\""}, {"&apos;", "'"}, {"&amp;", "&"}};
        for (String[] pair : entities) text = text.replace(pair[0], pair[1]);
        Matcher matcher = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);").matcher(text);
        StringBuffer decoded = new StringBuffer();
        while (matcher.find()) {
            String replacement = " ";
            try {
                String value = matcher.group(1);
                int cp = value.startsWith("x") ? Integer.parseInt(value.substring(1), 16) : Integer.parseInt(value);
                if (Character.isValidCodePoint(cp) && !(cp >= 0xD800 && cp <= 0xDFFF)) replacement = new String(Character.toChars(cp));
            } catch (Exception ignored) { }
            matcher.appendReplacement(decoded, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(decoded);
        return decoded.toString().replaceAll("[\\s\\u00a0]+", " ").trim();
    }
    private static long parseDate(String raw) {
        try { return Instant.parse(raw).toEpochMilli(); } catch (Exception ignored) { }
        try { return OffsetDateTime.parse(raw).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        try { return ZonedDateTime.parse(raw, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        for (String format : new String[]{"EEE, d MMM yyyy HH:mm:ss Z", "d MMM yyyy HH:mm:ss Z"}) {
            try { return ZonedDateTime.parse(raw, DateTimeFormatter.ofPattern(format, Locale.ENGLISH)).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        }
        return 0;
    }
    public static List<Article> normalizeArticles(List<Article> articles) {
        List<Article> sorted = new ArrayList<>(articles);
        sorted.sort((a, b) -> Long.compare(b.publishedAt, a.publishedAt));
        Map<String, Article> unique = new LinkedHashMap<>();
        for (Article a : sorted) if (!unique.containsKey(a.url)) unique.put(a.url, a);
        return new ArrayList<>(unique.values());
    }
}
