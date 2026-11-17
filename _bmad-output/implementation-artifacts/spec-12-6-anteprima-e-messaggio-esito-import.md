---
title: '12.6 Anteprima e messaggio esito import'
type: 'feature'
created: '2026-11-17'
status: 'review'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-12-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Intent e criteri forniti dall'utente">

## Intent

**Problem:** La conferma dell'import non espone tutti i conteggi utili e il risultato non riporta le creazioni o le azioni duplicate saltate. L'anteprima deve inoltre essere riconoscibile e annunciata dalla tecnologia assistiva.

**Approach:** Completare il piano di anteprima con i duplicati di azioni saltati, mostrare in una regione accessibile conteggi e creazioni previste e usare la stessa quantità nel pulsante di conferma. Dopo l'import mostrare prodotti e azioni importati, duplicati effettivamente saltati e categorie/prodotti realmente creati.

**Always:** Conservare parsing, validazione e import atomico esistenti; il conteggio effettivo dopo conferma riflette le scritture concluse e i duplicati ricontrollati durante l'esecuzione.

**Never:** Eseguire build o test; stage o commit; modificare file non pertinenti alla Story 12.6; introdurre migrazioni o modificare il comportamento delle storie precedenti.

</frozen-after-approval>

## Implementation Notes

- `ImportPlan` già espone prodotti/azioni e categorie/prodotti nuovi previsti; completare il conteggio delle azioni duplicate (già presenti o ripetute nel testo) durante l'anteprima. In esecuzione restituire il numero realmente saltato, incluso un duplicato introdotto tra anteprima e conferma.
- La schermata usa già un `AlertDialog` e uno snackbar. Rendere il contenuto dell'anteprima una regione semantica con heading annunciato, aggiungervi tutti i conteggi e creazioni previste, ed etichettare la conferma «Importa N prodotti e M azioni» con plurali corretti.
- Il messaggio di esito riporta prodotti/azioni importati, duplicati saltati, categorie e prodotti creati (incluse quantità zero). Aggiungere copertura mirata al caso d'uso e alla presentazione dove il repository offre fixture adatte; non eseguire alcun test/build per vincolo dell'utente.
- Discrepanza di tracking precedente: la spec 12.5 è `done`, mentre `sprint-status.yaml` la segna `review`. L'utente dichiara la storia completata; trattarla come completata per continuità, documentando la riconciliazione amministrativa senza attestare verifiche tecniche non eseguite.
- Implementazione completata in `ImportShoppingListUseCase.kt`, `ImportListViewModel.kt` e `ImportListScreen.kt`; i test del caso d'uso e il nuovo `ImportPreviewDialogTest.kt` coprono duplicati, semantica dell'anteprima, label e riepilogo. La conferma riporta stime; l'esito conta le azioni duplicate già escluse dall'anteprima più quelle trovate al controllo transazionale, e le creazioni effettive.
- Tracking riconciliato: 12.5 impostata `done` su dichiarazione utente (la spec era già done); 12.6 impostata `review`. Commenti in sprint status specificano che la riconciliazione 12.5 non certifica build/test e che per 12.6 i test restano da eseguire.
- Verifica statica: `git diff --check` eseguito senza errori; esaminati diff e status. Nessuna build o test è stata eseguita per vincolo esplicito.
- Evidenza fornita dall'utente dopo l'implementazione: `test-build-android.ps1` ha superato la compilazione Kotlin dell'app, compilato i test e l'APK, ma un test Compose è fallito perché cercava genericamente `PaneTitle`; il report mostrava due nodi, il pane `Dialog` e quello dell'anteprima. Il test ora cerca esattamente il valore `Anteprima importazione`; la schermata mantiene quel `paneTitle` sulla regione e il heading nello slot `title` dell'`AlertDialog`. Correzione in attesa di nuova esecuzione dell'utente; non verificata qui.

## Review Triage Log

- medium — patch: il nuovo riepilogo aveva omesso il conteggio delle voci aggiornate già mostrato prima; aggiunto `Voci aggiornate` insieme ai conteggi richiesti.
- medium — false: il conteggio duplicati è il totale delle azioni escluse dal testo importato durante anteprima e/o controllo in transazione; un'azione esclusa resta non importata anche se lo stato cambia prima della conferma. Il nome UI specifica che si tratta di azioni duplicate.
- medium — patch: la label «Duplicati saltati» era ambigua; anteprima ed esito ora dicono «Azioni duplicate saltate».
- medium — patch: mancava copertura di presentazione; aggiunto `ImportPreviewDialogTest` per i conteggi, heading/region, label della conferma, callback e testo del riepilogo. Il test non è stato eseguito per vincolo utente.
