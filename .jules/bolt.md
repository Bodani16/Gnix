## 2024-05-18 - Avoid String Interpolation in Tight Loops
**Learning:** String interpolation inside tight loops (like `filter` blocks) for string matching causes unnecessary memory allocations and GC thrashing on the UI thread, which degrades Android UI performance.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs.
