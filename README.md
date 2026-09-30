# Gnix

Aplicativo Android de notícias com visual vermelho e preto.

## Baixar o app

1. Abra a aba **[Actions](https://github.com/Bodani16/Gnix/actions)**.
2. Selecione a execução mais recente de **Compilar Gnix APK** com status concluído.
3. Na seção **Artifacts**, baixe **Gnix-APK-verificacao**.
4. Extraia o arquivo e abra `Gnix.apk` no celular.
5. Se necessário, permita a instalação de aplicativos de fontes desconhecidas.

> O APK das Actions é uma versão de verificação assinada com uma chave temporária. Para gerar uma versão local, siga as instruções abaixo.

## Principais recursos

- Escolha de assuntos e fontes de notícias.
- Filtros por categoria e busca local.
- Atualização manual por RSS/Atom.
- Acesso à reportagem original.
- Notícias salvas para leitura posterior.
- Cache para leitura offline.
- Adição de fontes RSS personalizadas via HTTPS.
- Sem conta, servidor próprio ou geração de textos por IA.

As categorias pertencem aos feeds escolhidos. O aplicativo não classifica automaticamente cada reportagem, e conteúdos pagos continuam sujeitos às regras da fonte original.

## Requisitos

- Android 8.1 ou superior.
- Pacote: `com.gnix.app`.
- O aplicativo não exige login nem arquivo `.env`.

## Gerar o APK localmente

No Linux, instale JDK 17+, Python 3, curl e unzip. Depois execute:

```bash
bash build-apk.sh
```

O APK será gerado em `dist/Gnix.apk`.

Para instalar usando ADB:

```bash
adb install -r dist/Gnix.apk
```

Também é possível abrir o projeto no Android Studio com Android SDK 36 e Gradle 8.13.

## Desenvolvimento

Para verificar o código Java sem instalar o Android SDK:

```bash
bash test-core.sh
```

A versão atual é **1.1.1**. O projeto usa Android API 36 como alvo e mantém Android 8.1 como versão mínima.
