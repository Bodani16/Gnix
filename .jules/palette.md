
## 2024-05-23 - Aggregate Touch Targets and Screen Reader Focus
**Learning:** Individual inner elements (`TextView`s) in an Android custom view group that have independent listeners and `isFocusable = true` fragment the touch area and screen reader navigation. Users experience smaller touch targets and disjointed accessibility announcements.
**Action:** Remove inner focusable attributes and listeners. Apply a single click listener (`ui.clickable`) and an aggregated `contentDescription` on the parent container (`holder.card`) so the whole logical unit is read cohesively by screen readers and acts as a large touch target.
