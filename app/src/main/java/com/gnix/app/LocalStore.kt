package com.gnix.app;

import android.content.Context;
import android.util.AtomicFile;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;

class LocalStore(context: Context) : FeedClient.MetaStore {
    private val root = context.filesDir
    private fun read(name: String): JSONArray = try {
        JSONArray(String(AtomicFile(File(root, name)).readFully(), Charsets.UTF_8))
    } catch (_: Exception) { JSONArray() }

    @Synchronized private fun write(name: String, value: JSONArray) {
        val file = AtomicFile(File(root, name))
        val stream = file.startWrite()
        try {
            stream.write(value.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (error: Exception) {
            file.failWrite(stream)
            throw error
        }
    }
    fun loadSelection(): Set<String> = read("selection.json").let { array -> (0 until array.length()).map { array.optString(it) }.toSet() }
    fun saveSelection(ids: Set<String>) = write("selection.json", JSONArray(ids.toList()))
    fun loadTopics(): Set<String> = read("topics.json").let { array -> (0 until array.length()).map { array.optString(it) }.toSet() }
    fun saveTopics(topics: Set<String>) = write("topics.json", JSONArray(topics.toList()))
    fun isConfigured(): Boolean = File(root, "selection.json").exists()
    fun loadCustomSources(): List<Source> = read("sources.json").let { array ->
        (0 until array.length()).mapNotNull { i ->
            val value = array.optJSONObject(i) ?: return@mapNotNull null
            val url = value.optString("url")
            if (!url.startsWith("https://") || FeedParser.safeUrl(url, url).isEmpty()) return@mapNotNull null
            Source(value.optString("id"), value.optString("name"), url, value.optString("category", "Geral"))
        }
    }
    fun saveCustomSources(sources: List<Source>) = write("sources.json", JSONArray().apply {
        sources.forEach { put(JSONObject().put("id", it.id).put("name", it.name).put("url", it.url).put("category", it.category)) }
    })
    private fun readArticles(name: String): List<Article> = read(name).let { array ->
        (0 until array.length()).mapNotNull { i ->
            val value = array.optJSONObject(i) ?: return@mapNotNull null
            val url = value.optString("url")
            if (url.isEmpty()) return@mapNotNull null
            Article(value.optString("title"), value.optString("summary"), url, value.optString("sourceId"), value.optLong("publishedAt"))
        }
    }
    private fun writeArticles(name: String, articles: List<Article>) = write(name, JSONArray().apply {
        articles.forEach { put(JSONObject().put("title", it.title).put("summary", it.summary).put("url", it.url).put("sourceId", it.sourceId).put("publishedAt", it.publishedAt)) }
    })
    fun loadCache(): List<Article> = readArticles("cache.json")
    fun saveCache(articles: List<Article>) = writeArticles("cache.json", articles)
    fun lastRefresh(): Long = read("refresh.json").optLong(0, 0)
    fun saveRefreshTime(timestamp: Long) = write("refresh.json", JSONArray().put(timestamp))
    fun loadSaved(): List<Article> = readArticles("saved.json")
    fun saveBookmarks(articles: List<Article>) = writeArticles("saved.json", articles)

    override fun etag(sourceId: String): String? = read("feed-meta.json").let { array ->
        for (i in 0 until array.length()) {
            val value = array.optJSONObject(i) ?: continue
            if (value.optString("id") == sourceId) return value.optString("etag").ifEmpty { null }
        }
        null
    }
    override fun lastModified(sourceId: String): String? = read("feed-meta.json").let { array ->
        for (i in 0 until array.length()) {
            val value = array.optJSONObject(i) ?: continue
            if (value.optString("id") == sourceId) return value.optString("lastModified").ifEmpty { null }
        }
        null
    }
    @Synchronized override fun save(sourceId: String, etag: String?, lastModified: String?) {
        val array = read("feed-meta.json")
        val next = JSONArray()
        var replaced = false
        for (i in 0 until array.length()) {
            val value = array.optJSONObject(i) ?: continue
            if (value.optString("id") != sourceId) next.put(value)
            else {
                replaced = true
                if (etag != null || lastModified != null) {
                    next.put(JSONObject().put("id", sourceId).put("etag", etag ?: "").put("lastModified", lastModified ?: ""))
                }
            }
        }
        if (!replaced && (etag != null || lastModified != null)) {
            next.put(JSONObject().put("id", sourceId).put("etag", etag ?: "").put("lastModified", lastModified ?: ""))
        }
        write("feed-meta.json", next)
    }
}
