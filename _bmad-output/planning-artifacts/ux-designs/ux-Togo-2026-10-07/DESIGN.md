---
name: Togo
description: Local-first Android shopping list app. High-Contrast Utility, minimal, fast in-store scanning, touch-first.
status: final
created: 2026-10-07
updated: 2026-11-05
sources:
  - _bmad-output/specs/spec-cose-da-fare/SPEC.md
  - _bmad-output/specs/spec-cose-da-fare/brownfield.md
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
  urgency-urgent-bg: '#FEF3C7'
  urgency-urgent-ink: '#92400E'
  urgency-urgent-border: '#B45309'
  urgency-relaxed-bg: '#F1F5F9'
  urgency-relaxed-ink: '#334155'
  urgency-relaxed-border: '#CBD5E1'
  urgency-exceptional-bg: '#DBEAFE'
  urgency-exceptional-ink: '#1E40AF'
  urgency-exceptional-border: '#2563EB'
  badge-bg: '#0F172A'
  badge-ink: '#FFFFFF'
  surface-subtle-dark: '#131B2E'
  surface-inverse-dark: '#0B0F17'
  ink-muted-dark: '#CBD5E1'
  ink-inverse-dark: '#0F172A'
  border-hairline-dark: '#1F2937'
  accent-action-dark: '#0F172A'
  accent-success-dark: '#15803D'
  accent-warning-dark: '#B45309'
  badge-bg-dark: '#0F172A'
  badge-ink-dark: '#FFFFFF'
  ink-struck: '#1F2937'
  ink-struck-dark: '#CBD5E1'
  focus-ring: '#2563EB'
  focus-ring-dark: '#60A5FA'
  surface-base-dark: '#0B0F17'
  surface-card-dark: '#131B2E'
  ink-primary-dark: '#F8FAFC'
  ink-secondary-dark: '#94A3B8'
  border-crisp-dark: '#64748B'
  accent-highlight-dark: '#60A5FA'
  urgency-urgent-bg-dark: '#422006'
  urgency-urgent-ink-dark: '#FDE68A'
  urgency-urgent-border-dark: '#B45309'
  urgency-relaxed-bg-dark: '#1E293B'
  urgency-relaxed-ink-dark: '#CBD5E1'
  urgency-relaxed-border-dark: '#475569'
  urgency-exceptional-bg-dark: '#172554'
  urgency-exceptional-ink-dark: '#BFDBFE'
  urgency-exceptional-border-dark: '#2563EB'
typography:
  title-screen:
    note: 'Android Headline Medium · Roboto 24sp SemiBold · Line Height 32sp'
  page-title:
    note: 'Android Title Large · Roboto 20sp Bold · Line Height 28sp'
  section-header:
    note: 'Android Title Medium · Roboto 16sp Bold · Line Height 24sp · Letter Spacing 0.15sp'
  item-name:
    note: 'Android Body Large · Roboto 16sp Medium · Line Height 24sp'
  item-meta:
    note: 'Android Label Large · Roboto 14sp SemiBold · Line Height 20sp'
  badge-label:
    note: 'Android Label Small · Roboto 12sp Bold · Line Height 16sp · Letter Spacing 0.3sp'
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
  page-header:
    bg: '{colors.surface-base}'
    border-bottom: '2px solid {colors.border-crisp}'
    height: '64px'
    title: '{typography.page-title}'
  pager-arrow:
    size: '48px'
    radius: '{rounded.md}'
    color: '{colors.ink-primary}'
    disabled: '{colors.ink-muted}'
  action-row:
    min-height: '{spacing.touch-target-min}'
    border: '1.5px solid {colors.border-crisp}'
    radius: '{rounded.md}'
    bg: '{colors.surface-card}'
    padding: '{spacing.3} {spacing.4}'
    accent-width: '4px'
  urgency-badge:
    radius: '{rounded.sm}'
    padding: '{spacing.1} {spacing.2}'
    text: '{typography.badge-label}'
  area-badge:
    bg: '{colors.surface-subtle}'
    text-color: '{colors.ink-secondary}'
    border: '1px solid {colors.border-hairline}'
    radius: '{rounded.sm}'
    padding: '{spacing.1} {spacing.2}'
  action-sheet:
    bg: '{colors.surface-base}'
    border-top: '2px solid {colors.border-crisp}'
    radius: '{rounded.lg} {rounded.lg} 0 0'
  choice-dialog:
    bg: '{colors.surface-base}'
    border: '2px solid {colors.border-crisp}'
    radius: '{rounded.lg}'
---

# TOGO — Design Spine

> Riferimento visivo ufficiale (Google Labs DESIGN.md spec). Definisce lo stile, i token visivi e le regole di rendering per l'applicazione Android TOGO.  
> Spines win on conflict: questo documento e `EXPERIENCE.md` vincono su qualunque wireframe, prototipo o mockup.
> Mockup di riferimento per la composizione: [`mockups/active-list.html`](mockups/active-list.html), [`mockups/todo-page.html`](mockups/todo-page.html), [`mockups/action-sheet.html`](mockups/action-sheet.html), [`mockups/share-dialog.html`](mockups/share-dialog.html), [`mockups/conclude-dialog.html`](mockups/conclude-dialog.html), [`mockups/voice-sheet.html`](mockups/voice-sheet.html), [`mockups/duplicate-dialog.html`](mockups/duplicate-dialog.html), [`mockups/history.html`](mockups/history.html).

---

## 1. Brand & Style

TOGO è un'applicazione Android local-first progettata come uno strumento di lavoro rapido e affidabile per la spesa quotidiana. Rifiuta l'estetica decorativa, le sfumature pastello e le animazioni leziose in favore della **High-Contrast Utility**:
- **Chiarezza istantanea:** testi neri nitidi su sfondo bianco puro, contorni netti da 1.5px e 2px per separare gli elementi con rigore geometrico.
- **Resistenza all'ambiente:** leggibilità impeccabile sotto la luce al neon fredda del supermercato o sotto la luce solare diretta.
- **Ergonomia ad una mano:** target touch ampi (minimo `{spacing.touch-target-min}`), contrasto massimo tra elementi da prendere ed elementi presi, badge quantità evidenti e scansionabili in frazioni di secondo mentre si spinge il carrello.

La superficie principale è un **pager a due pagine**: **Spesa** a sinistra e **Da fare** a destra. Le due pagine condividono la stessa intestazione, gli stessi bordi netti e la stessa gerarchia tipografica. Le azioni non sono prodotti: non hanno quantità, unità, marca o variante e non usano il badge quantità. La distinzione visiva è affidata a un accento verticale e a due badge leggibili, sempre accompagnati da testo.

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
- `{colors.urgency-urgent-bg}` / `{colors.urgency-urgent-ink}` / `{colors.urgency-urgent-border}`: ambra per l'urgenza **Urgente**. Testo scuro su fondo chiaro per mantenere il contrasto. Rapporto contrasto 6,37:1 (target AA 4,5:1 per testi di badge; il floor 7:1 si applica ai testi primari e di corpo dichiarati in §8).
- `{colors.urgency-relaxed-bg}` / `{colors.urgency-relaxed-ink}` / `{colors.urgency-relaxed-border}`: neutro per **A tempo perso**.
- `{colors.urgency-exceptional-bg}` / `{colors.urgency-exceptional-ink}` / `{colors.urgency-exceptional-border}`: blu per **Eccezionale**.
- I colori di urgenza non sono mai l'unico segnale: il badge contiene sempre l'etichetta scritta. In dark mode usano le varianti `*-dark` definite nei token.

Supporto Dark Mode:
In modalità scura (`{colors.surface-base-dark}` `#0B0F17`), i testi passano a `{colors.ink-primary-dark}` (`#F8FAFC`) con bordi `{colors.border-crisp-dark}` (`#64748B`) e i componenti usano le rispettive varianti `*-dark` (`surface-subtle-dark`, `ink-muted-dark`, `badge-bg-dark`, `accent-action-dark`, ecc.) per preservare il contrasto e non abbagliare al buio. Il valore di `{colors.border-crisp-dark}` è `#64748B` sia nel frontmatter che in questo paragrafo.

---

## 3. Typography

TOGO eredita la ramp tipografica nativa di **Android (Material 3 / Jetpack Compose)** basata su Roboto / Google Sans:

| Ruolo | Token | Spec nativa Android | Uso |
|---|---|---|---|
| Titolo Schermata | `{typography.title-screen}` | Headline Medium, 24sp, SemiBold | Header "Storico prodotti", intestazioni di schermate secondarie |
| Titolo Pagina | `{typography.page-title}` | Title Large, 20sp, Bold | "Spesa (N)", "Da fare (N)" nell'intestazione del pager |
| Intestazione Categoria | `{typography.section-header}` | Title Medium, 16sp, Bold, uppercase tracking | "ORTOFRUTTA", "BANCO FRIGO" |
| Nome Prodotto / Azione | `{typography.item-name}` | Body Large, 16sp, Medium | "Latte parzialmente scremato", "Chiamare il dentista" |
| Quantità / Badge | `{typography.item-meta}` | Label Large, 14sp, SemiBold | "2 l", "500 g", "3 pz" |
| Etichetta Badge | `{typography.badge-label}` | Label Small, 12sp, Bold, tracking 0.3sp | "URGENTE", "SALUTE", "A TEMPO PERSO" |
| Microcopy / Note | `{typography.caption}` | Body Small, 12sp, Regular | Dettagli marca, timestamp storico |

Nessun testo principale può scendere sotto i 14sp nella lista attiva. La leggibilità alla distanza del braccio disteso ha la priorità assoluta.

---

## 4. Layout & Spacing

Il layout è basato su un sistema a griglia modulare da **4px / 8px**:
- **Margine laterale schermo:** `{spacing.4}` (16px) standard su smartphone.
- **Distanza tra card prodotto:** `{spacing.2}` (8px) per mantenere alta densità informativa senza soffocare i target touch.
- **Target di tocco minimo:** `{spacing.touch-target-min}` (48x48dp) per ogni elemento interattivo (checkbox, pulsanti, chip).
- **Separazione categorie:** `{spacing.5}` (24px) di margine superiore prima di una nuova sezione di corsia.
- **Intestazione del pager:** fascia fissa da 64px con bordo inferiore da 2px. Titolo e frecce stanno sulla stessa riga; le azioni globali restano a destra. A font grande l'intestazione può crescere e il titolo può andare a capo.
- **Righe azione:** stessa densità delle righe prodotto, con due badge sotto il testo e un accento verticale da 4px. La riga cresce in altezza con Dynamic Type senza troncare il testo.

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
- **Badge Urgenza e Ambito:** Rettangoli compatti con `{rounded.sm}` (4px), testo mai sotto 12sp. L'urgenza è piena con il colore semantico; l'ambito è neutro con bordo hairline.
- **Riga Azione:** Card rettangolare con `{rounded.md}` (8px) e accento verticale interno da 4px sul lato sinistro.
- **Foglio Azione:** Bottom sheet con angoli superiori `{rounded.lg}` (12px) e bordo superiore da 2px.
- **Pulsante Vocale (FAB):** Cerchio perfetto (`{rounded.full}` / 9999px) da 64dp, solido e inconfondibile nell'angolo inferiore destro o centrale. Sulla pagina Da fare resta solo il FAB (+) contestuale.

---

## 7. Components

### 7.1 Riga Prodotto (`item-row`)
- **Struttura:** Flex orizzontale a 3 zone:
  - Sinistra: Checkbox touch `{components.checkbox-utility}` con padding generoso (48dp target).
  - Centro: Nome canonico del prodotto (`{typography.item-name}`) con eventuale attributo secondario sotto (`{typography.caption}`).
  - Destra: Badge quantità `{components.quantity-badge}`.
- **Stato Non Preso:** Bordo `{colors.border-crisp}`, testo `{colors.ink-primary}`, badge scuro pieno.
- **Stato Preso:** Testo barrato (`text-decoration: line-through`), colore `{colors.ink-struck}`, checkbox con icona di spunta `{colors.ink-inverse}` su `{colors.accent-action}`. Nessuna opacità è applicata al testo. `{colors.ink-muted}` è riservato ai soli componenti disabilitati.

### 7.2 Header di Categoria (`category-header`)
- Fascia rettangolare con bordo sinistro marcato da 4px (`{colors.border-crisp}`).
- Titolo categoria in maiuscolo compatto (`{typography.section-header}`) affiancato dal badge numerico degli articoli rimanenti (es. `ORTOFRUTTA · 2`).
- Comportamento: statico (nessun tap); sulle intestazioni di gruppo del pager è anche zona di swipe orizzontale per cambiare pagina; il conteggio mostra le voci non spuntate del gruppo.

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
- Lista verticale di 4 pulsanti:
  1. `Somma quantità (+N → Totale M)` (Primario nero in evidenza)
  2. `Sostituisci quantità (N)` (Secondario bordato)
  3. `Crea voce separata` (Secondario bordato)
  4. `Annulla` (Testuale discreto)

### 7.6 Intestazione di Pagina e Pager (`page-header`)
- Fascia fissa sopra la lista con bordo inferiore da 2px (`{components.page-header}`).
- **Sinistra:** icona Storico solo sulla pagina Spesa.
- **Centro:** freccia sinistra, titolo `Spesa (N)` o `Da fare (N)`, freccia destra. Le frecce sono target da 48dp (`{components.pager-arrow}`); quella senza pagina disponibile è disabilitata e resta annunciata come non disponibile.
- **Destra:** Condividi e Impostazioni su entrambe le pagine.
- La fascia è la principale zona di swipe orizzontale. Le righe della lista non cambiano il proprio gesto: lo swipe a sinistra continua a rivelare il cestino.
- Il titolo usa `{typography.page-title}` ed è un heading per TalkBack. A Dynamic Type elevato l'intestazione cresce e il titolo può andare a capo.

### 7.7 Riga Azione (`action-row`)
- **Struttura:** checkbox a sinistra (`{components.checkbox-utility}`), testo al centro (`{typography.item-name}`), nessuna quantità a destra.
- **Meta:** sotto il testo, una riga con badge urgenza (`{components.urgency-badge}`) e badge ambito (`{components.area-badge}`). I badge sono sempre leggibili come testo e non dipendono solo dal colore.
- **Accento:** barra verticale interna da 4px sul lato sinistro, con il colore dell'urgenza. È discreta e non sostituisce il badge.
- **Stato attivo:** bordo `{colors.border-crisp}`, testo `{colors.ink-primary}`, tap sulla riga → modifica, swipe a sinistra → cestino.
- **Stato Fatte:** testo barrato, badge attenuati, checkbox spuntata. Niente tap-edit e niente cestino; la checkbox deseleziona.
- **Divieti visivi:** nessun badge quantità, nessuna marca, variante o unità.

### 7.8 Foglio Azione (`action-sheet`)
- Bottom sheet (`{components.action-sheet}`) con titolo `Aggiungi cosa da fare` o `Modifica cosa da fare`.
- Campo testo a una riga, autofocus, placeholder `Es. Chiamare il dentista`.
- Selettore urgenza a tre chip: `Urgente`, `A tempo perso` (default), `Eccezionale`.
- Selettore ambito a cinque chip: `Casa`, `Pratiche`, `Salute`, `Lavoro`, `Altro` (default).
- Pulsante primario nero `Aggiungi` / `Salva`; secondario `Annulla`. Il primario è disabilitato se il testo è vuoto.
- Errore inline `Azione già presente` se testo, urgenza e ambito coincidono con un'altra azione; il foglio resta aperto.

### 7.9 Sezione Fatte (`fatte-section`)
- Come `Presi`: header collassabile `Fatte (N)`, default espansa se N > 0, chevron di espansione.
- Righe con checkbox spuntata, testo barrato e badge attenuati. Nessun tap-edit e nessun cestino; la checkbox deseleziona.
- Ordine dalla più recente in cima.
- In fondo, pulsante largo `Concludi attività` (`{colors.accent-action}`) visibile solo se N > 0.
- La sezione non ha un'azione di annulla dopo la conclusione.

### 7.10 Dialogo Concludi Attività (`conclude-dialog`)
- Modale centrata ad alto contrasto, stesso linguaggio del dialog duplicati.
- Titolo `Concludere le attività?`; corpo `Verranno eliminate N attività spuntate. L'operazione non si può annullare.`
- Primario nero `Concludi attività`; secondario testuale `Annulla`.
- Nessun undo e nessuno storico delle azioni concluse.

### 7.11 Dialogo Condivisione (`share-dialog`)
- Modale (`{components.choice-dialog}`) con tre scelte grandi, ciascuna con conteggio: `Solo spesa (N)`, `Solo azioni (M)`, `Entrambe (N+M)`.
- Le scelte con la parte corrispondente vuota sono disabilitate con nota `Nessuna voce`; se entrambe sono vuote l'icona Condividi mostra `Non c'è nulla da condividere` e non apre il dialogo.
- Default: la scelta corrispondente alla pagina corrente.
- Primario nero `Condividi`; secondario `Annulla`.

### 7.12 Import con Azioni
- La schermata `Importa lista` mostra anche il blocco `[DA FARE]` nell'esempio di formato.
- La conferma e l'esito riportano prodotti, azioni, duplicati saltati e categorie/prodotti creati.
- Il blocco `[DA FARE]` non crea prodotti o categorie e non usa il badge quantità.

### 7.13 Mapping nomi componenti
| Componente | Token DESIGN | Pattern EXPERIENCE | Mockup |
|---|---|---|---|
| Riga Prodotto | item-row | §4.1 | active-list.html |
| Riga Azione | action-row | §4.7 | todo-page.html |
| Header di Categoria | category-header | §7.2 | todo-page.html |
| Foglio Azione | action-sheet | §4.9 | action-sheet.html |
| Bottom Sheet Vocale | voice-sheet | §4.4 | voice-sheet.html |
| Dialog Duplicati | duplicate-dialog | §4.5 | duplicate-dialog.html |
| Dialog Concludi Attività | conclude-dialog | §4.8 / §7.10 | conclude-dialog.html |
| Dialog Condivisione | share-dialog | §4.10 / §7.11 | share-dialog.html |
| FAB Vocale | voice-fab | §4.4 | — |
| FAB Azione (+) | fab-action | §4.6 | todo-page.html |

---

## 8. Do's and Don'ts

### Do
- Utilizzare sempre contrasto elevato (> 7:1) per testi e quantità.
- Rispettare sempre l'area di tocco minima di 48x48dp per ogni controllo interattivo.
- Mostrare la quantità e l'unità di misura in un badge chiaramente distinto dal nome del prodotto.
- Mantenere la riga del prodotto scansionabile a colpo d'occhio senza scroll orizzontali.
- Mostrare sempre urgenza e ambito delle azioni con testo leggibile; il colore è un rinforzo, non l'unico segnale.
- Tenere il titolo della pagina e le frecce visibili sopra la lista; la zona di swipe del pager non deve mai coprire le righe.
- Lasciare che la riga azione cresca in altezza con Dynamic Type invece di troncare il testo.

### Don't
- Non usare animazioni lente o transizioni decorative che rallentano l'uso ad una mano in corsia.
- Non usare font sottili (`light` o `thin`) o colori grigi chiari per testi importanti.
- Non nascondere l'unità di misura (un "2" ambiguo senza "l" o "kg" crea confusione).
- Non alterare i bordi con ombre sfumate pesanti: l'identità visiva è pulita, geometrica e solida.
- Non usare badge quantità, marca o variante per le azioni: non sono prodotti.
- Non rendere l'intera schermata un pager gestuale: lo swipe orizzontale sulle righe è il cestino.
- Non affidare l'urgenza solo al colore: chi non distingue i colori deve poterla leggere.
- Non nascondere il titolo della pagina dietro un menu o una tab bar inferiore.
