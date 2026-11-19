# 🟢 JavaQuest100

> **100 livelli. 1 linguaggio. Zero scuse.**
> Impara Java 25 giocando, con il cervello acceso e il terminale verde fosforescente. ☕⚡

JavaQuest100 è un gioco-corso per Windows: ogni livello ti spiega un argomento di Java, ti sfida a **rimettere in ordine le righe di codice** come un puzzle e ti interroga con un mini-quiz. Sbagli? Perdi punti (e un pezzo di dignità). Indovini? Punti, gloria e un bel verde brillante. 😎

## 🎮 Come si gioca

1. **Lezione**: leggi la spiegazione e guarda il codice d'esempio. Il colore del codice cambia in base alla categoria, perché la vita è già troppo grigia.
2. **Esercizio**: clicca i frammenti di codice per comporre il programma giusto. Attenzione ai *distrattori*, righe dall'aria innocente che sono lì solo per fregarti. 🕵️
3. **Mini-quiz**: 3 domande a risposta multipla e si passa al livello successivo.
4. **Fine gioco**: il gioco genera da solo un **PDF** con le risposte giuste e sbagliate. Puoi scaricarlo e appenderlo in camera (o nasconderlo, scegli tu).

| Azione | Punti |
|---|---|
| Esercizio indovinato | **+10** 🎉 |
| Tentativo sbagliato | **-5** 💥 |
| Domanda del quiz giusta | **+5** |
| Domanda del quiz sbagliata | **-2** |

🎲 **Tutto è casuale!** Ogni volta che apri il gioco, l'ordine dei livelli, dei frammenti e delle risposte si rimescola. Nessun trucco da memorizzare.

🆘 Bloccato? Ogni schermata ha il pulsante **AIUTO** con un suggerimento. Non ti giudichiamo. Troppo. 🙈

📚 Nel menu **Elenco** trovi tutte le 100 domande divise per categoria, per allenarti dove fa più male.

## 🗺️ Il programma: 10 categorie × 10 fasi

1. Fondamenti
2. Controllo del flusso
3. Metodi, Array e Stringhe
4. Programmazione a oggetti
5. Interfacce, Record, Enum, Sealed
6. Generics e Collections
7. Lambda e Stream
8. Eccezioni, I/O, NIO
9. Concorrenza (Virtual Threads inclusi 🧵)
10. Java moderno 21-25 e strumenti

Il piano completo con tutte le 100 fasi è in [`data-src/outline.md`](data-src/outline.md).

## 💾 Scarica e gioca

Vai nella sezione **[Releases](../../releases)** e scarica `JavaQuest100_singolo.exe`. Fine, niente installer, niente Java da installare: il runtime è già dentro.

- Funziona su **Windows 64 bit**.
- Al primo avvio si estrae in `%LOCALAPPDATA%\JavaQuest100_app` (~150 MB), poi parte in un lampo.
- ⚠️ L'exe non è firmato digitalmente: SmartScreen o l'antivirus potrebbero fare i diffidenti. Clicca *Maggiori informazioni → Esegui comunque*.

## 🛠️ Compilarlo da soli

Il repository non contiene il JDK né JavaFX (sono pesanti). Per compilare:

1. Scarica **JDK 25** (es. Temurin) e mettilo in `tools/jdk`.
2. Scarica **JavaFX 25** SDK e jmods per Windows (gluonhq.com) in `tools/javafx-sdk` e `tools/javafx-jmods`.
3. Lancia:

```powershell
powershell -ExecutionPolicy Bypass -File build.ps1 -Test            # crea dist\JavaQuest100\JavaQuest100.exe
powershell -ExecutionPolicy Bypass -File build-singolo.ps1          # crea exe-unico\JavaQuest100_singolo.exe
```

Altri comandi utili: `run.ps1` (avvio in sviluppo), `python data-src/merge.py` (unisce i contenuti `part*.json` nel curriculum).

## 🧱 Sotto il cofano

- Java 25 + JavaFX, **nessuna libreria esterna**: parser JSON, evidenziatore di sintassi e generatore PDF sono scritti a mano.
- Tema nero e verde in stile terminale, tutto nel CSS ([`style.css`](src/main/resources/css/style.css)).
- Runtime ridotto con `jlink`, pacchetto con `jpackage`, file unico autoestraente.

Buon divertimento e che il `NullPointerException` sia con te. 🚀
