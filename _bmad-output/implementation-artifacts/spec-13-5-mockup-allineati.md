---
title: '13.5 Mockup allineati'
type: 'feature'
created: '2026-11-19'
status: 'done'
route: 'oneshot'
review_loop_iteration: 0
baseline_commit: 'be43403e6c33706145ef0b208a1914d27470e83f'
context:
  - '{project-root}/_bmad-output/implementation-artifacts/epic-13-context.md'
  - '{project-root}/_bmad-output/planning-artifacts/epics.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/DESIGN.md'
  - '{project-root}/_bmad-output/planning-artifacts/ux-designs/ux-Togo-2026-10-07/EXPERIENCE.md'
---

<frozen-after-approval reason="Obiettivo e criteri definiti dall'utente">

## Intent

Allineare gli otto mockup HTML di riferimento al contratto UX di Togo. Ogni riga azione, comprese le righe spuntate, mostra l'urgenza in testo. I controlli hanno un target minimo di 48 × 48 CSS px, equivalente ai 48 × 48 dp nel mockup, e uno stato `:focus-visible` per la navigazione da tastiera o D-pad. Le versioni scure mostrano gli stessi contenuti con i token di `DESIGN.md` per consentire il controllo del contrasto.

Lavorare nel checkout corrente e limitare le modifiche ai mockup e agli artefatti BMAD della 13.5. Registrare nel solo tracking che la 13.4 è completata per dichiarazione dell'utente. Il vincolo esclude build e test con qualsiasi strumento, stage, commit, reset e ripristino.

</frozen-after-approval>

## Implementation Notes

- I quattro mockup che mostrano azioni (`todo-page.html`, `action-sheet.html`, `share-dialog.html` e `conclude-dialog.html`) usano badge testuali per tutte le urgenze. L'anteprima delle attività completate mostra gli stessi badge della lista. I testi barrati usano il token `ink-struck`.
- `active-list.html`, `history.html`, `duplicate-dialog.html` e `voice-sheet.html` condividono con gli altri quattro mockup `mockups.css` per dimensioni minime, indicatore di focus e token colore. Il target della checkbox misura 48 × 48 CSS px, con simbolo da 26 px. Checkbox, opzioni e sezioni di esempio usano controlli HTML nativi. Nelle schermate con dialog o foglio aperto, i controlli dietro lo scrim sono inerti. Le frecce non disponibili nelle schermate principali restano raggiungibili e hanno `aria-disabled="true"`.
- I mockup si aprono in tema chiaro. Aggiungendo `#scuro` all'indirizzo, ad esempio `todo-page.html#scuro` o `action-sheet.html#scuro`, si applica la palette scura senza duplicare il markup.
- La rilettura del checkout ha portato a usare un contorno di focus interno per icone e intestazioni collassabili e a riservare spazio nello scorrimento sopra i controlli fissi delle due liste. Nello Storico il campo di ricerca occupa tutta l'area visibile, icona compresa. Su schermi stretti, titoli e sottotitoli possono andare a capo e i dialoghi di condivisione e conclusione si adattano alla larghezza disponibile.
- La Story 13.4 è passata da `in-progress` a `done` nel solo sprint status, per dichiarazione dell'utente. La spec storica risultava `in-review`. Nessuna verifica tecnica della 13.4 è stata eseguita in questa sessione.
- Rilettura statica del markup, del CSS e del diff nella sessione corrente. Restano da verificare nel browser il rendering, il contrasto e il focus con tastiera e D-pad. Build e test non eseguiti.
