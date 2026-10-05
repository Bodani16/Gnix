## 2024-05-24 - Avoid string interpolation in filter blocks
**Learning:** In Android/Kotlin, using string interpolation (like `"${obj.a} ${obj.b}"`) inside tight loops like `.filter` creates many temporary String allocations, leading to high Garbage Collection overhead and potential UI thread stuttering.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs to prevent unnecessary memory allocations and GC thrashing.
