package com.gnix.app

/**
 * Fontes públicas conferidas em 2026-09-30; disponibilidade pode mudar.
 * Tecnoblog saiu do catálogo: passou a responder 403 a clientes não-navegador.
 * Reuters e AP não entram: a Reuters encerrou o RSS público (404) e a AP
 * bloqueia leitores automatizados (403), então nenhum endpoint é legível aqui.
 * Gazeta do Povo não publica RSS: /feed, /rss e variantes respondem 200 com o
 * HTML da home (catch-all da SPA) e /feed/atom/ devolve 403 do S3, então nem a
 * autodescoberta encontra feed. Entra pelo Google Notícias filtrado por domínio,
 * cujos links passam por um interstitial que só resolve com JavaScript.
 */
object SourceCatalog {
    val initial = listOf(
        Source("g1", "G1", "https://g1.globo.com/rss/g1/", "Geral"),
        Source("agencia", "Agência Brasil", "https://agenciabrasil.ebc.com.br/rss/ultimasnoticias/feed.xml", "Geral"),
        Source("gazetadopovo", "Gazeta do Povo", "https://news.google.com/rss/search?q=site:gazetadopovo.com.br&hl=pt-BR&gl=BR&ceid=BR:pt-419", "Geral"),
        Source("bbc", "BBC News Brasil", "https://feeds.bbci.co.uk/portuguese/rss.xml", "Mundo"),
        Source("g1-mundo", "G1 · Mundo", "https://g1.globo.com/rss/g1/mundo/", "Mundo"),
        Source("dw", "DW Brasil", "https://rss.dw.com/rdf/rss-br-all", "Mundo"),
        Source("rfi", "RFI Brasil", "https://www.rfi.fr/br/rss", "Mundo"),
        Source("g1-economia", "G1 · Economia", "https://g1.globo.com/rss/g1/economia/", "Economia"),
        Source("olhardigital", "Olhar Digital", "https://olhardigital.com.br/feed/", "Tecnologia"),
        Source("canaltech", "Canaltech", "https://canaltech.com.br/rss/", "Tecnologia"),
        Source("ge", "ge · Esportes", "https://ge.globo.com/rss/ge/", "Esportes"),
        Source("g1-saude", "G1 · Ciência e Saúde", "https://g1.globo.com/rss/g1/ciencia-e-saude/", "Saúde"),
        Source("g1-politica", "G1 · Política", "https://g1.globo.com/rss/g1/politica/", "Política"),
        Source("gamevicio", "GameVicio", "https://www.gamevicio.com/rss/", "Jogos"),
        Source("g1-videos", "G1 · Vídeos", "https://g1.globo.com/rss/g1/videos/", "Vídeos"),
        Source("g1-cultura", "G1 · Pop & Arte", "https://g1.globo.com/rss/g1/pop-arte/", "Cultura")
    )
    val categories = listOf("Geral", "Mundo", "Política", "Economia", "Tecnologia", "Jogos", "Esportes", "Saúde", "Vídeos", "Cultura")
}
