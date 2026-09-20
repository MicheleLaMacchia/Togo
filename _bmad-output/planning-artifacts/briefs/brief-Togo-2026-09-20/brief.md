---
title: "Product Brief: TOGO"
status: final
created: 2026-09-20
updated: 2026-09-20
---

# Product Brief: TOGO

## Executive Summary

TOGO è un’app Android local-first per creare e usare liste della spesa affidabili. Aiuta l’utente a catturare un prodotto nel momento in cui sta finendo — anche tramite comando vocale — e a ritrovarlo in una categoria coerente durante la spesa.

Il valore principale non è sostituire una semplice nota, ma ridurre due errori ricorrenti: dimenticare ciò che serve e ritrovarsi con prodotti duplicati o classificati in modo incoerente. L’MVP è pensato per un singolo utilizzatore, ma il modello dovrà poter evolvere verso più liste, condivisione e collaborazione familiare.

## Il problema

Quando un prodotto finisce o sta per finire, l’utente può non avere il telefono a portata di mano o può avere le mani impegnate. Se rimanda la registrazione, rischia di dimenticarlo. Quando arriva il momento della spesa, una nota testuale generica non offre necessariamente categorie coerenti, controllo dei duplicati o una struttura utile alla consultazione.

Il problema si presenta in due momenti collegati:

- a casa, durante la cattura dei prodotti da acquistare;
- al supermercato, durante la consultazione e la gestione della lista.

Il costo dello status quo è una spesa incompleta, una lista disordinata e un maggiore sforzo mentale per controllare e correggere le voci.

## La soluzione

TOGO offre una sola lista “Spesa” nell’MVP, alimentabile manualmente oppure con un comando vocale relativo a un prodotto per volta. Ogni voce viene interpretata, associata a una categoria canonica e registrata con quantità e unità normalizzate quando disponibili.

Se un prodotto normalizzato sembra già presente, soprattutto nella stessa categoria specifica, l’app avvisa l’utente e chiede se creare una nuova voce o aumentare la quantità. L’app non deve generare avvisi per tutti i prodotti della stessa categoria generale: latte e yogurt, ad esempio, non sono duplicati solo perché appartengono ai latticini. Le ambiguità non devono produrre modifiche silenziose: l’utente può correggere o annullare.

Il percorso vocale dell’MVP è intenzionalmente limitato: un prodotto per comando, con conferma breve quando l’interpretazione è chiara e una domanda mirata quando mancano prodotto, quantità, unità o categoria. In caso di errore o annullamento, la lista non viene modificata.

Durante la spesa, l’utente può depennare le voci dall’app. I prodotti acquistati vengono conservati in uno storico minimale e possono essere riaggiunti tramite l’interfaccia. La lista attiva può essere condivisa come testo raggruppato per categoria.

## Cosa rende TOGO diverso

La differenziazione iniziale è di esperienza ed esecuzione, non una barriera tecnologica:

- comando vocale focalizzato sulla cattura rapida di ciò che manca;
- categorie canoniche e coerenti, con un solo posizionamento principale per prodotto;
- controllo dei duplicati prima di aumentare o creare una voce;
- quantità modellate come valore più unità, per evitare di trattare “2 l”, “500 g” e “6 pezzi” come semplice testo;
- dati locali e funzionamento del nucleo dell’app indipendente dalle integrazioni opzionali.

Il vantaggio da costruire nel tempo è un catalogo personale correggibile: le correzioni dell’utente potranno migliorare sinonimi, categorie e preferenze senza rendere obbligatorio l’uso di AI remota.

## A chi serve

### Utente iniziale

Una persona che gestisce la propria spesa e vuole ridurre le dimenticanze senza interrompere ciò che sta facendo. Cerca velocità nella cattura e ordine nella consultazione, non una gestione complessa dell’inventario.

### Utenti futuri

Coppie e famiglie che usano più liste, condividono una lista attiva o contribuiscono alla stessa spesa. Questi utenti appartengono alla visione del prodotto, non al perimetro operativo del primo MVP.

## Criteri di successo

L’MVP è utile se:

- consente di aggiungere un prodotto senza perdere il contesto dell’attività in corso;
- colloca i prodotti nella categoria corretta nella maggior parte dei casi d’uso reali;
- riduce la comparsa di duplicati o chiede conferma prima di crearli;
- rende la lista consultabile e ordinata durante la spesa;
- permette di depennare i prodotti e ritrovare quelli acquistati nello storico;
- produce una condivisione testuale leggibile e raggruppata per categoria.

Le prime verifiche dovranno misurare almeno:

- correttezza della categoria assegnata;
- numero di duplicati creati o segnalati correttamente;
- tempo necessario per aggiungere un prodotto;
- numero di correzioni richieste dopo un comando vocale;
- prodotti dimenticati durante una spesa reale.

[ASSUNZIONE] Le soglie numeriche saranno definite dopo un primo test d’uso, perché non sono ancora disponibili dati reali di riferimento.

## Perimetro dell’MVP

### Incluso

- Android-only, con dati conservati localmente sul dispositivo;
- una lista iniziale chiamata “Spesa”;
- inserimento manuale;
- inserimento vocale tramite Google Assistant/App Actions o ingresso vocale equivalente disponibile sul dispositivo;
- un prodotto per comando vocale;
- categorie canoniche, sinonimi, quantità e unità normalizzate;
- rilevamento dei possibili duplicati e richiesta di conferma;
- correzione e annullamento delle operazioni rischiose;
- depennamento manuale dall’app;
- storico minimale dei prodotti acquistati;
- condivisione testuale della lista attiva per categoria;

### Esplicitamente fuori scope

- iOS;
- wake word proprietario “Hey TOGO” e ascolto continuo;
- riconoscimento vocale offline;
- comandi vocali multi-prodotto e transazioni atomiche complete;
- più liste nell’interfaccia MVP;
- collaborazione simultanea e sincronizzazione cloud;
- backup/import tramite file;
- reminder e integrazione con Google Calendar o Google Tasks;
- prezzi, scontrini, confezioni commerciali e confronti di risparmio;
- AI obbligatoria o personalizzazione remota.

I reminder potranno essere aggiunti in una versione successiva, dopo aver validato il nucleo della lista. In questo modo l’MVP non dipende da OAuth, permessi Google, ricorrenze, notifiche o comportamenti esterni all’app.

## Visione

TOGO può diventare uno spazio personale e condiviso per organizzare la spesa: più liste nominate, collaborazione tra persone, storico arricchito con quantità, negozio e prezzi, e suggerimenti basati sulle abitudini dell’utente.

La traiettoria deve partire dalla fiducia nella categorizzazione. Prima di aggiungere automazioni sofisticate, TOGO dovrà rendere visibili, correggibili e riutilizzabili le proprie regole. In questo modo il futuro confronto dei costi e il supporto a coppie e famiglie poggeranno su dati ordinati, non su testo ambiguo.
