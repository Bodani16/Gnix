## 2024-05-24 - Main Thread String Allocation in Search Filter
**Learning:** During text searches that run on the main thread, concatenating strings for each item to do a single `.contains()` search allocates memory proportionally to the collection size on every keystroke, causing unnecessary GC churn and potential frame drops.
**Action:** Always prefer short-circuited boolean checks (`A.contains() || B.contains()`) over concatenating fields for unified search.
