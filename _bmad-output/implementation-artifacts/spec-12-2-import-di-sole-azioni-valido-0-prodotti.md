---
title: '12.2 Import di sole azioni valido (0 prodotti)'
type: 'feature'
created: '2026-11-16'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-12-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="Story 12.2 richiesta dall'utente">

## Intent

**Problem:** L'anteprima rifiuta un blocco `[DA FARE]` valido quando non contiene righe prodotto, impedendo di confermare un import composto solo da azioni.

**Approach:** Rappresentare le azioni nel piano d'importazione, mostrare il numero di prodotti e azioni nella conferma e consentire la conferma di un piano con zero prodotti. La conferma salva le azioni come azioni, senza creare prodotti o categorie.

</frozen-after-approval>

## Implementation Notes

- Il conteggio azioni corrisponde alle righe descrittive indentate del blocco esportato; scope e urgenza usano le conversioni esistenti dei relativi enum. Deduplica e nuove regole di validazione restano alle Story 12.3 e successive.
- Il piano conserva le azioni separatamente dagli elementi prodotto. La conferma le inserisce tramite `ShoppingListRepository.addAction`; i cicli di creazione catalogo restano limitati agli elementi prodotto.
- Discrepanza 12.1: la spec aveva già `status: done`, mentre `sprint-status.yaml` indicava `review`. Su dichiarazione esplicita dell'utente ho allineato il tracking a `done`; la nota precisa che build e test non sono stati eseguiti, quindi non attribuisce loro esito positivo.
- File modificati: use case e schermata/modello di stato dell'import, test del use case, tracking sprint per la riconciliazione 12.1 e questa spec. Il test aggiunto non è stato eseguito per vincolo utente.
- Revisione statica separata: corretti i quattro rilievi su letture inutili del catalogo nel percorso solo azioni, singolare dei conteggi, messaggio di esito e nota sui prodotti presenti. `git diff --check` è stato eseguito; build e test restano da eseguire dall'utente.

## Review Triage Log

- patch — il percorso solo azioni non interroga più tassonomia, marche o varianti; in precedenza queste letture non erano necessarie e potevano bloccare l'anteprima.
- patch — l'anteprima compone il singolare del prodotto nei piani misti; prima mostrava "1 prodotti".
- patch — il messaggio di esito conserva "Importato 1 prodotto" per il caso prodotto singolo senza azioni.
- patch — l'avviso sull'aggiornamento dei prodotti presenti appare solo quando il piano contiene prodotti.
