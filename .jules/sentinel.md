## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.
## 2024-05-24 - [Implement Strict XXE Prevention via JAXP]
**Vulnerability:** Missing strict JAXP 1.5+ features to block external entity resolution natively in DocumentBuilderFactory.
**Learning:** For defense in depth against XXE, setting `accessExternalDTD` and `accessExternalSchema` properties to empty strings (`""`) on the `DocumentBuilderFactory` is required to strictly forbid external connections during XML parsing. Android's XMLConstants may not expose these directly at compile time in older APIs, so using literal strings like `"http://javax.xml.XMLConstants/property/accessExternalDTD"` inside a try-catch is safer.
**Prevention:** Always set `accessExternalDTD` and `accessExternalSchema` properties to `""` using string literals when parsing external XML.
