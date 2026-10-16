# Epic 6 Context: Accessibilità, Dark Mode e Qualità Trasversale

<!-- Compiled from planning artifacts. Edit freely. Regenerate with compile-epic-context if planning docs change. -->

## Goal

Make TOGO usable and legible across accessibility needs and lighting conditions while protecting voice data. The app must expose complete screen-reader information, honor system font scaling without clipping, provide sufficiently large touch targets, and apply a complete dark palette with readable contrast. Validate the cross-cutting quality targets for input speed, category assignment, duplicate detection, and explicit user confirmation. Audio and recognized text must remain transient and limited to the current voice operation.

## Stories

- Story 6.1: Etichette TalkBack e Semantica Accessibile
- Story 6.2: Target Touch e Dynamic Type
- Story 6.3: Dark Mode Completa
- Story 6.4: Privacy Audio e Audit Performance

## Requirements & Constraints

- All meaningful information must have a textual equivalent; voice is not the only way to use the app. Support TalkBack on product rows, the checked-items section, App Bar actions, VoiceFAB, and duplicate-resolution choices. A product row must convey canonical name, quantity and unit, category, checked state, and the action to mark it purchased. The “Presi (N)” section must announce its item count and expanded/collapsed state.
- Maintain text/background contrast of at least 7:1 for accessibility; the dark primary text/background pair must meet this threshold. Preserve the light-theme high-contrast baseline as well.
- Every interactive control—including checkboxes, buttons, chips, App Bar icons, and FAB—needs a touch target of at least 48×48dp.
- Respect Android system font scaling, including the largest setting: product names must not be forcibly ellipsized, rows must grow with content without screen overflow, and quantity badges must remain legible and not overlap names.
- Voice privacy is strict: do not persist audio files or PCM buffers, save recognized text as a separate log, or send recognized text to remote endpoints. Use recognized text only for the current operation. List and history data remain local in the MVP.
- Acceptance targets: median manual addition ≤15 seconds; clear voice addition ≤10 seconds, excluding confirmation reading; ≥95% correct canonical-category assignments across at least 200 Italian test cases; exact-duplicate detection ≥98% precision and ≥95% recall; zero unconfirmed changes in acceptance tests. These are initial targets to validate on a representative test set.

## Technical Decisions

- Keep presentation in Jetpack Compose with ViewModels and immutable UI state, following the existing Clean Architecture/UDF boundary; accessibility semantics belong on the rendered controls and sections.
- Use the dedicated dark design tokens consistently: `surface-base-dark #0B0F17`, `surface-card-dark #131B2E`, `ink-primary-dark #F8FAFC`, `ink-secondary-dark #94A3B8`, `border-crisp-dark #475569`, and `accent-highlight-dark #60A5FA`. Do not substitute light tokens in dark mode.
- Dark appearance follows Android system mode and must remain consistent when the app enters background/foreground or switches between light and dark, without crashes or stale UI state.
- Voice recognition uses the OS recognition service; TOGO must not retain its audio or recognized text beyond current processing. Keep the manual/local app usable independently of voice availability.

## UX & Interaction Patterns

- Screen-reader labels should be complete and meaningful, not just control names. For an item, announce its name, spoken quantity and unit, category, checked/unchecked status, and the double-tap action. Announce section state and provide explicit content descriptions for App Bar controls and VoiceFAB. The duplicate dialog must expose its title and all available choices.
- Preserve the compact, high-contrast “High-Contrast Utility” visual identity in both themes. Dark-mode colors apply across ItemRow, CategoryHeader, VoiceBottomSheet, DuplicateResolutionDialog, and FAB; test each rather than only the main list surface.
- Layouts adapt to enlarged text instead of clipping or overlapping. Keep touch targets generous for one-handed use while shopping.

## Cross-Story Dependencies

- These are app-wide quality requirements applied to UI and flows established in earlier epics, especially the list rows/checked section, App Bar, voice sheet, and duplicate dialog. Dark-mode coverage must include those shared components rather than introduce parallel feature-specific styling.
- Privacy validation depends on the voice-recognition flow from Epic 4; category and duplicate-quality measurements exercise the catalog/input and deduplication behavior from Epic 3. Manual addition remains a fallback if voice recognition is unavailable.
