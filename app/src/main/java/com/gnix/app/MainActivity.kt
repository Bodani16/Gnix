package com.gnix.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.os.Build
import android.view.WindowManager
import android.widget.*
import android.text.InputType
import android.text.TextUtils
import android.text.TextWatcher
import android.text.Editable
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors

class MainActivity : Activity() {
    private enum class Page { WELCOME, TOPICS, SOURCES, FEED, SAVED }
    private lateinit var ui: GnixViews
    private lateinit var store: LocalStore
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var footer: LinearLayout
    private var feedList: ListView? = null
    private var feedAdapter: ArticleAdapter? = null
    private val worker = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var searchRunnable: Runnable? = null
    private val editionDateFormat = SimpleDateFormat("EEEE, d 'de' MMMM", Locale("pt", "BR"))
    private var sources = SourceCatalog.initial.toMutableList()
    private var custom = mutableListOf<Source>()
    private var selected = mutableSetOf<String>()
    private var topics = mutableSetOf<String>()
    private var articles = listOf<Article>()
    private var saved = listOf<Article>()
    private var sourceById: Map<String, Source> = emptyMap()
    private var savedUrls: Set<String> = emptySet()
    private var page = Page.WELCOME
    private var configured = false
    private var loading = false
    private var category = "Tudo"
    private var query = ""
    private var searching = false
    private var notice = ""
    private var selectionRevision = 0
    private var sourceCount: TextView? = null
    private var sourceContinue: TextView? = null
    private var destroyed = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        ui = GnixViews(this); store = LocalStore(this)
        custom = store.loadCustomSources().toMutableList(); sources.addAll(custom)
        selected = store.loadSelection().toMutableSet(); topics = store.loadTopics().toMutableSet()
        articles = store.loadCache(); saved = store.loadSaved(); configured = store.isConfigured()
        rebuildIndexes()
        if (configured) page = Page.FEED
        state?.getString("page")?.let { runCatching { page = Page.valueOf(it) } }
        state?.getStringArrayList("draftSelection")?.let { selected = it.toMutableSet() }
        state?.getStringArrayList("draftTopics")?.let { topics = it.toMutableSet() }
        category = state?.getString("category") ?: "Tudo"
        query = state?.getString("query") ?: ""
        searching = state?.getBoolean("searching") ?: false
        if (Build.VERSION.SDK_INT >= 30) {
            window.setDecorFitsSystemWindows(false)
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)
        }
        if (Build.VERSION.SDK_INT >= 33) {
            onBackInvokedDispatcher.registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT) { handleBack() }
        }
        render()
        if (configured && page == Page.FEED && selected.isNotEmpty() &&
            DisplayPolicy.needsRefresh(System.currentTimeMillis(), store.lastRefresh(), articles.isEmpty())) refresh()
    }
    override fun onSaveInstanceState(out: Bundle) {
        out.putString("page", page.name)
        out.putString("category", category)
        out.putString("query", query)
        out.putBoolean("searching", searching)
        out.putStringArrayList("draftSelection", ArrayList(selected))
        out.putStringArrayList("draftTopics", ArrayList(topics))
        super.onSaveInstanceState(out)
    }
    override fun onDestroy() {
        destroyed = true
        searchRunnable?.let { mainHandler.removeCallbacks(it) }
        mainHandler.removeCallbacksAndMessages(null)
        worker.shutdownNow()
        super.onDestroy()
    }
    // Lint pede a migração para o OnBackPressedDispatcher do AndroidX, que este
    // projeto não usa por restrição de escopo. A API 33+ já é atendida pelo
    // OnBackInvokedDispatcher nativo registrado em onCreate; este override é o
    // único caminho de "voltar" nas APIs 27 a 32, onde predictive back não existe.
    // O próprio check avisa que não considera opt-in por Activity.
    @Suppress("GestureBackNavigation")
    @Deprecated("Compatible back handling for Android 8.1+")
    override fun onBackPressed() { handleBack() }
    private fun rebuildIndexes() {
        sourceById = sources.associateBy { it.id }
        savedUrls = saved.mapTo(HashSet(saved.size)) { it.url }
    }
    private fun handleBack() {
        when (page) {
            Page.TOPICS -> {
                if (configured) {
                    topics = store.loadTopics().toMutableSet()
                    selected = SelectionPolicy.reconcile(sources, selected, topics, false).toMutableSet()
                    page = Page.SOURCES
                } else page = Page.WELCOME
                render()
            }
            Page.SOURCES -> {
                if (configured) { selected = store.loadSelection().toMutableSet(); topics = store.loadTopics().toMutableSet(); page = Page.FEED }
                else page = Page.TOPICS
                render()
            }
            Page.SAVED -> { page = Page.FEED; render() }
            else -> { if (searching) { searching = false; query = ""; render() } else finishAfterTransition() }
        }
    }
    private fun toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    private fun persist(action: () -> Unit): Boolean = try { action(); true } catch (_: Exception) { toast("Não foi possível salvar. Tente novamente."); false }
    private fun iconFor(category: String) = when (category) { "Mundo" -> "world"; "Política" -> "politics"; "Economia" -> "economy"; "Tecnologia" -> "tech"; "Jogos" -> "games"; "Esportes" -> "sports"; "Saúde" -> "health"; "Vídeos" -> "video"; "Cultura" -> "culture"; else -> "news" }
    private fun title(text: String, subtitle: String) {
        ui.add(content, ui.text(text, 29f, bold = true), 10)
        ui.add(content, ui.text(subtitle, 15f, GnixViews.muted), 26)
    }
    private fun render() {
        root = ui.column().apply { setBackgroundColor(GnixViews.background) }
        setContentView(root)
        val gutter = DisplayPolicy.horizontalGutter(ui.dp(resources.configuration.screenWidthDp), ui.dp(720))
        root.setPadding(gutter, 0, gutter, 0)
        if (Build.VERSION.SDK_INT >= 30) root.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            val keyboard = insets.getInsets(WindowInsets.Type.ime())
            val safeWidth = windowManager.currentWindowMetrics.bounds.width() - bars.left - bars.right
            val centered = DisplayPolicy.horizontalGutter(safeWidth, ui.dp(720))
            view.setPadding(bars.left + centered, bars.top, bars.right + centered, maxOf(bars.bottom, keyboard.bottom))
            insets
        }
        content = ui.column().apply { setPadding(ui.dp(22), ui.dp(10), ui.dp(22), ui.dp(18)) }
        feedList = null; feedAdapter = null
        if (page == Page.FEED || page == Page.SAVED) {
            feedList = ListView(this).apply {
                id = R.id.feed_list
                divider = null; dividerHeight = 0; cacheColorHint = android.graphics.Color.TRANSPARENT
                isVerticalScrollBarEnabled = false
                addHeaderView(content, null, false)
            }
            root.addView(feedList, LinearLayout.LayoutParams(-1, 0, 1f))
        } else {
            val scroll = ScrollView(this).apply { id = R.id.onboarding_scroll; isFillViewport = true; isVerticalScrollBarEnabled = false; addView(content) }
            root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        }
        footer = ui.column().apply { setPadding(ui.dp(22), ui.dp(12), ui.dp(22), ui.dp(18)) }
        root.addView(footer, LinearLayout.LayoutParams(-1, -2))
        when (page) {
            Page.WELCOME -> welcome()
            Page.TOPICS -> topicPicker()
            Page.SOURCES -> sourcePicker()
            Page.FEED, Page.SAVED -> edition()
        }
        root.requestApplyInsets()
    }
    private fun brand(parent: LinearLayout, compact: Boolean = false) {
        val row = ui.row()
        val word = ui.text("gnix", if (compact) 34f else 48f, bold = true).apply {
            typeface = Typeface.create("sans-serif-black", Typeface.BOLD); letterSpacing = -.06f
            contentDescription = "Gnix"
        }
        row.addView(word)
        row.addView(View(this).apply { background = ui.shape(GnixViews.red, 8) }, LinearLayout.LayoutParams(ui.dp(9), ui.dp(9)).apply { leftMargin = ui.dp(7); gravity = Gravity.BOTTOM; bottomMargin = ui.dp(if (compact) 8 else 11) })
        ui.add(parent, row, 0)
    }
    private fun stepHeader(step: Int) {
        val row = ui.row()
        row.addView(ui.iconButton("back", "Voltar") { handleBack() }, LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        row.addView(ui.text("PERSONALIZE SEU GNIX", 11f, GnixViews.muted, true).apply { gravity = Gravity.CENTER; letterSpacing = .12f }, LinearLayout.LayoutParams(0, -2, 1f))
        row.addView(ui.text("0$step / 02", 12f, GnixViews.red, true))
        ui.add(content, row, 22)
        val progress = ui.row()
        for (i in 1..2) progress.addView(View(this).apply { background = ui.shape(if (i <= step) GnixViews.red else GnixViews.border, 3) }, LinearLayout.LayoutParams(0, ui.dp(3), 1f).apply { if (i == 1) rightMargin = ui.dp(7) })
        ui.add(content, progress, 28)
    }
    private fun welcome() {
        brand(content)
        ui.add(content, ui.text("O MUNDO, DO SEU JEITO.", 11f, GnixViews.red, true).apply { letterSpacing = .18f }, 38)
        val hero = ui.card().apply { setPadding(ui.dp(24), ui.dp(30), ui.dp(24), ui.dp(30)) }
        val icon = FrameLayout(this).apply {
            background = ui.shape(GnixViews.red, 20)
            addView(GnixIcon(this@MainActivity, "news", GnixViews.background), FrameLayout.LayoutParams(ui.dp(37), ui.dp(37), Gravity.CENTER))
        }
        hero.addView(icon, LinearLayout.LayoutParams(ui.dp(70), ui.dp(70)).apply { bottomMargin = ui.dp(24) })
        ui.add(hero, ui.text("Menos ruído.\nMais informação.", 34f, bold = true), 16)
        ui.add(hero, ui.text("As notícias que importam para você, reunidas em uma edição com as fontes que você escolhe.", 16f, GnixViews.muted), 10)
        ui.add(content, hero, 28)
        for ((number, text) in listOf("01" to "Escolha seus assuntos", "02" to "Selecione suas fontes", "03" to "Leia no seu ritmo")) {
            val row = ui.row()
            row.addView(ui.text(number, 13f, GnixViews.red, true), LinearLayout.LayoutParams(ui.dp(40), -2))
            row.addView(ui.text(text, 16f))
            ui.add(content, row, 20)
        }
        ui.add(footer, ui.primary("Criar minha edição") { page = Page.TOPICS; render() }, 12)
        ui.add(footer, ui.text("Suas fontes. Sua leitura. Sem cadastro.", 12f, GnixViews.muted).apply { gravity = Gravity.CENTER })
    }
    private fun topicPicker() {
        stepHeader(1)
        title("O que te interessa?", "Escolha os assuntos que você quer acompanhar.")
        val categories = (SourceCatalog.categories + custom.map { it.category }).distinct()
        val columns = DisplayPolicy.topicColumns(resources.configuration.screenWidthDp.coerceAtMost(720), resources.configuration.fontScale)
        for (pair in categories.chunked(columns)) {
            val row = ui.row()
            for ((index, cat) in pair.withIndex()) {
                val on = cat in topics
                val cell = ui.card(if (on) GnixViews.red else GnixViews.surface).apply {
                    gravity = Gravity.CENTER; setPadding(ui.dp(10), ui.dp(24), ui.dp(10), ui.dp(24)); minimumHeight = ui.dp(126)
                }
                ui.clickable(cell, 22, if (on) GnixViews.red else GnixViews.surface) {
                    if (on) topics.remove(cat) else topics.add(cat)
                    render()
                }
                cell.contentDescription = "$cat, ${if (on) "selecionado" else "não selecionado"}"
                cell.addView(GnixIcon(this, iconFor(cat), if (on) GnixViews.background else GnixViews.foreground), LinearLayout.LayoutParams(ui.dp(30), ui.dp(30)).apply { bottomMargin = ui.dp(14) })
                cell.addView(ui.text(cat, 15f, if (on) GnixViews.background else GnixViews.foreground, true))
                row.addView(cell, LinearLayout.LayoutParams(0, -2, 1f).apply { if (index < columns - 1) rightMargin = ui.dp(12) })
            }
            repeat(columns - pair.size) { row.addView(View(this), LinearLayout.LayoutParams(0, 1, 1f)) }
            ui.add(content, row, 12)
        }
        ui.add(content, ui.text("Você pode mudar suas escolhas quando quiser.", 13f, GnixViews.muted), 12)
        ui.add(footer, ui.primary(if (topics.isEmpty()) "Escolha ao menos um assunto" else "Continuar · ${topics.size} assuntos", topics.isNotEmpty()) {
            selected = SelectionPolicy.reconcile(sources, selected, topics, !configured).toMutableSet()
            page = Page.SOURCES; render()
        })
    }
    private fun sourcePicker() {
        if (configured) {
            val top = ui.row()
            top.addView(ui.iconButton("back", "Voltar") { handleBack() })
            top.addView(ui.text("SUAS PREFERÊNCIAS", 11f, GnixViews.muted, true), LinearLayout.LayoutParams(0, -2, 1f))
            ui.add(content, top, 24)
        } else stepHeader(2)
        title("Em quem você confia?", "Escolha as fontes da sua edição. Você está no controle.")
        sourceCount = ui.text("${selected.size} fontes selecionadas", 13f, GnixViews.red, true)
        ui.add(content, sourceCount!!, 20)
        val visible = if (topics.isEmpty() && configured) sources else sources.filter { it.category in topics }
        for (cat in visible.map { it.category }.distinct()) {
            val header = ui.row()
            header.addView(GnixIcon(this, iconFor(cat), GnixViews.muted), LinearLayout.LayoutParams(ui.dp(18), ui.dp(18)).apply { rightMargin = ui.dp(10) })
            header.addView(ui.text(cat.uppercase(Locale("pt", "BR")), 11f, GnixViews.muted, true).apply { letterSpacing = .12f })
            ui.add(content, header, 12)
            val panel = ui.card().apply { setPadding(ui.dp(18), ui.dp(5), ui.dp(18), ui.dp(5)) }
            val group = visible.filter { it.category == cat }
            group.forEachIndexed { index, source ->
                val row = ui.row().apply { minimumHeight = ui.dp(72) }
                val labels = ui.column()
                ui.add(labels, ui.text(source.name, 16f, bold = true), 5)
                ui.add(labels, ui.text(hostOf(source.url), 11f, GnixViews.muted))
                row.addView(labels, LinearLayout.LayoutParams(0, -2, 1f))
                val toggle = ui.switch(source.id in selected).apply { contentDescription = "Selecionar ${source.name}" }
                toggle.setOnCheckedChangeListener { _, checked ->
                    if (checked) selected.add(source.id) else selected.remove(source.id)
                    sourceCount?.text = "${selected.size} fontes selecionadas"
                    sourceContinue?.let { it.isEnabled = selected.isNotEmpty(); it.alpha = if (it.isEnabled) 1f else .4f }
                }
                row.addView(toggle)
                panel.addView(row)
                if (index < group.lastIndex) panel.addView(ui.divider())
            }
            ui.add(content, panel, 24)
        }
        ui.add(content, ui.secondary("+ Adicionar minha fonte RSS") { addSource() }, 12)
        if (configured) ui.add(content, ui.secondary("Escolher assuntos") { page = Page.TOPICS; render() })
        sourceContinue = ui.primary(if (configured) "Salvar preferências" else "Abrir minha edição", selected.isNotEmpty()) {
            if (selected.isEmpty()) return@primary
            if (!persist { store.saveSelection(selected); store.saveTopics(topics) }) return@primary
            configured = true; selectionRevision++; category = "Tudo"; page = Page.FEED
            articles = articles.filter { it.sourceId in selected }; notice = ""; render(); refresh()
        }
        ui.add(footer, sourceContinue!!)
    }
    private fun hostOf(url: String): String = try {
        Uri.parse(url).host ?: "Fonte personalizada"
    } catch (_: Exception) { "Fonte personalizada" }
    private fun addSource() {
        val fields = ui.column().apply { setPadding(ui.dp(24), ui.dp(12), ui.dp(24), 0) }
        fun field(hint: String, type: Int): EditText = EditText(this).apply {
            this.hint = hint; inputType = type; setTextColor(GnixViews.foreground); setHintTextColor(GnixViews.muted)
            minHeight = ui.dp(56); isSingleLine = true
        }
        val name = field("Nome da fonte", InputType.TYPE_CLASS_TEXT)
        val url = field("https://site.com ou endereço do feed", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        fields.addView(name); fields.addView(url)
        val spinner = Spinner(this)
        val categories = SourceCatalog.categories
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        fields.addView(spinner)
        val dialog = AlertDialog.Builder(this).setTitle("Adicionar fonte RSS").setMessage("Informe o endereço HTTPS do site ou do feed RSS/Atom.")
            .setView(fields).setNegativeButton("Cancelar", null).setPositiveButton("Adicionar", null).create()
        dialog.setOnShowListener {
            val button = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            button.setOnClickListener {
                val endpoint = url.text.toString().trim()
                if (name.text.isBlank()) { name.error = "Dê um nome à fonte"; return@setOnClickListener }
                if (!endpoint.startsWith("https://") || FeedParser.safeUrl(endpoint, endpoint).isEmpty()) { url.error = "Informe um endereço HTTPS válido"; return@setOnClickListener }
                if (sources.any { it.url == endpoint }) { url.error = "Esta fonte já foi adicionada"; return@setOnClickListener }
                val source = Source("custom-${UUID.randomUUID()}", name.text.toString().trim().take(80), endpoint, categories[spinner.selectedItemPosition])
                button.isEnabled = false; button.text = "Verificando…"
                worker.execute {
                    val result = runCatching { FeedClient().verifiedFeedUrl(source) }
                    runOnUiThread {
                        if (destroyed || !dialog.isShowing) return@runOnUiThread
                        button.isEnabled = true; button.text = "Adicionar"
                        if (result.isFailure) {
                            url.error = result.exceptionOrNull()?.message ?: "Não foi possível ler o feed. Confira o endereço e sua conexão."
                            return@runOnUiThread
                        }
                        val feedUrl = result.getOrThrow()
                        if (sources.any { it.url == feedUrl }) { url.error = "Esta fonte já foi adicionada"; return@runOnUiThread }
                        val resolved = Source(source.id, source.name, feedUrl, source.category)
                        val next = custom + resolved
                        if (persist { store.saveCustomSources(next) }) {
                            custom = next.toMutableList(); sources.add(resolved); topics.add(resolved.category); selected.add(resolved.id)
                            rebuildIndexes()
                            dialog.dismiss(); render()
                        }
                    }
                }
            }
        }
        dialog.show()
    }
    private fun edition() {
        val top = ui.row()
        val logo = ui.column(); brand(logo, true)
        top.addView(logo, LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(ui.iconButton("search", "Pesquisar notícias", searching) { searching = !searching; query = ""; render() }, LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        top.addView(ui.iconButton("refresh", "Atualizar notícias") { refresh() }, LinearLayout.LayoutParams(ui.dp(48), ui.dp(48)))
        ui.add(content, top, 18)
        val isSaved = page == Page.SAVED
        ui.add(content, ui.text(if (isSaved) "Sua biblioteca" else "Sua edição", 27f, bold = true), 7)
        ui.add(content, ui.text(if (isSaved) "Boas leituras, guardadas para depois." else editionDateFormat.format(Date()).replaceFirstChar { it.uppercase() }, 13f, GnixViews.muted), 20)
        if (searching) {
            val search = EditText(this).apply {
                hint = "Buscar título, assunto ou fonte"; setText(query); inputType = InputType.TYPE_CLASS_TEXT
                isSingleLine = true; textSize = 15f; setTextColor(GnixViews.foreground); setHintTextColor(GnixViews.muted)
                background = ui.shape(GnixViews.surface, 16); setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
            }
            ui.add(content, search, 18)
            search.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    query = s.toString()
                    searchRunnable?.let { mainHandler.removeCallbacks(it) }
                    val task = Runnable { if (!destroyed) drawArticles() }
                    searchRunnable = task
                    mainHandler.postDelayed(task, 250L)
                }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        val filters = ui.row()
        val items = if (isSaved) saved else articles
        val cats = items.mapNotNull { article -> sourceById[article.sourceId]?.category }.distinct()
        val filterCats = if (cats.size > 1) listOf("Tudo") + cats else listOf("Tudo")
        if (category !in filterCats) category = "Tudo"
        for (cat in filterCats) filters.addView(ui.pill(cat, cat == category) { category = cat; render() }, LinearLayout.LayoutParams(-2, -2).apply { rightMargin = ui.dp(8) })
        ui.add(content, HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false; addView(filters) }, 18)
        if (loading) {
            val status = ui.row()
            status.addView(ProgressBar(this).apply { indeterminateTintList = android.content.res.ColorStateList.valueOf(GnixViews.red) }, LinearLayout.LayoutParams(ui.dp(20), ui.dp(20)).apply { rightMargin = ui.dp(12) })
            status.addView(ui.text("Reunindo suas notícias…", 13f, GnixViews.muted))
            ui.add(content, status, 16)
        }
        if (notice.isNotEmpty() && !isSaved) {
            val info = ui.card(GnixViews.raised)
            ui.add(info, ui.text(notice, 13f, GnixViews.muted))
            ui.add(content, info, 16)
        }
        articleContainer = ui.column()
        ui.add(content, articleContainer!!)
        feedAdapter = ArticleAdapter(this, ui,
            sourceFor = { id -> sourceById[id] },
            isSaved = { url -> url in savedUrls },
            open = { openArticle(it) },
            toggleSaved = { article ->
                val wasSaved = article.url in savedUrls
                val next = if (wasSaved) saved.filterNot { it.url == article.url } else listOf(article) + saved
                if (persist { store.saveBookmarks(next) }) {
                    saved = next
                    rebuildIndexes()
                    drawArticles()
                    feedList?.announceForAccessibility(if (wasSaved) "Notícia removida dos salvos" else "Notícia salva")
                }
            })
        feedList?.adapter = feedAdapter
        drawArticles()
        bottomNav()
    }
    private var articleContainer: LinearLayout? = null
    private fun drawArticles() {
        val list = articleContainer ?: return
        list.removeAllViews()
        val isSaved = page == Page.SAVED
        val base = if (isSaved) saved else articles.filter { it.sourceId in selected }
        val needle = query.trim()
        val filtered = if (needle.isEmpty() && category == "Tudo") base else base.filter { article ->
            val src = sourceById[article.sourceId]
            (category == "Tudo" || src?.category == category) &&
                (needle.isEmpty() || article.title.contains(needle, ignoreCase = true) || article.summary.contains(needle, ignoreCase = true) || src?.name?.contains(needle, ignoreCase = true) == true)
        }
        feedAdapter?.submit(filtered)
        list.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
        if (filtered.isEmpty()) {
            val empty = ui.card().apply { gravity = Gravity.CENTER; setPadding(ui.dp(24), ui.dp(36), ui.dp(24), ui.dp(36)) }
            empty.addView(GnixIcon(this, if (isSaved) "bookmark" else "news", GnixViews.red), LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)).apply { bottomMargin = ui.dp(18) })
            val headline = when { query.isNotBlank() -> "Nenhuma notícia encontrada"; isSaved -> "Guarde uma boa leitura"; selected.isEmpty() -> "Sua edição começa nas fontes"; loading -> "Sua edição está a caminho"; category != "Tudo" -> "Ainda sem notícias neste assunto"; else -> "Vamos montar sua edição?" }
            ui.add(empty, ui.text(headline, 20f, bold = true).apply { gravity = Gravity.CENTER }, 10)
            val message = when { query.isNotBlank() -> "Tente buscar com outras palavras."; isSaved -> "Toque no marcador de uma notícia para ler depois."; selected.isEmpty() -> "Escolha ao menos uma fonte para acompanhar."; loading -> "Buscando publicações das fontes que você escolheu."; else -> "Atualize as notícias ou ajuste suas fontes. As fontes podem estar temporariamente indisponíveis." }
            ui.add(empty, ui.text(message, 14f, GnixViews.muted).apply { gravity = Gravity.CENTER }, 18)
            if (!isSaved && !loading && query.isBlank()) ui.add(empty, ui.secondary(if (selected.isEmpty()) "Escolher fontes" else "Atualizar notícias") {
                if (selected.isEmpty()) { page = Page.SOURCES; render() } else refresh()
            })
            ui.add(list, empty)
            return
        }
    }
    private fun openArticle(article: Article) {
        if (FeedParser.safeUrl(article.url, article.url).isEmpty()) { toast("Link inválido"); return }
        try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(article.url))) }
        catch (_: Exception) { toast("Nenhum navegador disponível para abrir esta notícia.") }
    }
    private fun bottomNav() {
        footer.setPadding(ui.dp(12), ui.dp(9), ui.dp(12), ui.dp(9))
        footer.addView(ui.divider())
        val row = ui.row()
        for ((label, icon, destination) in listOf(Triple("Edição", "news", Page.FEED), Triple("Fontes", "settings", Page.SOURCES), Triple("Salvos", "bookmark", Page.SAVED))) {
            val active = page == destination
            val item = ui.column().apply { gravity = Gravity.CENTER; minimumHeight = ui.dp(64); setPadding(0, ui.dp(10), 0, ui.dp(6)) }
            item.addView(GnixIcon(this, icon, if (active) GnixViews.red else GnixViews.muted), LinearLayout.LayoutParams(ui.dp(22), ui.dp(22)).apply { gravity = Gravity.CENTER_HORIZONTAL; bottomMargin = ui.dp(6) })
            item.addView(ui.text(label, 11f, if (active) GnixViews.red else GnixViews.muted, active).apply { gravity = Gravity.CENTER }, LinearLayout.LayoutParams(-1, -2))
            ui.clickable(item, 14, GnixViews.background) {
                if (destination == Page.SOURCES) { selected = store.loadSelection().toMutableSet(); topics = store.loadTopics().toMutableSet() }
                category = "Tudo"; query = ""; searching = false; page = destination; render()
            }
            row.addView(item, LinearLayout.LayoutParams(0, -2, 1f))
        }
        footer.addView(row)
    }
    private fun refresh() {
        if (loading) { toast("A atualização já está em andamento."); return }
        if (selected.isEmpty()) { notice = "Escolha suas fontes para montar uma edição."; render(); return }
        val snapshot = sources.filter { it.id in selected }
        val oldCache = articles
        val revision = selectionRevision
        loading = true; notice = ""; render()
        worker.execute {
            val result = NewsRepository(FeedClient(store)).refresh(snapshot, oldCache)
            runOnUiThread {
                if (destroyed) return@runOnUiThread
                loading = false
                if (revision != selectionRevision) { if (page == Page.FEED || page == Page.SAVED) refresh(); return@runOnUiThread }
                articles = result.articles
                val successCount = snapshot.size - result.failedSourceIds.size
                persist {
                    store.saveCache(articles)
                    store.saveRefreshTime(DisplayPolicy.refreshTimestamp(System.currentTimeMillis(), successCount, snapshot.size))
                }
                notice = if (result.failedSourceIds.isEmpty()) "" else {
                    val names = result.failedSourceIds.mapNotNull { id -> sourceById[id]?.name }.joinToString(", ")
                    "Não foi possível atualizar: $names. Mantivemos as notícias já disponíveis dessas fontes."
                }
                if (page == Page.FEED || page == Page.SAVED) render()
            }
        }
    }
}
