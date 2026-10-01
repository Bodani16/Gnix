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
    private static final Pattern DOCTYPE = Pattern.compile("<!DOCTYPE", Pattern.CASE_INSENSITIVE);
    private static final Pattern ENTITY_DECL = Pattern.compile("<!ENTITY", Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_STYLE = Pattern.compile("(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>");
    private static final Pattern TAG = Pattern.compile("(?is)<[^>]*>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x[0-9a-fA-F]+|[0-9]+);");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s\\u00a0]+");
    private static final Pattern CHARSET_HEADER = Pattern.compile("(?i)charset\\s*=\\s*[\"']?([a-z0-9_-]+)");
    private static final Pattern XML_ENCODING = Pattern.compile("(?i)encoding\\s*=\\s*['\"]([^'\"]+)['\"]");
    private static final Pattern HTML_LINK = Pattern.compile("(?is)<link\\b[^>]*>");
    private static final Pattern HTML_ATTRIBUTE = Pattern.compile("([\\w:-]+)\\s*=\\s*(?:\"([^\"]*)\"|'([^']*)'|([^\\s>]+))", Pattern.CASE_INSENSITIVE);
    private static final String[] SUFFIX_SEPARATORS = {" - ", " – ", " — ", " | "};
    private static final DateTimeFormatter RFC1123 = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final DateTimeFormatter[] CUSTOM_DATES = {
        DateTimeFormatter.ofPattern("EEE, d MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        DateTimeFormatter.ofPattern("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH)
    };

    public static List<Article> parse(String xml, Source source) throws Exception {
        if (xml.length() > MAX_XML) throw new IllegalArgumentException("Feed muito grande");
        if (DOCTYPE.matcher(xml).find() || ENTITY_DECL.matcher(xml).find())
            throw new IllegalArgumentException("Declaração externa não permitida");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setExpandEntityReferences(false);
        try { factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true); } catch (Exception ignored) { }
        try { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); } catch (Exception ignored) { }
        try { factory.setFeature("http://xml.org/sax/features/external-general-entities", false); } catch (Exception ignored) { }
        try { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false); } catch (Exception ignored) { }
        try { factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); } catch (Exception ignored) { }
        try { factory.setXIncludeAware(false); } catch (Exception ignored) { }
        try { factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalDTD", ""); } catch (Exception ignored) { }
        try { factory.setAttribute("http://javax.xml.XMLConstants/property/accessExternalSchema", ""); } catch (Exception ignored) { }
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
            String publisher = field(item, "source");
            title = stripPublisherSuffix(title, publisher.isEmpty() ? source.name : plainText(publisher));
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
    /**
     * Agregadores como o Google Notícias repetem o veículo no fim do título
     * ("Manchete - Gazeta do Povo"). O nome já aparece ao lado da manchete na
     * lista, então o sufixo só rouba espaço da própria manchete.
     */
    static String stripPublisherSuffix(String title, String publisher) {
        if (publisher.isEmpty()) return title;
        for (String separator : SUFFIX_SEPARATORS) {
            String marker = separator + publisher;
            if (title.length() > marker.length() && title.regionMatches(true, title.length() - marker.length(), marker, 0, marker.length())) {
                String trimmed = title.substring(0, title.length() - marker.length()).trim();
                if (!trimmed.isEmpty()) return trimmed;
            }
        }
        return title;
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
    public static String discoverFeedUrl(String html, String pageUrl) {
        Matcher links = HTML_LINK.matcher(html);
        while (links.find()) {
            Map<String, String> attributes = new HashMap<>();
            Matcher values = HTML_ATTRIBUTE.matcher(links.group());
            while (values.find()) {
                String value = first(values.group(2) == null ? "" : values.group(2),
                    values.group(3) == null ? "" : values.group(3), values.group(4) == null ? "" : values.group(4));
                attributes.put(values.group(1).toLowerCase(Locale.ROOT), value);
            }
            String rel = attributes.getOrDefault("rel", "").toLowerCase(Locale.ROOT);
            String type = attributes.getOrDefault("type", "").toLowerCase(Locale.ROOT);
            if (!Arrays.asList(rel.split("\\s+")).contains("alternate") ||
                !(type.startsWith("application/rss+xml") || type.startsWith("application/atom+xml"))) continue;
            String url = safeUrl(attributes.getOrDefault("href", "").replace("&amp;", "&"), pageUrl);
            if (url.startsWith("https://")) return url;
        }
        return "";
    }
    public static String plainText(String html) {
        String text = SCRIPT_STYLE.matcher(html).replaceAll(" ");
        text = TAG.matcher(text).replaceAll(" ");
        text = text.replace("&nbsp;", " ").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
        Matcher matcher = NUMERIC_ENTITY.matcher(text);
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
        return WHITESPACE.matcher(decoded.toString()).replaceAll(" ").trim();
    }
    private static long parseDate(String raw) {
        if (raw == null || raw.isEmpty()) return 0;
        try { return Instant.parse(raw).toEpochMilli(); } catch (Exception ignored) { }
        try { return OffsetDateTime.parse(raw).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        try { return ZonedDateTime.parse(raw, RFC1123).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        for (DateTimeFormatter format : CUSTOM_DATES) {
            try { return ZonedDateTime.parse(raw, format).toInstant().toEpochMilli(); } catch (Exception ignored) { }
        }
        return 0;
    }
    public static Matcher charsetMatcher(String contentType) { return CHARSET_HEADER.matcher(contentType == null ? "" : contentType); }
    public static Matcher xmlEncodingMatcher(String probe) { return XML_ENCODING.matcher(probe); }
    public static List<Article> normalizeArticles(List<Article> articles) {
        List<Article> sorted = new ArrayList<>(articles);
        sorted.sort((a, b) -> Long.compare(b.publishedAt, a.publishedAt));
        Map<String, Article> unique = new LinkedHashMap<>();
        for (Article a : sorted) if (!unique.containsKey(a.url)) unique.put(a.url, a);
        return new ArrayList<>(unique.values());
    }
}
