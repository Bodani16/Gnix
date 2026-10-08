## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.
## 2026-10-08 - Use String Literal for XML Secure Processing Constant
**Vulnerability:** Application crash on older Android versions and potential failure to set secure processing features.
**Learning:** Referencing `javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING` can cause a `NoClassDefFoundError` (which is an `Error`, not an `Exception`) on older Android APIs, bypassing the `catch (Exception ignored)` block.
**Prevention:** Use the exact string literal `"http://javax.xml.XMLConstants/feature/secure-processing"` when configuring `DocumentBuilderFactory` to prevent class-loading errors on older devices while safely applying security constraints.
