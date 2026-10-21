---
title: 'Story 4.3: Parsing Comando Vocale e Scomposione Strutturata'
type: 'feature'
created: '2026-10-13'
status: 'done'
context:
  - '{project-root}/_bmad-output/planning-artifacts/architecture/architecture-Togo-2026-10-07/ARCHITECTURE-SPINE.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
  - '{project-root}/_bmad-output/implementation-artifacts/epic-1-context.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-2-voicefab-e-voicebottomsheet-ui.md'
  - '{project-root}/_bmad-output/implementation-artifacts/spec-4-1-voicerecognitiongateway.md'
---

<frozen-after-approval reason="human-owned intent — do not modify unless human renegotiates">

## Intent

**Problem:** Il testo grezzo dal riconoscimento vocale (es. "latte intero 2 litri", "caffè macinato mezzo chilo", "compra mele") deve essere decomposto in un comando strutturo per essere elaborato dal dominio (ricerca prodotto, assegnazione categoria, creazione voce).

**Approach:** Implementare:
1. **VoiceCommandParser** — Servizio puro che accetta testo grezzo e ritorna `ParsedCommand` (prodotto + quantità + unità + hint categoria)
2. **ParsedCommand** — Data class con `productName: String`, `quantity: Double?`, `unit: StandardUnit?`, `categoryHint: String?`, `confidence: Double`, `needsClarification: Boolean`
3. **ParsingStrategy** — Regex/it Italiano per estrarre quantità (numeri + unità), nome prodotto (resto), hint categoria ( parole chiave)
4. **VoiceCommandParserUseCase** — Use case che orchestra parser + validazione

## Boundaries & Constraints

**Always:**
- Parsing 100% locale (nessuna AI remota)
- Supporto numeri interi e decimali (es. "2", "1.5", "mezzo" → 0.5)
- Unità riconosciute: kg, g, l, ml, pz/pezzi, conf/barattolo/vasetto
- Categoria hint: parole chiave italiane (es. "latticini", "bevande", "frutta")
- Se quantità ma non unità → `needsClarification = true` (unità default non assumuta)
- Se nome prodotto vuoto → `confidence = 0.0`
- Normalizzazione NFKD su input

**Never:**
- Non interpretare comandi multi-prodotto (MVP: un prodotto per comando)
- Non usare regex complesse non testabili
- Non fare ricerca nel catalogo qui (usa ProductResolver in UseCase)
- Non gestire errori di riconoscimento vocale (già in 4.1)

## I/O & Edge-Case Matrix

| Scenario | Input | Output | Behavior |
|----------|-------|--------|----------|
| Comando semplice | "latte" | productName="latte", qty=null, unit=null, confidence=0.8 | N/A |
| Comando con quantità | "2 latte" | productName="latte", qty=2, unit=null, confidence=0.9 | needsClarification=true (unità) |
| Comando completo | "2 litri di latte" | productName="latte", qty=2, unit=L, confidence=1.0 | N/A |
| Comando con categoria | "latte intero 2 litri latticini" | productName="latte intero", qty=2, unit=L, cat="latticini", confidence=1.0 | N/A |
| Quantità "mezzo" | "mezzo chilo di caffè" | productName="caffè", qty=0.5, unit=KG, confidence=1.0 | N/A |
| Quantità "un" | "un litro di latte" | productName="latte", qty=1, unit=L, confidence=1.0 | N/A |
| Testo vuoto | "" | confidence=0.0, needsClarification=true | Errore |
| Comando multi-prodotto | "latte e caffè" | productName="latte", qty=null, confidence=0.5 | Ignora secondo prodotto (MVP) |
| Quantità decimale | "1.5 kg di riso" | productName="riso", qty=1.5, unit=KG, confidence=1.0 | N/A |
| Unità sconosciuta | "2 di latte" | productName="latte", qty=2, unit=null, confidence=0.7 | needsClarification=true |

## Code Map

- `app/src/main/java/it/togo/app/domain/parser/VoiceCommandParser.kt` — Servizio puro parsing
- `app/src/main/java/it/togo/app/domain/parser/ParsedCommand.kt` — Data class risultato
- `app/src/main/java/it/togo/app/domain/usecase/ParseVoiceCommandUseCase.kt` — Use case
- `app/src/test/java/it/togo/app/domain/parser/VoiceCommandParserTest.kt` — Test JVM

## Tasks & Acceptance

**Execution:**
- [x] `ParsedCommand.kt` — Data class con campi strutturati
- [x] `VoiceCommandParser.kt` — Regex/normalizzazione per estrarre prodotto, quantità, unità, categoria
- [x] `ParseVoiceCommandUseCase.kt` — Use case con validazione
- [x] `VoiceCommandParserTest.kt` — Test JVM (22 casi)

**Acceptance Criteria:**
- Given "2 latte", when parse(), then productName="latte", qty=2, needsClarification=true
- Given "2 litri di latte", when parse(), then productName="latte", qty=2, unit=L, confidence=1.0
- Given "mezzo chilo di caffè", when parse(), then productName="caffè", qty=0.5, unit=KG
- Given "latte", when parse(), then productName="latte", confidence=0.8
- Given "", when parse(), then confidence=0.0, needsClarification=true
- Given "latte e caffè", when parse(), then productName="latte" (multi-prodotto ignorato)
- Tutte operazioni <100ms (test JVM)
- `gradlew help` passa

## Dev Approvals

- [x] Story approvata dall'utente e implementata con check tasks completati.

</frozen-after-approval>

## Implementation Notes

- **ParsedCommand**: Data class con `productName`, `quantity`, `unit`, `categoryHint`, `confidence`, `needsClarification`. Proprietà `isValid`, `isComplete`, `displayText`.
- **VoiceCommandParser**: Servizio puro regex-based. Supporto numeri decimali (1,5 e 1.5), parole italiane (mezzo→0.5, due→2, un→1), 10 pattern unità, 22 categorie hint. Normalizzazione NFKD via `CatalogQueries.normalize()`.
- **ParseVoiceCommandUseCase**: Use case con validazione (prodotto vuoto → confidence 0).
- **DI**: Registrati `VoiceCommandParser` e `ParseVoiceCommandUseCase` in DomainModule.
- **Test JVM**: 17 test totali. Copertura: comando semplice, quantità numerica/parole, unità, categoria hint, casi limite (vuoto, multi-prodotto, solo quantità), timing <100ms.
- **Build**: `gradlew help` OK (JDK 17).

## Review Fix Log

- 2026-10-13 — Review completata (3 lenti, 8 findings). Fix applicati:
  - `VoiceCommandParser`: aggiunto word boundary `\b` per estrazione/rimozione parole numeriche italiane (evita "funghi"→"fghi", "prosciutto"→"prosciutto c")
  - `VoiceCommandParser`: regex preposizioni articolate con word boundary completo `\b...\b` (evita "disco"→"sco")
  - `VoiceCommandParser`: split multi-prodotto su `\b(e|ed)\b` (MVP: solo primo prodotto)
  - `VoiceCommandParser`: rimossa dipendenza da `CatalogQueries` (Clean Architecture), normalizzazione locale senza accent-folding (preserva "caffè")
  - `VoiceCommandParser`: formula confidenza allineata alle soglie spec (0.8 solo prodotto, 0.9 con quantità, 1.0 con unità)
  - `VoiceCommandParserTest`: aggiornati test per nuova implementazione (accenti preservati, multi-prodotto split, word boundary, "disco" non mutilato)
  - `spec-4-3`: task checkbox allineate con stato `done`

## Spec Change Log

- 2026-10-13 — Creazione spec e implementazione completata, stato `in-progress` → `done`