## 2024-05-24 - Avoid string interpolation in filter blocks for string matching
**Learning:** String interpolation inside a tight loop (like a `filter` block in `MainActivity.kt`) causes unnecessary memory allocations for every evaluated item, leading to GC thrashing on the UI thread.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs to prevent unnecessary string creation and improve UI performance during operations like search.
