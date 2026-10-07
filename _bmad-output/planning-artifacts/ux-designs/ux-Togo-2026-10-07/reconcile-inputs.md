# Riconciliazione Input — TOGO UX

Riconciliazione dei requisiti provenienti da `prd.md` e `brief.md` con le decisioni catturate nelle spine `DESIGN.md` ed `EXPERIENCE.md`.

## 1. Copertura Requisiti PRD

| Requisito PRD | Trattamento UX | Destinazione / Stato |
|---|---|---|
| **Piattaforma Android Local-First** | Confermato: smartphone Android portrait, offline Room/SQLite, no auth. | `EXPERIENCE.md §1` |
| **Stile Minimalista & UI Semplice** | Confermato: Direzione C "High-Contrast Utility" (bordi netti, testi ad alto contrasto, zero fronzoli). | `DESIGN.md §1, §2` |
| **Cattura Vocale a singolo prodotto** | Confermato: Bottom Sheet snello, trascrizione live, scomposizione strutturata, pulsante esplicito di conferma. | `EXPERIENCE.md §4.4`, `mockups/voice-sheet.html` |
| **Depennamento & Sezione Presi** | Confermato: Opzione B (articoli barrati scivolano nella sezione pieghevole "Presi (N)"). | `EXPERIENCE.md §4.2`, `mockups/active-list.html` |
| **Flusso "Concludi spesa"** | Confermato: Archiviazione permanente dei soli articoli barrati nello storico; gli articoli non presi rimangono per la prossima spesa. | `EXPERIENCE.md §4.3` |
| **Risoluzione Duplicati** | Confermato: Modal a 3 scelte esplicite con "Somma quantità" come azione raccomandata primaria in evidenza. | `EXPERIENCE.md §4.5`, `mockups/duplicate-dialog.html` |
| **Storico Prodotti & Riaggiunta** | Confermato: Schermata dedicata accessibile da icona App Bar con ricerca e pulsante rapido "+ Riaggiungi". | `EXPERIENCE.md §2.1, §8 (UJ-5)`, `mockups/history.html` |
| **Navigazione Ergonomica** | Confermato: Schermata unica a tutto spazio (no bottom tab bar), icone fisse in App Bar. | `EXPERIENCE.md §2.2` |
| **Feedback Aptico** | Confermato: Vibrazione secca su check articolo e su conferma vocale. | `EXPERIENCE.md §6` |
| **Condivisione Lista** | Confermato: Testo formattato solo con articoli attivi raggruppati per corsia via Android Sharesheet. | `EXPERIENCE.md §8 (UJ-3)` |

## 2. Decisioni Qualitative Riconciliate
Tutte le direttive fornite durante la sessione interattiva con Mike sono state incorporate senza omissioni o conflitti con i documenti a monte.
