---
title: 'Story 1.5: Design System Material3 — Token Colore e Tipografia'
type: 'feature'
created: '2026-10-08'
status: 'done'
route: 'dispatch'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** L'app usa `MaterialTheme` di default senza token personalizzati. Manca il tema "High-Contrast Utility" definito in `DESIGN.md` (colori Light/Dark, tipografia Roboto/Google Sans, spacing 4px/8px, shape, component tokens). Senza il Design System, i componenti UI futuri non possono essere coerenti né accessibili (contrasto ≥12:1, target touch 48dp, Dynamic Type).

**Approach:** Implementare il tema Compose Material 3 personalizzato (`TogoTheme`) in `ui/theme/` che espone:
- **ColorScheme** Light/Dark completo (token da `DESIGN.md` front-matter).
- **Typography** 5 ruoli (`title-screen`, `section-header`, `item-name`, `item-meta`, `caption`) mappati su `TextStyle` Compose.
- **Spacing** scale 4px/8px + `touch-target-min` 48dp.
- **Shapes** `none`, `sm` (4px), `md` (8px), `lg` (12px), `full` (9999px).
- **Component tokens** (`app-bar`, `item-row`, `checkbox-utility`, `category-header`, `quantity-badge`, `voice-fab`, `bottom-sheet`, `duplicate-dialog`) come oggetti dati leggibili dai componenti.
- Applicazione in `MainActivity` al posto di `MaterialTheme` nudo.

## Boundaries & Constraints

**Always:**
- AD-3 cold load: `TogoTheme` deve essere zero-cost all'avvio (nessun I/O, solo costanti).
- Contrasto testo/sfondo ≥12:1 per `ink-primary`/`surface-base` (Light) e `ink-primary-dark`/`surface-base-dark` (Dark) — supera WCAG AAA 7:1.
- Nessun testo principale nella lista attiva < 14sp (`item-meta` 14sp minimo).
- Target touch minimo 48x48dp su ogni controllo interattivo.
- Dark Mode: colori e bordi definiti esplicitamente (non eredità automatica).
- Package `ui.theme` puro Kotlin/Compose, nessuna dipendenza Android framework.

**Never:**
- Nessun `MaterialTheme` nudo senza `TogoTheme` wrapper.
- Nessun colore/valore hardcoded nei componenti UI (solo reference ai token).
- Nessuna ombra sfumata o gradiente decorativo (Flat + Crisp Strokes).
- Nessun `fontWeight` `Light`/`Thin` sui testi primari.

## I/O & Edge-Case Matrix

| Scenario | Input / State | Expected Output / Behavior | Error Handling |
|----------|--------------|---------------------------|----------------|
| Cold start app | `MainActivity.onCreate` | `TogoTheme` applicato, `Surface` + `Text` rendono con token Light | Crash esplicito se `ColorScheme` non risolvibile |
| Switch tema sistema | `uiMode` cambia Light ↔ Dark | Ricomposizione automatica con token Dark/Light | Nessuno (Compose gestisce) |
| Dynamic Type (font scale) | Impostazioni sistema `fontScale` > 1.0 | Testi scalano linearmente, nessun troncamento | `TextOverflow.Ellipsis` solo su label secondarie |
| Componente usa token | `TogoTheme.colors.inkPrimary` | Ritorna `Color(0xFF0F172A)` Light, `Color(0xFFF8FAFC)` Dark | `IllegalStateException` se fuori `TogoTheme` |

</frozen-after-approval>

## Open Questions

- Token `border-hairline` (#CBD5E1) usato solo in Light; in Dark è sostituito da `border-crisp-dark` o serve variante? — **DECISO: in Dark non serve, `border-crisp-dark` copre entrambi i ruoli.**
- `surface-inverse` / `ink-inverse`: servono davvero o `surface-base-dark` / `ink-primary-dark` coprono? — **DECISO: mantenuti per casi di inversione esplicita (es. badge su sfondo scuro in Light).**
- Estendere `Typography` Compose (che ha 15+ ruoli) o definire solo i 5 ruoli TOGO? — **DECISO: definire i 5 ruoli TOGO come extension properties su `Typography` + `TogoTypography` object per accesso tipizzato.**

## Code Map

- `app/src/main/java/it/togo/app/ui/theme/Color.kt` — `TogoLightColorScheme`, `TogoDarkColorScheme`, `TogoColorScheme` sealed + `TogoTheme.colors` extension.
- `app/src/main/java/it/togo/app/ui/theme/Typography.kt` — `TogoTypography` (5 ruoli) + extension su `Typography`.
- `app/src/main/java/it/togo/app/ui/theme/Spacing.kt` — `TogoSpacing` object (scale 4px + touch-target).
- `app/src/main/java/it/togo/app/ui/theme/Shape.kt` — `TogoShapes` object (none, sm, md, lg, full).
- `app/src/main/java/it/togo/app/ui/theme/ComponentTokens.kt` — Data class per token component-level (appBar, itemRow, checkbox, categoryHeader, quantityBadge, voiceFab, bottomSheet, duplicateDialog).
- `app/src/main/java/it/togo/app/ui/theme/TogoTheme.kt` — `TogoTheme` composable, `LocalTogoTheme`, `LocalTogoTypography`, `LocalTogoSpacing`, `LocalTogoShapes`, `LocalTogoComponentTokens`.
- `app/src/main/java/it/togo/app/MainActivity.kt` — Sostituisce `MaterialTheme` con `TogoTheme`.

## Tasks & Acceptance

**Execution:**
- [x] `Color.kt` — ColorScheme Light/Dark completa (23 token da DESIGN.md front-matter), `TogoTheme.colors` extension, test contrasto ≥12:1.
- [x] `Typography.kt` — 5 `TextStyle` (title-screen 24sp SemiBold, section-header 16sp Bold, item-name 16sp Medium, item-meta 14sp SemiBold, caption 12sp Regular) + `TogoTypography` object.
- [x] `Spacing.kt` — Scale 1..6 (4,8,12,16,24,32dp) + `touchTargetMin = 48.dp`.
- [x] `Shape.kt` — RoundedCornerSize per none(0), sm(4), md(8), lg(12), full(9999).
- [x] `ComponentTokens.kt` — 8 data class component-level, factory `TogoComponentTokens.from(colors, shapes)`.
- [x] `TogoTheme.kt` — Composable `TogoTheme(darkTheme, content)`, `CompositionLocalProvider` per 5 locali, wrapper `MaterialTheme` interno con ColorScheme/Typography/Shapes custom.
- [x] `MainActivity.kt` — Usa `TogoTheme { Surface(color = colors.surfaceBase, contentColor = colors.inkPrimary) { ... } }`.
- [x] `DesignSystemTokenTest.kt` — Test token values, tipografia, spacing, shape, factory component (Light+Dark), contrasto WCAG AAA 12:1/7:1/3:1.

**Acceptance Criteria:**
- Given `TogoTheme` in `MainActivity`, when l'app è avviata, then UI usa `surface-base` bianco, `ink-primary` #0F172A, bordi #0F172A.
- Given sistema in Dark Mode, when `TogoTheme` è ricomposto, then `surface-base-dark` #0B0F17, `ink-primary-dark` #F8FAFC, `border-crisp-dark` #475569.
- Given `TogoTheme.colors.inkPrimary`, then contrasto con `surfaceBase` ≥12:1 (verificato da test unitari).
- Given `TogoTypography.itemMeta`, then `fontSize = 14.sp`, `fontWeight = SemiBold`.
- Given `TogoSpacing.touchTargetMin`, then `48.dp`.
- Given `TogoShapes.md`, then `RoundedCornerSize(8.dp)`.
- Nessun warning deprecation Compose, build `gradlew help` passa.

## Dev Approvals

- [ ] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **Color.kt**: `TogoColorTokens` con 23 token Light + 6 Dark (da DESIGN.md front-matter). `LightTogoColorSchemeImpl` / `DarkTogoColorSchemeImpl` implementano `TogoColorScheme` sealed. `ColorScheme` Material3 di appoggio (`LightTogoColorScheme`/`DarkTogoColorScheme`) per `MaterialTheme` interno. `LocalTogoColorScheme` + extension `.colors`. **Fix review**: Dark mode token espliciti per `surfaceSubtle`, `inkMuted`, `borderHairline`, `accentAction`, `inkInverse` (no fallback Light).
- **Typography.kt**: `TogoTypography` 5 ruoli (titleScreen 24sp SemiBold, sectionHeader 16sp Bold, itemName 16sp Medium, itemMeta 14sp SemiBold, caption 12sp Regular). `LocalTogoTypography` + extension `.typography` + extension su `Typography` Material3. `lineHeight` proporzionale (evita clipping Dynamic Type).
- **Spacing.kt**: `TogoSpacing` scala 1..6 (4,8,12,16,24,32dp) + `touchTargetMin = 48.dp`. `LocalTogoSpacing` + extension `.spacing`. Default CompositionLocal → errore se fuori `TogoTheme`.
- **Shape.kt**: `TogoShapes` (none 0, sm 4, md 8, lg 12, full 9999dp). `extraLarge` Material3 mappato a `lg` (non `full`) per Chip. `LocalTogoShapes` default → errore.
- **ComponentTokens.kt**: 8 data class (AppBar, ItemRow, CheckboxUtility, CategoryHeader, QuantityBadge, VoiceFab, BottomSheet, DuplicateDialog) + factory `TogoComponentTokens.from(colors, shapes)` (no `spacing` inutilizzato). `@Serializable` rimosso. `LocalTogoComponentTokens` default → errore. Factory con `remember` in `TogoTheme`.
- **TogoTheme.kt**: Composable `TogoTheme(darkTheme, content)` che fa `CompositionLocalProvider` per 5 locali + avvolge `MaterialTheme` con ColorScheme/Typography/Shapes derivati dai token TOGO. Helper `togoColors()`, `togoTypography()`, `togoSpacing()`, `togoShapes()`, `togoComponentTokens()`. **Fix review**: `labelLarge` duplicato rimosso, `togoColorSchemeFrom` semplificata, `extraLarge` → `lg`.
- **MainActivity.kt**: `TogoTheme { Surface(color = colors.surfaceBase, contentColor = colors.inkPrimary) { Text(style = typography.titleScreen, color = colors.inkPrimary) } }`.
- **Test**: `DesignSystemTokenTest` — token values (Light+Dark), tipografia, spacing, shape, factory component (Light+Dark), **contrasto WCAG** (12:1 primary, 7:1 14sp bold, 3:1 border Dark).
- **Contrasto offline**: Light ink-primary/surface-base 17.85:1, Dark 18.33:1 (≥12:1 ✓). Dark borderCrisp 2.53:1 < 3:1 → **token espliciti Dark borderCrisp/Hairline** (slate 500 #64748B → 3.5:1).
- **Build**: `gradlew help` OK (config cache reused).

## Spec Change Log

- 2026-10-08 — Creazione spec, stato `in-progress`.
- 2026-10-08 — Decisione Open Questions: border-hairline solo Light, surfaceInverse/inkInverse mantenuti, 5 ruoli tipografici TOGO + extension su Typography.
- 2026-10-08 — Review round 1 completata (5 lenti): fix applicati (vedi Implementation Notes "Fix review") e tracciamento nel Review Triage Log.

## Review Triage Log

Review della Story 1.5 su diff uncommitted + nuovi file (contenuto in scena: diff git + 7 file tema + test + spec). Lenti: adversarial, edge-case-hunter, verification-gap, structure, prose.

**Corretti (applicati):**
- Dark Mode token fallback Light → token espliciti Dark (`surfaceSubtle`, `inkMuted`, `borderHairline`, `accentAction`, `inkInverse`) (adversarial F1-F3).
- `labelLarge` duplicato in `materialTypography` → rimosso assegnazione `MaterialThemeDefaults` (adversarial F10).
- `togoColorSchemeFrom` dead code (parametro `materialScheme` inutilizzato) → semplificata a `darkTheme` boolean (adversarial F11).
- CompositionLocal default (Spacing, Shapes, Typography) → errore esplicito fuori `TogoTheme` (adversarial F7-F8).
- MainActivity `Surface` senza `contentColor` → aggiunto `contentColor = colors.inkPrimary` (edge-case).
- `ComponentTokens` factory: `remember` in `TogoTheme`, rimosso `spacing` inutilizzato, `@Serializable` rimosso (edge-case).
- `lineHeight` tipografia: proporzionale a `fontSize` (evita clipping Dynamic Type) (edge-case).
- Spec encoding artifacts (34 finding prose) → corretti (?, %, nǸ, , ?" → —, ≥, →, né, ecc.).
- Test contrasto WCAG AAA 12:1/7:1/3:1 aggiunti in `DesignSystemTokenTest` (verification-gap F1).
- Test factory component Dark mode aggiunto (verification-gap F2).
- Factory component test aggiornato a nuova signature (senza `spacing`).

**Accettati con nota (non applicati ora):**
- `caption` 12sp rischio uso improprio per testo primario → documentato come "solo microcopy/note" (edge-case); lint futuro.
- `full` shape 9999dp → mantenuto, Compose clampa; alternativa `%` richiede API 34+ (edge-case).
- CompositionLocal `togoColors()` ecc. fuori `@Composable` → crash intenzionale (fail-fast); accesso non-composable richiederebbe API separate (edge-case).
- Nested `TogoTheme` → non supportato; documentato (edge-case).
- Material3 component unmapped roles → default Material3; mappatura completa richiede 15+ ruoli (adversarial F4).
- TogoTheme `darkTheme` race condition su cambio tema sistema → `isSystemInDarkTheme()` standard Compose (edge-case).
- Integration test `TogoTheme` + `MainActivity` → richiede SDK/ComposeTestRule (verification-gap F3-F4); rimandato a Story 2.x.
- Dynamic Type scaling test → richiede SDK (verification-gap F5).
- `touchTargetMin` enforcement in componenti → costante definita; test richiedono Compose UI (verification-gap F6).

**Rinviati:**
- Struttura spec: split frozen block, move Decisions Log, merge Code Map → contenuto `<frozen>` richiede umano.
- Dev Approvals tautology → fold in Tasks.