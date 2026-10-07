---
name: Togo
description: Local-first Android shopping list app. High-Contrast Utility, minimal, fast in-store scanning, touch-first.
status: final
created: 2026-10-07
updated: 2026-10-07
colors:
  surface-base: '#FFFFFF'
  surface-subtle: '#F8FAFC'
  surface-card: '#FFFFFF'
  surface-inverse: '#0F172A'
  ink-primary: '#0F172A'
  ink-secondary: '#475569'
  ink-muted: '#94A3B8'
  ink-inverse: '#FFFFFF'
  border-crisp: '#0F172A'
  border-hairline: '#CBD5E1'
  accent-action: '#0F172A'
  accent-success: '#15803D'
  accent-highlight: '#2563EB'
  accent-warning: '#B45309'
  badge-bg: '#0F172A'
  badge-ink: '#FFFFFF'
  surface-base-dark: '#0B0F17'
  surface-card-dark: '#131B2E'
  ink-primary-dark: '#F8FAFC'
  ink-secondary-dark: '#94A3B8'
  border-crisp-dark: '#475569'
  accent-highlight-dark: '#60A5FA'
typography:
  title-screen:
    note: 'Android Headline Medium · Roboto 24sp SemiBold · Line Height 32sp'
  section-header:
    note: 'Android Title Medium · Roboto 16sp Bold · Line Height 24sp · Letter Spacing 0.15sp'
  item-name:
    note: 'Android Body Large · Roboto 16sp Medium · Line Height 24sp'
  item-meta:
    note: 'Android Label Large · Roboto 14sp SemiBold · Line Height 20sp'
  caption:
    note: 'Android Body Small · Roboto 12sp Regular · Line Height 16sp'
rounded:
  none: '0px'
  sm: '4px'
  md: '8px'
  lg: '12px'
  full: '9999px'
spacing:
  '1': '4px'
  '2': '8px'
  '3': '12px'
  '4': '16px'
  '5': '24px'
  '6': '32px'
  'touch-target-min': '48px'
components:
  app-bar:
    bg: '{colors.surface-base}'
    border-bottom: '2px solid {colors.border-crisp}'
    height: '64px'
  item-row:
    min-height: '{spacing.touch-target-min}'
    border: '1.5px solid {colors.border-crisp}'
    radius: '{rounded.md}'
    bg: '{colors.surface-card}'
    padding: '{spacing.3} {spacing.4}'
  checkbox-utility:
    size: '26px'
    border: '2px solid {colors.border-crisp}'
    radius: '{rounded.sm}'
    check-color: '{colors.ink-inverse}'
    checked-bg: '{colors.accent-action}'
  category-header:
    bg: '{colors.surface-subtle}'
    border-left: '4px solid {colors.border-crisp}'
    padding: '{spacing.2} {spacing.3}'
  quantity-badge:
    bg: '{colors.badge-bg}'
    text-color: '{colors.badge-ink}'
    radius: '{rounded.sm}'
    padding: '{spacing.1} {spacing.2}'
  voice-fab:
    size: '64px'
    bg: '{colors.accent-action}'
    text-color: '{colors.ink-inverse}'
    border: '2px solid {colors.border-crisp}'
    radius: '{rounded.full}'
  bottom-sheet:
    bg: '{colors.surface-base}'
    border-top: '2px solid {colors.border-crisp}'
    radius: '{rounded.lg} {rounded.lg} 0 0'
  duplicate-dialog:
    bg: '{colors.surface-base}'
    border: '2px solid {colors.border-crisp}'
    radius: '{rounded.lg}'
---

# TOGO — Design Spine

> Riferimento visivo ufficiale (Google Labs DESIGN.md spec). Definisce lo stile, i token visivi e le regole di rendering per l'applicazione Android TOGO.  
> Spines win on conflict: questo documento e `EXPERIENCE.md` vincono su qualunque wireframe, prototipo o mockup.
> Mockup di riferimento per la composizione: [`mockups/active-list.html`](mockups/active-list.html), [`mockups/voice-sheet.html`](mockups/voice-sheet.html), [`mockups/duplicate-dialog.html`](mockups/duplicate-dialog.html), [`mockups/history.html`](mockups/history.html).

---

## 1. Brand & Style

TOGO è un'applicazione Android local-first progettata come uno strumento di lavoro rapido e affidabile per la spesa quotidiana. Rifiuta l'estetica decorativa, le sfumature pastello e le animazioni leziose in favore della **High-Contrast Utility**:
- **Chiarezza istantanea:** testi neri nitidi su sfondo bianco puro, contorni netti da 1.5px e 2px per separare gli elementi con rigore geometrico.
- **Resistenza all'ambiente:** leggibilità impeccabile sotto la luce al neon fredda del supermercato o sotto la luce solare diretta.
- **Ergonomia ad una mano:** target touch ampi (minimo `{spacing.touch-target-min}`), contrasto massimo tra elementi da prendere ed elementi presi, badge quantità evidenti e scansionabili in frazioni di secondo mentre si spinge il carrello.

---

## 2. Colors

La palette sfrutta il massimo contrasto acromatico con accenti funzionali discreti:

- `{colors.surface-base}` (`#FFFFFF`): Sfondo primario delle schermate e dei fogli modali. Luce pulita, priva di dominante cromatica.
- `{colors.surface-subtle}` (`#F8FAFC`): Grigio chiarissimo per banner di categoria e aree secondarie.
- `{colors.ink-primary}` (`#0F172A`): Testo primario, titoli, bordi strutturali. Contrasto > 12:1 con lo sfondo.
- `{colors.ink-secondary}` (`#475569`): Testo di supporto, attributi opzionali (marca, variante).
- `{colors.ink-muted}` (`#94A3B8`): Testo di articoli depennati o elementi disabilitati.
- `{colors.border-crisp}` (`#0F172A`): Bordo nero/antracite profondo per card, dialoghi, selettori e divider.
- `{colors.badge-bg}` (`#0F172A`) e `{colors.badge-ink}` (`#FFFFFF`): Badge di quantità invertito ad altissimo contrasto per attirare l'occhio sul valore numerico (es. `2 l`, `1 kg`).
- `{colors.accent-success}` (`#15803D`): Verde saldo per la spunta di acquisto confermato e micro-feedback positivi.
- `{colors.accent-highlight}` (`#2563EB`): Blu tecnico per indicare collegamenti di correzione rapida o indicatori di ascolto vocale.

Supporto Dark Mode:
In modalità scura (`{colors.surface-base-dark}` `#0B0F17`), i testi passano a `{colors.ink-primary-dark}` (`#F8FAFC`) con bordi grigio-ardesia `{colors.border-crisp-dark}` (`#475569`) per preservare la batteria OLED e non abbagliare al buio.

---

## 3. Typography

TOGO eredita la ramp tipografica nativa di **Android (Material 3 / Jetpack Compose)** basata su Roboto / Google Sans:

| Ruolo | Token | Spec nativa Android | Uso |
|---|---|---|---|
| Titolo Schermata | `{typography.title-screen}` | Headline Medium, 24sp, SemiBold | Header "Spesa", "Storico prodotti" |
| Intestazione Categoria | `{typography.section-header}` | Title Medium, 16sp, Bold, uppercase tracking | "ORTOFRUTTA", "BANCO FRIGO" |
| Nome Prodotto | `{typography.item-name}` | Body Large, 16sp, Medium | "Latte parzialmente scremato" |
| Quantità / Badge | `{typography.item-meta}` | Label Large, 14sp, SemiBold | "2 l", "500 g", "3 pz" |
| Microcopy / Note | `{typography.caption}` | Body Small, 12sp, Regular | Dettagli marca, timestamp storico |

Nessun testo principale può scendere sotto i 14sp nella lista attiva. La leggibilità alla distanza del braccio disteso ha la priorità assoluta.

---

## 4. Layout & Spacing

Il layout è basato su un sistema a griglia modulare da **4px / 8px**:
- **Margine laterale schermo:** `{spacing.4}` (16px) standard su smartphone.
- **Distanza tra card prodotto:** `{spacing.2}` (8px) per mantenere alta densità informativa senza soffocare i target touch.
- **Target di tocco minimo:** `{spacing.touch-target-min}` (48x48dp) per ogni elemento interattivo (checkbox, pulsanti, chip).
- **Separazione categorie:** `{spacing.5}` (24px) di margine superiore prima di una nuova sezione di corsia.

---

## 5. Elevation & Depth

TOGO adotta un'elevazione minima e strutturale (**Flat + Crisp Strokes**):
- Zero ombre sfumate o gradienti decorativi su elementi della lista.
- Profondità ottenuta tramite **bordi solidi** (`1.5px solid {colors.border-crisp}`) e colori pieni.
- Modali e Bottom Sheet utilizzano un backdrop oscurato (`rgba(15, 23, 42, 0.6)`) con bordo superiore marcato da 2px e raggio `{rounded.lg}`.

---

## 6. Shapes

- **Checkbox:** Quadrato con angoli leggermente ammorbiditi (`{rounded.sm}` / 4px) per comunicare precisione e solidità.
- **Card Prodotto:** Rettangoli bordati con `{rounded.md}` (8px).
- **Badge Quantità:** Pillole compatte bordate o piene con `{rounded.sm}` (4px).
- **Pulsante Vocale (FAB):** Cerchio perfetto (`{rounded.full}` / 9999px) da 64dp, solido e inconfondibile nell'angolo inferiore destro o centrale.

---

## 7. Components

### 7.1 Riga Prodotto (`item-row`)
- **Struttura:** Flex orizzontale a 3 zone:
  - Sinistra: Checkbox touch `{components.checkbox-utility}` con padding generoso (48dp target).
  - Centro: Nome canonico del prodotto (`{typography.item-name}`) con eventuale attributo secondario sotto (`{typography.caption}`).
  - Destra: Badge quantità `{components.quantity-badge}`.
- **Stato Non Preso:** Bordo `{colors.border-crisp}`, testo `{colors.ink-primary}`, badge scuro pieno.
- **Stato Preso:** Testo barrato (`text-decoration: line-through`), opacità 45%, colore `{colors.ink-muted}`, checkbox con icona di spunta verde/nera.

### 7.2 Header di Categoria (`category-header`)
- Fascia rettangolare con bordo sinistro marcato da 4px (`{colors.border-crisp}`).
- Titolo categoria in maiuscolo compatto (`{typography.section-header}`) affiancato dal badge numerico degli articoli rimanenti (es. `ORTOFRUTTA · 2`).

### 7.3 Sezione Collassabile Presi
- Card pieghevole posta in calce alla lista attiva con titolo `Presi (N)` e icona a freccia (chevron) per espandere/collassare.
- Contiene gli articoli spuntati durante la sessione corrente.
- In fondo alla sezione espansa compare il pulsante primario largo `Concludi spesa`.

### 7.4 Bottom Sheet Vocale (`voice-sheet`)
- Pannello snello che sale dal fondo occupando circa il 45% dello schermo.
- Parte superiore: animazione d'onda o icona microfono attiva con stato `In ascolto...`.
- Parte centrale: testo trascritto in tempo reale in corsivo, seguito dalla card di scomposizione strutturata:
  - Nome: `Latte parzialmente scremato`
  - Quantità: `2 l`
  - Categoria: `Banco Frigo / Latticini` (con icona penna per modifica rapida)
- Azioni inferiori: Pulsante largo nero `{colors.accent-action}` `Conferma e aggiungi`, affiancato o sovrastante `Annulla`.

### 7.5 Dialog Risoluzione Duplicati (`duplicate-modal`)
- Finestra modale centrata ad alto contrasto.
- Titolo in grassetto: `Prodotto già in lista`.
- Testo descrittivo esplicito con quantità attuale e nuova.
- Lista verticale di 3 pulsanti:
  1. `Somma quantità (+N → Totale M)` (Primario nero in evidenza)
  2. `Sostituisci quantità (N)` (Secondario bordato)
  3. `Crea voce separata` (Secondario bordato)
  4. `Annulla` (Testuale discreto)

---

## 8. Do's and Don'ts

### Do
- Utilizzare sempre contrasto elevato (> 7:1) per testi e quantità.
- Rispettare sempre l'area di tocco minima di 48x48dp per ogni controllo interattivo.
- Mostrare la quantità e l'unità di misura in un badge chiaramente distinto dal nome del prodotto.
- Mantenere la riga del prodotto scansionabile a colpo d'occhio senza scroll orizzontali.

### Don't
- Non usare animazioni lente o transizioni decorative che rallentano l'uso ad una mano in corsia.
- Non usare font sottili (`light` o `thin`) o colori grigi chiari per testi importanti.
- Non nascondere l'unità di misura (un "2" ambiguo senza "l" o "kg" crea confusione).
- Non alterare i bordi con ombre sfumate pesanti: l'identità visiva è pulita, geometrica e solida.
