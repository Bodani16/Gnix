## 2024-05-24 - Avoid string interpolation in tight loops
**Learning:** String interpolation like `"${article.title} ${article.summary} ${src?.name.orEmpty()}"` inside a tight `filter` block causes unnecessary memory allocations and GC thrashing on the UI thread, which drops frames.
**Action:** Use individual `.contains()` checks combined with short-circuiting logical ORs `||` instead of concatenating strings first.
