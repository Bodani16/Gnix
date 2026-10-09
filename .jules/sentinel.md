## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.
## 2023-10-24 - Secure Processing Bypass via NoClassDefFoundError
**Vulnerability:** Use of `javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING` caused a potential unhandled `Error` on older Android APIs.
**Learning:** Older Android versions lack some constants. Because `NoClassDefFoundError` extends `Error`, it bypasses `catch (Exception)`, potentially crashing the parser thread before XXE defenses were fully applied.
**Prevention:** Use exact string literals (e.g., `"http://javax.xml.XMLConstants/feature/secure-processing"`) for JAXP features instead of constants, as already done for other parser features.
