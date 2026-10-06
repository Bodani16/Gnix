## 2024-05-24 - Aggregated accessibility focus for article cards
**Learning:** TalkBack reads each focusable text and touch target inside the card separately, causing fragmented navigation. Applying an aggregated `contentDescription` to the parent and disabling individual elements' a11y focus creates a cohesive reading experience.
**Action:** Use `ui.clickable()` on the parent card, assign a unified `contentDescription`, and set `View.IMPORTANT_FOR_ACCESSIBILITY_NO` on child elements to ensure cohesive TalkBack focus.
