## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.

## 2024-05-25 - [Fix XXE Protection Bypass on Older Androids]
**Vulnerability:** The XML parser configuration caught `Exception` when configuring `FEATURE_SECURE_PROCESSING`, but referencing `javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING` can throw a `NoClassDefFoundError` (a subclass of `Error`, not `Exception`) on older Android API levels where the constant doesn't exist. This bypasses the try-catch block and aborts the remaining XML protection configurations, leaving the parser vulnerable to XXE.
**Learning:** Hardcoded class constants from newer Java/Android APIs can cause linkage errors (`Error`) that are not caught by standard `Exception` catch blocks, silently dropping security configurations.
**Prevention:** When configuring XML parsers (like `DocumentBuilderFactory`) across different Android API levels, always use the literal string values for JAXP properties instead of `javax.xml.XMLConstants` to guarantee the configuration attempt completes without throwing fatal linkage errors.
