## 2026-10-09 - Avoid String Interpolation in Tight Loops
**Learning:** Using string interpolation inside tight loops (like `filter` blocks) for string matching causes unnecessary memory allocations and garbage collection thrashing on the UI thread, which can negatively impact performance during search typing.
**Action:** Instead of string interpolation, use individual `.contains()` checks combined with short-circuiting logical ORs to prevent unnecessary memory allocations and improve performance.
