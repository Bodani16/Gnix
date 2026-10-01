## 2024-10-24 - Improve tap targets for news articles
**Learning:** Native Android TextView elements have small touch targets and individual `setOnClickListener` attachments create disjointed touch zones within a card, which is poor for accessibility and interaction design. The `ArticleAdapter` bound click listeners individually to headline, summary, and "read more" text.
**Action:** Consolidate click actions to the entire card layout using `ui.clickable()` on the card container, and add an appropriate overall content description for screen readers, moving away from fragmented touch targets.
