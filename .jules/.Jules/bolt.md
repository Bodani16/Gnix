## 2026-10-10 - Avoid String Interpolation in Tight UI Loops
**Learning:** Using string interpolation (e.g., `"${a} ${b}".contains(x)`) inside tight loops like `filter` blocks on the UI thread causes unnecessary memory allocations and GC thrashing, leading to frame drops during operations like search.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs (e.g., `a.contains(x) || b.contains(x)`) instead of concatenating strings before checking.
