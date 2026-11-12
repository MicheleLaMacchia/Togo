---
title: '10.1 HorizontalPager con due pagine'
type: 'feature'
created: '2026-11-12'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="intento esplicito della Story 10.1">

## Intent

**Problem:** La schermata principale deve offrire Spesa e Da fare come due pagine dello stesso pager orizzontale, con Spesa selezionata all'avvio a freddo.

**Approach:** Mantenere le pagine nell'ordine Spesa, Da fare e garantire con copertura di regressione il numero esatto di pagine e la selezione iniziale. Non estendere la navigazione oltre i criteri della Story 10.1.

</frozen-after-approval>

## Implementation Notes

- L'ispezione statica iniziale mostrava `HorizontalPager` in `ActiveListScreen`, con l'elenco enum come conteggio e `ActiveListUiState.currentPage` inizializzato a `SHOPPING`. L'implementazione rende esplicito l'ordine delle due pagine senza estendere comportamenti delle Story 10.2+.
- Il worktree era pulito prima dell'avvio. La spec 9.4 è `done` e il commit `fbaa102` documenta l'implementazione e la copertura; il tracker era rimasto `review`. Riallineare 9.4 a `done` sulla dichiarazione esplicita dell'utente e sull'evidenza versionata, senza attribuire build/test eseguiti.
- Build e test sono vietati in questa esecuzione; restituire all'utente i comandi da eseguire manualmente.
- Implementazione: introdotta `ActiveListPage.pagerPages` come elenco fisso `[SHOPPING, ACTIONS]`; `HorizontalPager` usa lo stesso elenco per conteggio, chiavi e composizione. Aggiunto `ActiveListPagerContractTest` per l'ordine/numero delle pagine e la selezione iniziale `SHOPPING`.
- Verifiche eseguite: solo ispezione statica del diff e del wiring del pager; nessuna build o test eseguiti per vincolo esplicito dell'utente.
- Correzione successiva basata sul report dell'utente: i test Compose fallivano prima del corpo del test perché mancavano `RobolectricTestRunner` e `@Config(sdk = [34])`; aggiunte le annotazioni usate dagli altri test Compose del progetto. La suite non è stata rilanciata.

## Review Triage Log

- low -- Allineato lo stato della spec al completamento dell'implementazione e alla revisione, evitando la discrepanza tra `ready-for-dev` e il tracking.
- medium -- La selezione usava `page.ordinal` mentre il rendering usava l'elenco del pager; stato iniziale e sincronizzazione ora calcolano l'indice da `pagerPages`.
- medium -- I soli test di modello non coprivano il composable; aggiunto un test Compose sul conteggio, sulla pagina iniziale e sull'ordine effettivamente renderizzato.
- medium -- I tre test Compose terminavano in `ComposeUiTest.android.kt:320` con `Build.FINGERPRINT == null`; aggiunto runner/config Robolectric per inizializzare l'ambiente Android richiesto.
