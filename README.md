# Gnix

App Android nativo de notícias com identidade vermelho e preto, inspirado nas referências visuais enviadas pelo usuário.

## Estado desta entrega

**Código-fonte preparado; APK ainda não compilado.** A máquina de desenvolvimento tem Java 21, mas não possui SDK Android ou Gradle. Downloads falharam com conexão recusada ao proxy do sandbox; as solicitações de permissão de rede foram interrompidas. Não foi feita compilação de Kotlin, lint Android ou execução em emulador. Não há arquivo APK neste pacote e não se afirma que a compilação Android passou.

Foram executadas as verificações Java do leitor RSS/Atom e da combinação de notícias com cache. O cliente HTTP Java também foi compilado localmente. Veja `docs/verification.md` para o resultado atualizado. O código foi adaptado ao Android 16 (API 36), mas isso ainda não foi validado em aparelhos Samsung, Motorola ou Pixel.

## Funcionalidades no código

- Apresentação Gnix, seleção de assuntos em grade e escolha de fontes com interruptores.
- Edição personalizada em cartões, filtros por categoria da fonte e busca local.
- Atualização manual por RSS/Atom, links para a reportagem original e notícias salvas.
- Persistência privada de escolhas, cache e salvos, sem conta ou servidor próprio.
- Fonte RSS personalizada via HTTPS, verificada antes de ser adicionada.
- Títulos e descrições publicados pelas fontes; não há geração de texto por IA nem envio de e-mail.
- Android 8.1 ou superior; pacote `com.gnix.app`.

As categorias pertencem aos feeds escolhidos: não existe classificação automática de cada reportagem. Conteúdo pago continua sujeito às condições da fonte original.

## Fontes iniciais

G1, Agência Brasil, BBC News Brasil, Tecnoblog, Canaltech e feeds temáticos do G1/ge. **Esses endpoints são candidatos e não puderam ser validados ao vivo neste ambiente.** O aplicativo informa fontes que falham e mantém o cache disponível. Não há parceria ou afiliação com os veículos.

## Compilar no Linux

Requisitos: JDK 17+, Python 3, curl, unzip e acesso à Internet.

```bash
bash build-apk.sh
```

O script prepara Gradle 8.13 e, quando necessário, o SDK Android 36. Aceita as licenças Android para instalar as ferramentas. Executa os testes unitários, lint e assembleDebug, verifica a assinatura e grava `dist/Gnix.apk`.

Se o Android SDK já estiver instalado, defina `ANDROID_HOME` para esse caminho antes de executar. O instalador de SDK incluído no script é para Linux; no macOS/Windows, instale o SDK pelo Android Studio.

## Compilar pelo Android Studio

Abra a pasta do projeto no Android Studio com suporte a AGP 8.13.2. Instale Android SDK 36 e Build Tools 36.0.0. Configure Gradle 8.13 localmente ou use o script Linux para baixá-lo. Faça a sincronização e execute `:app:testDebugUnitTest`, `:app:lintDebug` e `:app:assembleDebug`. O resultado estará em `app/build/outputs/apk/debug/app-debug.apk`.

Não há Gradle Wrapper JAR nesta entrega: ele também precisa ser obtido pela rede. Depois de instalar Gradle, execute `gradle wrapper --gradle-version 8.13` para criar o wrapper oficial.

## Compilação automática opcional

O arquivo `.github/workflows/android.yml` está preparado para gerar e disponibilizar o APK como artifact em um repositório GitHub. **O workflow não foi executado nem publicado.** Ele pode ser acionado em Actions após o projeto ser colocado em um repositório pelo usuário. Nenhuma mensagem, upload ou publicação externa foi feita nesta sessão.

## Instalar o APK após compilar

Abra `Gnix.apk` no celular e permita a instalação pelo aplicativo que abriu o arquivo. Ou use `adb install -r dist/Gnix.apk`. A edição pode ser lida offline quando já há cache; atualizar precisa de Internet.

O APK de debug usa assinatura de desenvolvimento e não é uma entrega preparada para Play Store. Preserve a chave gerada em `~/.android/debug.keystore` para futuras atualizações compatíveis. Uma compilação com outra chave pode exigir desinstalação, apagando o armazenamento local.

## Verificações locais sem Android SDK

```bash
bash test-core.sh
```

Esse comando testa o código de produção Java usando o compilador do JDK. Não substitui testes de interface, persistência Android, lint ou compilação do APK.

## Adaptação ao Android atual

Versão do projeto: 1.1.1. Alvo Android 16/API 36, mantendo Android 8.1 como mínimo. Ferramentas: AGP 8.13.2, Gradle 8.13, SDK/Build Tools 36.

- API de Voltar do Android 13+ e compatibilidade com versões anteriores.
- Layout de ponta a ponta no Android 11+, respeitando barras, recortes e teclado.
- ListView nativa que recicla cartões visíveis; salvar não recria a tela inteira.
- Grade de assuntos com uma, duas ou três colunas conforme espaço e tamanho de fonte.
- Conteúdo centralizado em telas amplas, com suporte a janelas redimensionáveis.
- Ícones adaptativos e variante monocromática para ícones temáticos Android 13+.
- Cache recente evita atualização automática repetida por dez minutos; falhas invalidam esse prazo para permitir nova tentativa. Atualização manual continua disponível.

Não foram adicionados serviços em segundo plano, permissões de bateria especiais ou SDKs de fabricantes. O projeto não usa bibliotecas nativas, portanto não necessita APKs por ABI. Essas escolhas não representam certificação de funcionamento em cada aparelho.

Matriz de testes pendente: Android 8.1, 9, 10, 11, 12, 13, 14, 15 e 16; One UI em Samsung, Android Motorola e Pixel; gestos e navegação por três botões; recorte da câmera; fonte ampliada; teclado; rotação e janela dividida; perda de conexão e fontes indisponíveis. APK, consumo real de RAM/bateria e fluidez permanecem sem medição até a compilação e testes físicos.
