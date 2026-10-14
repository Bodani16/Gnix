## 2024-05-24 - Avoid String Interpolation in Tight Filter Loops
**Learning:** Using string interpolation (e.g., `"${a} ${b}".contains(x)`) inside a tight filtering loop on the main UI thread creates excessive short-lived allocations, leading to memory pressure and garbage collection (GC) thrashing. This causes dropped frames during typing/searching.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs (e.g., `a.contains(x) || b.contains(x)`) to avoid unnecessary memory allocations entirely.
