## 2026-10-08 - Prevent GC thrashing in filter loops
**Learning:** String interpolation inside tight loops (like `filter` blocks on the UI thread) causes unnecessary memory allocations and GC thrashing, which is a specific performance anti-pattern in this codebase's search implementation.
**Action:** Avoid string interpolation for string matching in loops. Instead, use individual `.contains()` checks combined with short-circuiting logical ORs to prevent allocations.
