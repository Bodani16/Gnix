## 2024-05-24 - Avoid string interpolation in tight loops
**Learning:** String interpolation inside tight loops (like `filter` blocks) creates unnecessary memory allocations and causes GC thrashing on the UI thread, severely degrading performance during frequent re-evaluations (e.g., live search).
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs instead of concatenating strings before matching to prevent memory allocations.
