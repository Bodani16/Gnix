## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.

## 2024-05-25 - [Fix ReDoS Vulnerability in HTML Parser]
**Vulnerability:** Regular Expression Denial of Service (ReDoS) due to catastrophic backtracking in SCRIPT_STYLE regex.
**Learning:** Parsing untrusted HTML containing unclosed tags (like `<script>`) with a regex ending in `.*?</\1\s*>` causes exponential backtracking if the closing tag is missing. This can freeze the parsing thread indefinitely.
**Prevention:** Use an alternation with the end-of-string anchor `$` (e.g., `.*?(?:</\1\s*>|$)`) to allow the match to fail/consume fast without unbounded backtracking when the closing tag is missing.
