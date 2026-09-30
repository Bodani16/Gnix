# Gnix — proposta para a primeira versão Android

Status: escopo aprovado pelo usuário; implementação ainda não iniciada. Referências visuais recebidas posteriormente e incorporadas abaixo.

## Objetivo
Entregar um aplicativo Android instalável como APK. O usuário escolhe fontes de notícias e lê uma edição personalizada, no estilo newsletter. Nome do produto: Gnix. Idioma inicial: português brasileiro.

## Abordagens consideradas
1. Android nativo em Kotlin (recomendado): boa integração com Android, armazenamento local e instalação direta; exige preparar o SDK Android neste ambiente.
2. Flutter: adequado caso uma versão iOS seja planejada; acrescenta outro conjunto de ferramentas à compilação.
3. Site dentro de um aplicativo: facilita compartilhar uma interface web, mas oferece menos integração nativa. Não é a recomendação para este pedido.

## Escopo proposto
- Primeira abertura com escolha de fontes e confirmação das preferências.
- Tela “Sua edição”, com título, fonte, data e descrição disponibilizada pelo feed de cada notícia.
- Tela “Fontes”, com catálogo inicial de feeds públicos em português e opção de adicionar endereço RSS/Atom.
- Tela “Salvos”, com notícias marcadas para ler depois.
- Atualização manual das notícias e abertura da reportagem original no navegador.
- Preferências, notícias recentes e itens salvos armazenados no aparelho.
- Visual editorial com boa legibilidade, identidade Gnix em vermelho e preto, conforme ajuste solicitado pelo usuário.
- Seleção inicial de assuntos em grade de cartões, seguida da seleção das fontes correspondentes. Assuntos filtram o catálogo por categorias declaradas pelas fontes; não há classificação automática de reportagens por IA.

## Referência visual
Usar as quatro capturas enviadas pelo usuário como referência de estrutura: cartões arredondados, assuntos em grade, filtros horizontais e fontes com interruptores. Adaptar nome e marca para Gnix e aplicar a identidade vermelho e preto solicitada: fundo #0D0D0F, cartões #1B1B1F, texto principal #FAFAFA, texto secundário #B8B8C2 e destaque vermelho #E53935. Botões preenchidos em vermelho terão texto preto #0D0D0F para contraste; cartões de notícias manterão texto branco. Incluir acesso às notícias salvas. No feed, mostrar filtros horizontais para todas as fontes ou uma categoria do catálogo. Não reproduzir o mascote Gazeta nem as legendas e controles da rede social presentes nas capturas.

“Newsletter” nesta versão significa uma edição dentro do aplicativo. Não inclui envio por e-mail, conta de usuário ou resumo gerado por IA. Os textos apresentados são os títulos e descrições publicados pelas próprias fontes. A edição é montada ao abrir ou atualizar o app; não há promessa de entrega em horário programado.

## Arquitetura e dados
Aplicativo Kotlin, pacote `com.gnix.app`, Android 8.1 ou superior. Camadas pequenas para interface, leitura de RSS/Atom e persistência local. O repositório busca apenas feeds selecionados via HTTPS, converte-os para um modelo comum e remove duplicatas pelo endereço da reportagem. Ordenação por data, com fallback quando o feed não fornece data válida. Sem servidor próprio ou credenciais externas.

Links de reportagens e endereços de feeds serão limitados a HTTP/HTTPS, com feeds obtidos por HTTPS. Conteúdo HTML dos feeds será convertido para texto, sem execução de scripts. Downloads terão limite de tamanho e tempo de espera.

## Estados e falhas
Escolher pelo menos uma fonte para montar uma edição. Quando não houver notícias, mostrar orientação para selecionar fontes ou atualizar. Se uma fonte falhar, preservar notícias de outras fontes e indicar a falha. Sem conexão, permitir leitura do conteúdo já armazenado e dos itens salvos. Um feed inválido gera mensagem de erro sem apagar preferências.

## Validação e entrega
Validar interpretação de RSS/Atom, duplicatas, seleção de fontes e persistência. Compilar o APK e conferir pacote, versão mínima e assinatura. Fazer validação em emulador se houver suporte disponível; declarar explicitamente se essa validação não for possível. Entregar código-fonte, instruções de compilação e APK assinado para instalação direta. Assinatura para distribuição em loja e publicação na Play Store não fazem parte desta versão.

## Situação do ambiente
Workspace sem projeto existente. Java 21 disponível; Gradle e SDK Android não foram encontrados nos caminhos comuns nem no PATH. Preparação dessas ferramentas e disponibilidade dos feeds precisam ser verificadas durante a implementação. Não há APK compilado neste estágio.

## Ajuste posterior autorizado: Android atual
Foco em Samsung, Motorola e Google Pixel, usando APIs padrão Android. Alvo Android16/API36; mínimo Android8.1 (API27). Atualizar gesto Voltar, barras/recortes/teclado, grade responsiva, ícones adaptativos/temáticos e reciclagem dos cartões. Evitar atualizações automáticas repetidas com cache recente, sem mascarar falhas parciais. Preservar identidade vermelho/preto e escopo funcional aprovado. Testes nos fabricantes e compilação do APK continuam necessários antes de afirmar compatibilidade validada.
