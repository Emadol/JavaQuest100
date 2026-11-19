package app;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

/** Schermata iniziale. */
final class HomeScreen extends BorderPane {

    HomeScreen(App app) {
        GameSession s = app.session();
        VBox v = Ui.screen();
        v.getStyleClass().add("home");
        v.getChildren().add(Ui.header("JAVA QUEST 100",
                "Impara Java 25 in " + s.totalLevels() + " livelli: lezione, esercizio di composizione del codice e mini-quiz.",
                "Premi \"Inizia\" per giocare: ogni livello parte da una lezione, poi devi comporre il codice "
                        + "scegliendo le righe giuste nell'ordine giusto e infine rispondere a 3 domande.\n"
                        + "L'ordine dei livelli, delle righe e delle risposte è casuale a ogni avvio.\n"
                        + "\"Elenco domande\" ti permette di aprire liberamente qualunque esercizio. "
                        + "\"Termina\" mostra i risultati e genera il PDF di riepilogo."));

        VBox rules = Ui.card("rules");
        rules.getChildren().addAll(
                Ui.label("Come si gioca", "card-title"),
                Ui.label("1.  Leggi la lezione e l'esempio (i colori del codice cambiano con la categoria).", "body"),
                Ui.label("2.  Componi il codice: clicca i frammenti per aggiungerli, riordinali con ▲ ▼, rimuovili con ✕, poi Verifica.", "body"),
                Ui.label("3.  Rispondi al mini-quiz di 3 domande.", "body"),
                Ui.label("Punteggio: esercizio +10 (−5 per ogni tentativo errato) · quiz +5 / −2 per domanda.", "body", "accent"));

        String stats = s.isStarted()
                ? "Partita in corso: livello " + s.levelNumber() + " di " + s.totalLevels() + " · punteggio " + s.score()
                : "Nuova partita pronta: " + s.totalLevels() + " livelli in ordine casuale.";
        if (s.isFinished()) stats = "Partita terminata con " + s.score() + " punti.";

        Button play = Ui.btn(s.isFinished() ? "Vedi risultati" : s.isStarted() ? "Continua" : "Inizia", "btn-primary", "btn-big");
        play.setOnAction(e -> app.play());
        Button fresh = Ui.btn("Nuova partita", "btn-big");
        fresh.setOnAction(e -> app.newGame());
        fresh.setVisible(s.isStarted());
        fresh.setManaged(s.isStarted());
        Button list = Ui.btn("Elenco domande", "btn-big");
        list.setOnAction(e -> app.showList());
        Button end = Ui.btn("Termina", "btn-big", "btn-danger");
        end.setOnAction(e -> app.confirmFinish());
        FlowPane actions = new FlowPane(12, 12, play, fresh, list, end);
        actions.setAlignment(Pos.CENTER_LEFT);

        v.getChildren().addAll(rules, Ui.label(stats, "subtitle", "status"), actions);
        setCenter(Ui.scroll(v));
        play.requestFocus();
    }
}
