## 2026-10-11 - Optimize string search in filter block
**Learning:** Avoid string interpolation inside tight loops (like `filter` blocks) for string matching. It causes unnecessary memory allocations and GC thrashing on the UI thread.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs to prevent GC thrashing.
