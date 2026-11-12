---
title: '10.3 Swipe solo su intestazione, gruppi e spazi vuoti'
type: 'feature'
created: '2026-11-12'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-10-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
---

<frozen-after-approval reason="intento esplicito della Story 10.3">

## Intent

**Problem:** Il cambio pagina deve coesistere con lo swipe di eliminazione delle righe senza che i due gesti si contendano il drag.

**Approach:** Abilitare lo swipe pager solo su intestazione, intestazioni di gruppo e spazi vuoti. Ogni riga deve trattenere il drag; le righe attive continuano a rivelare il cestino.

**Boundaries & Constraints**

**Always:** Conservare `HorizontalPager` con scorrimento generico disabilitato e i gestori di eliminazione delle righe attive.

**Never:** Estendere navigazione, back, persistenza o accessibilità delle frecce delle Story 10.4–10.6. Non eseguire build o test.

</frozen-after-approval>

## Implementation Notes

- La spec 10.2 è `done`, mentre il tracker la riportava `review`; l’utente ne ha confermato il completamento. Riallineare 10.2 a `done` annotando che la decisione segue l’indicazione esplicita dell’utente e non costituisce evidenza di build o test.
- L’ispezione statica ha rilevato che le righe spesa non eliminabili non consumavano il drag e potevano propagare lo swipe al pager. Ora consumano il drag; il test usa lo stesso modificatore pager per coprire header, gruppo, spazio vuoto e righe attive/non eliminabili.
- Resi `rememberPagerSwipeModifier`, `ShoppingPage` e `ActionsPage` `internal` per verificare nei test Compose il wiring effettivo delle pagine. Aggiunti test per swipe in entrambe le direzioni sull’header con controllo del pager renderizzato, intestazioni e spazi vuoti delle due pagine (incluse le liste vuote), cestino sulle righe attive anche partendo dalle checkbox o dall’area cestino, nessuna eliminazione, stabilità dell’espansione categoria, righe non eliminabili della spesa e della sezione Fatte, e scorrimento verticale della lista.
- `git diff --check` non ha segnalato errori. Su successiva autorizzazione esplicita dell'utente, `test-build-android.ps1` è stato eseguito: prima i test segnalavano tre casi di gesture, poi il tentativo più recente si è fermato per un errore Kotlin (`Restricted suspending functions`) nel helper del pass iniziale.
- Corretto il helper per usare direttamente lo scope ristretto fornito da `awaitEachGesture`. La correzione non venne compilata dopo l'ultima modifica; in quel momento la Story restava in `review` in attesa di verifica utente, stato poi riconciliato a `done` come documentato in fondo a questa spec.
- Il controllo aggiuntivo di scroll verticale su Spesa è stato rimosso perché non fa parte dei criteri 10.3 e falliva nei precedenti tentativi. Restano i controlli verticali sulla pagina Da fare.
- Worktree inizialmente pulito sul branch `dev`; preservate le modifiche introdotte per questa Story senza stage o commit.

## Review Triage Log

- medium — `assertExists` non provava che il cestino fosse rivelato; i test ora richiedono che il controllo sia mostrato e verificano lo spostamento effettivo del contenuto della riga.
- medium — Le aree di swipe usavano contenitori dimostrativi; i test ora compongono `ShoppingPage` e `ActionsPage` e usano le intestazioni reali e i rispettivi spazi liberi.
- medium — Mancava il caso della riga azione spuntata; aggiunto il test della checkbox nella sezione Fatte.
- medium — Le righe attive erano testate fuori dalle pagine; i test ora le compongono nei rispettivi contenitori di produzione.
- medium — Mancava lo swipe su intestazione categoria e spazio libero di Spesa; entrambi sono coperti e il test verifica che la sezione resti espansa.
- low — La nota della spec ometteva la copertura Fatte; aggiornata per includere righe spuntate e non eliminabili.
- medium — Mancava la direzione inversa sulle righe; i test esercitano swipe destro e sinistro senza cambio pagina o eliminazione.
- medium — L’header era testato solo da Spesa verso Da fare e senza il pager; ora verifica entrambe le direzioni con `ActiveListPager` composto.
- medium — Gli stati vuoti non erano coperti; aggiunti test per lo stato vuoto di entrambe le pagine.
- medium — I test non contavano gli eventi di eliminazione; ora verificano che lo swipe non emetta eventi delete.
- medium — La copertura di scroll verticale era limitata a Da fare; aggiunto il caso della lista Spesa.
- medium — La copertura dei gesti da checkbox mancava; i test esercitano le checkbox attive e spuntate e verificano gli eventi risultanti.
- false — Il drag della checkbox prodotto non era raggiungibile dal gestore riga: il modificatore `pointerInput` è sul `Box` esterno di `ItemRow`, antenato anche della checkbox; il test ora parte dalla checkbox.
- false — L’area cestino di `ItemRow` non aveva un gestore drag: è discendente del `Box` esterno che possiede il gestore riga; aggiunto un test sul cestino rivelato.
- medium — L’area cestino di `ActionRow` era fuori dal contenuto trascinabile e poteva propagare il drag; ora consuma il drag e il test verifica che pager ed eliminazione restino inattivi.
- medium — La suite aggiunta non includeva l’import di `swipeUp`; aggiunto l’import necessario nei test di scroll.
- medium — Le righe non eliminabili della spesa lasciavano propagare il drag; ora consumano il gesto e i test sulle checkbox confermano che il pager non parte.

- Riconciliazione 2026-11-12: su indicazione esplicita dell'utente, la Story 10.3 è stata considerata completata e lo stato della spec è stato allineato a `done`. Il commit `63ae6f7` contiene il lavoro; la nota precedente documenta che l'ultimo tentativo di compilazione si era fermato prima della correzione dell'helper. Questa riconciliazione non dichiara build o test superati.
