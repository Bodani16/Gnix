# Verificação — 30/09/2026

## Executado
- `bash test-core.sh`: **40 verificações passaram**. Compila o código Java de produção e o harness com o módulo `jdk.compiler`, sem avisos.
- Casos: RSS/Atom, descrição HTML, fonte, datas, link alternativo, URLs inseguros e ausentes, XML inválido, DOCTYPE, duplicatas, ordenação, falha parcial, modo offline com cache, seleção vazia, feed vazio e troca de assuntos.
- `bash -n build-apk.sh` e `bash -n test-core.sh`: sintaxe shell aceita.
- Manifest, estilos e ícone: XML bem formado.
- Revisão independente estática: identificou colisões de nomes de cores Kotlin, fontes ocultas selecionadas e links ausentes resolvidos para a URL do feed. Ajustes aplicados; regressões Java verificadas antes e depois da correção.

## Bloqueado / não executado
- Gradle, Kotlin Android e SDK: não instalados.
- Compilação APK, Android lint e assinatura: **não executados**.
- Emulador e aparelho: **não testados**.
- Feeds reais: **não validados ao vivo**.
- Workflow GitHub: **não executado**.
- Persistência e interface Android: código escrito, integração sem execução.

O download do SDK/Gradle retornou `curl: (7) Failed to connect to proxy port 8080`. Duas solicitações de permissão de rede foram interrompidas. Não houve instalação das ferramentas, publicação externa ou produção de APK. Testes do núcleo não substituem compilação ou teste Android.

## Decisões de execução
- Projeto isolado em `/workspace/gnix`, pois o `.git` do workspace está vazio e somente leitura. Custo: repositório de código separado.
- Núcleo portátil Java e interface Kotlin, pois o compilador Java local permite verificar a leitura de notícias sem SDK. Custo: duas linguagens no projeto.
- Testes executáveis pelo JDK substituem a execução Gradle enquanto as ferramentas não estão disponíveis. Custo: integração Android continua sem validação.
- Catálogo inicial mantém endpoints candidatos sem alegar validação. Custo: algumas fontes podem falhar ao atualizar; o app preserva cache e admite uma fonte RSS personalizada.

Não há APK incluído no arquivo de código-fonte.

## Ajustes para Android atual

AGP 8.13.2, Gradle 8.13 e alvo/compilação API 36. Novos testes de regras responsivas e atualização de cache foram vistos falhar antes da implementação, depois passaram: telefones estreitos, fonte grande, tela ampla, centralização, cache recente/antigo/vazio e sucesso/falha parcial.

Revisão estática independente da otimização não identificou novo erro definido de Kotlin/API, mas encontrou falha parcial marcada como atualização recente. Corrigida com regressão RED→GREEN: somente sucesso de todas as fontes produz timestamp recente; falha invalida a atualização.

Reciclagem dos cartões, ícones adaptativos, gestos, teclado e recortes estão implementados no código, mas ainda não foram executados no Android. Samsung, Motorola e Pixel não foram testados. Nova tentativa curta de acesso ao repositório Android retornou novamente conexão recusada ao proxy. Não há APK nesta entrega.

## Android 8.1 e superior

Mínimo alterado para API27 (Android8.1), por solicitação do usuário; alvo API36 preservado. Versão 1.1.1/código3. Revisão das APIs recentes: layout de ponta a ponta, IME e WindowMetrics são executados somente na API30+; OnBackInvokedDispatcher somente na API33+. Versões anteriores usam o retorno legado e o ajuste de teclado do sistema. Ícones adaptativos possuem recurso v26; monocromático é restrito a v33. Nenhum teste real em Android8.1 nem compilação do APK foi possível.
