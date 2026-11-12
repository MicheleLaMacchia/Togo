---
title: '10.4 Frecce pager focusabili con TalkBack'
type: 'feature'
created: '2026-11-12'
status: 'in-progress'
baseline_commit: '63ae6f76f28b8eda317145d86e7047626383005c'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="intento esplicito della Story 10.4">

## Intent

**Problem:** Le frecce del pager non descrivono chiaramente la destinazione e lo stato di disponibilità a TalkBack; il cambio pagina non annuncia esplicitamente titolo e conteggio aggiornati.

**Approach:** Rendere le frecce controlli accessibili con etichette esplicite e stato non disponibile sulla freccia senza destinazione, mantenendola nel percorso di esplorazione TalkBack. Segnalare il titolo della pagina e il conteggio come regione live quando cambiano.

**Boundaries & Constraints**

**Always:** Conservare la navigazione a due pagine e i controlli Material esistenti; il controllo non disponibile deve rimanere raggiungibile tramite accessibilità.

**Never:** Modificare i gesti sulle righe o anticipare il comportamento Back e la persistenza delle Story 10.5 e 10.6. Non eseguire build o test.

</frozen-after-approval>

## Implementation Notes

- Riconciliazione Story 10.3: l'utente la considera completata; il commit `63ae6f7` contiene il lavoro, ma la spec precedente registra che l'ultimo tentativo si fermò in compilazione prima della correzione dell'helper. Spec e tracking sono stati allineati a `done` per indicazione esplicita; nessuna build o test viene dichiarato superato.
- Implementate etichette di destinazione `Torna a Spesa` e `Vai a Da fare`. La freccia al bordo resta nel percorso di accessibilità con semantica disabilitata e callback inerte; l'opacità ridotta conserva l'aspetto non disponibile.
- Il titolo e conteggio sono esposti come regione live polite. Il test Compose dell'intestazione copre etichette, stato ai due bordi, callback senza cambio pagina, focusability dei nodi e semantica live anche dopo il cambio pagina.
- Non sono state eseguite build né test. L'agente riferisce `git diff --check` pulito sui file applicativi; l'utente dovrà eseguire i comandi di verifica.
- Riprova utente 2026-11-12: `test-build-android.ps1` ha compilato il codice principale e assemblato l'APK, ma `compileDebugUnitTestKotlin` è fallito con quattro riferimenti irrisolti a `assert`; aggiunto l'import `androidx.compose.ui.test.assert`. L'agente non ha rilanciato build o test.

## Review Triage Log

- low — patch: Il test controllava la regione live solo sul titolo iniziale; aggiunta l'asserzione dopo il passaggio a `Da fare (3)`. Il modificatore era applicato anche al titolo aggiornato.
- low — patch: Il controllo del titolo invariato non contava gli eventi al bordo; aggiunto il conteggio e verificata l'assenza di `SelectPage` sulla freccia non disponibile. La guardia UI già bloccava l'evento.
- low — patch: La freccia non disponibile sulla pagina Da fare non aveva asserzione di callback inerte; aggiunto il controllo simmetrico. La guardia UI copre anche questo bordo.
- low — patch: Il test non interrogava esplicitamente la focusability; aggiunta l'asserzione `isFocusable()` insieme a nodo presente, click action e stato disabilitato. Il controllo Material resta attivo per il percorso accessibile.