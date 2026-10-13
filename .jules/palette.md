
## 2024-05-18 - TalkBack Grouping for Interactive Parent Containers
**Learning:** When grouping dynamic UI elements for TalkBack in Gnix, do not apply aggregated `contentDescription`s or focus directly to a parent container if it has interactive child elements (like nested buttons), as this swallows child focus and breaks their accessibility.
**Action:** Encapsulate the non-interactive text elements into a dedicated wrapper (e.g., `ui.column().apply { isFocusable = true }`), set `View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS` or `View.IMPORTANT_FOR_ACCESSIBILITY_NO` on its children, and keep the interactive elements as separate, accessible siblings.
