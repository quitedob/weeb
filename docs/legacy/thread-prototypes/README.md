# Archived thread UI prototypes

These three unreferenced Vue components were removed from the production source tree during the report cleanup. They are preserved as text for product/design reference because the original report called out their unique reply UI.

They are not runnable components and have not been verified as a supported feature. They contain old API assumptions and unsafe HTML rendering. Do not copy them back into the application without updating their contracts, sanitizing HTML and adding access-control and interaction tests. The backend `/api/threads` controller exists independently; archiving this unused UI does not remove that API.

The separate legacy chat page's useful emoji search/package pagination has been ported into the active `PaginatedEmojiPicker.vue` component.
