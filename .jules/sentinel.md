## 2024-05-24 - [Enhance XXE Protection in FeedParser]
**Vulnerability:** Weak XML External Entity (XXE) protection relying partly on substring search (checking for "<!DOCTYPE") and missing key OWASP-recommended JAXP security feature flags.
**Learning:** Parsing untrusted XML data (like external RSS/Atom feeds) requires defensive configuration of DocumentBuilderFactory. String replacement/searching is insufficient as XML declarations can be encoded or obfuscated. Standard secure processing and explicit disallowance of DOCTYPE declarations are required.
**Prevention:** Always configure `DocumentBuilderFactory` with `FEATURE_SECURE_PROCESSING`, `disallow-doctype-decl`, and disable external entity resolution explicitly to prevent XXE, SSRF, and XML Bomb attacks.

## 2024-05-24 - [Fix ReDoS Vulnerability in FeedParser]
**Vulnerability:** Regular Expression Denial of Service (ReDoS) vulnerability in `SCRIPT_STYLE` regex `(?is)<(script|style)\\b[^>]*>.*?</\\1\\s*>`. An attacker could inject malformed HTML with unclosed `<script>` or `<style>` tags, causing catastrophic backtracking and freezing the app due to the unbounded lazy quantifier `.*?`.
**Learning:** Reluctant/lazy quantifiers (`.*?`) bounded by specific closing sequences (like closing tags) are dangerous if that closing sequence might not appear. The regex engine will backtrack excessively (quadratic or worse complexity) scanning to the end of the input string repeatedly.
**Prevention:** Bound such lazy quantifiers by the end of the string if the closing tag is missing (e.g., `(?:</\\1\\s*>|$)`), ensuring worst-case linear performance without catastrophic backtracking when dealing with untrusted user input.
