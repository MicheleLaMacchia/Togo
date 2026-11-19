---
title: 'Radio azione e svuotamento testo import'
type: 'bugfix'
created: '2026-11-19'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context: []
---

<frozen-after-approval reason="Richieste esplicite dell'utente">

## Intent

**Problem:** Nella sheet "Nuova azione" i radio button sono disposti verticalmente e la selezione non si distingue bene dallo sfondo. In "Importa lista" manca un comando per cancellare il testo inserito.

**Approach:** Disporre le opzioni radio in orizzontale, consentendone il ritorno a capo quando serve, e applicare colori selezionati con contrasto leggibile. Aggiungere "Svuota" per cancellare il testo della lista incollato.

**Boundaries & Constraints**

**Always:** Conservare selezione esclusiva, etichette e accessibilità dei gruppi radio; il pulsante svuota il solo testo di importazione e resta disabilitato durante analisi/importazione.

**Never:** Non modificare la lista salvata dell'utente né importare contenuti senza conferma. Non eseguire build o test automatici; l'utente li esegue.

</frozen-after-approval>

## Implementation Notes

- I radio dei selettori Urgenza e Ambito sono in `ActionSheet.kt`; il colore del testo selezionato usa attualmente `inkInverse` e il layout crea una riga per opzione.
- `ImportListScreen.kt` mostra `uiState.text`; `ImportListViewModel.kt` gestisce cambi testo e stati di analisi/importazione. Il comando deve azzerare quel campo e gli eventuali errori/anteprime pendenti senza toccare dati persistiti.
- Implementati FlowRow, colori radio espliciti e testo sempre leggibile in `ActionSheet.kt`; aggiunto `clearText()` alla ViewModel e il pulsante disabilitato a testo vuoto o durante analisi/importazione in `ImportListScreen.kt`.
- Aggiunti test Compose di disposizione orizzontale per entrambi i gruppi e test ViewModel per lo svuotamento. Nessuna build o suite è stata eseguita, come richiesto.

## Review Triage Log

- low, corretto: una nuova asserzione Kotlin `assert` poteva essere disattivata; sostituita con `assertTrue` JUnit e aggiunto il controllo orizzontale per Urgenza.
- low, corretto: il nome del test di svuotamento prometteva verifica di errore e anteprima non preparati; rinominato per descrivere il caso effettivo.
- low, respinto: un test addizionale di wrapping a larghezza stretta avrebbe coperto il comportamento nativo di `FlowRow`; i gruppi usano già il layout adattivo e i test verificano la disposizione orizzontale nel caso standard.
- low, respinto: test separati per la guardia durante analisi/importazione e per lo stato enabled della schermata aggiungerebbero setup coroutine/UI; la guardia è esplicita in `clearText()` e l'enabled è legato direttamente al testo e ai due stati occupati.
