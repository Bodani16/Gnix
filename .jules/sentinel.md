## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.

## 2024-10-13 - [Prevent Security Downgrade on Older Android APIs]
**Vulnerability:** Bypass of XML parser security features (`FEATURE_SECURE_PROCESSING`) on older Android APIs due to unhandled `NoClassDefFoundError` when accessing `javax.xml.XMLConstants`.
**Learning:** Referencing `javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING` causes a `NoClassDefFoundError` on older Android versions where the class is absent. Because `Error` is not caught by `catch (Exception e)`, the initialization block aborts early, leaving subsequent security features unconfigured and the parser vulnerable.
**Prevention:** Use the hardcoded string literal `"http://javax.xml.XMLConstants/feature/secure-processing"` instead of the class constant, and always broaden exception handling to catch `Throwable` during defensive parser initialization.
