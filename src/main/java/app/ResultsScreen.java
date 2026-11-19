package app;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** Risultati finali + PDF generato automaticamente nella cartella temporanea. */
final class ResultsScreen extends BorderPane {
    private Path pdf;

    ResultsScreen(App app) {
        GameSession s = app.session();
        VBox v = Ui.screen();
        v.getStyleClass().add("results");
        v.getChildren().add(Ui.header("Risultati", "Partita terminata",
                "Qui trovi il riepilogo della partita. Il PDF con tutte le risposte giuste e sbagliate "
                        + "(con la risposta corretta) è già stato creato nella cartella temporanea: "
                        + "usa \"Scarica PDF\" per salvarne una copia dove vuoi."));

        VBox scoreCard = Ui.card("score-card");
        scoreCard.setAlignment(Pos.CENTER);
        scoreCard.getChildren().addAll(Ui.label("Punteggio finale", "card-title"), label(String.valueOf(s.score()), "big-score"));

        GridPane stats = new GridPane();
        stats.getStyleClass().add("stats");
        stats.setHgap(24);
        stats.setVgap(8);
        int wrongAttempts = s.records().stream().filter(r -> r.kind() == GameSession.Kind.ESERCIZIO)
                .mapToInt(GameSession.AnswerRecord::wrongAttempts).sum();
        List<String[]> rows = List.of(
                new String[]{"Livelli completati", s.completedLevels() + " / " + s.totalLevels()},
                new String[]{"Esercizi giusti al primo colpo", String.valueOf(s.count(GameSession.Kind.ESERCIZIO, true))},
                new String[]{"Esercizi con errori o non risolti", String.valueOf(s.count(GameSession.Kind.ESERCIZIO, false))},
                new String[]{"Tentativi errati negli esercizi", String.valueOf(wrongAttempts)},
                new String[]{"Domande quiz giuste", String.valueOf(s.count(GameSession.Kind.QUIZ, true))},
                new String[]{"Domande quiz sbagliate", String.valueOf(s.count(GameSession.Kind.QUIZ, false))});
        for (int k = 0; k < rows.size(); k++) {
            stats.add(label(rows.get(k)[0], "stat-name"), 0, k);
            stats.add(label(rows.get(k)[1], "stat-value"), 1, k);
        }
        VBox statsCard = Ui.card("stats-card");
        statsCard.getChildren().addAll(Ui.label("Statistiche", "card-title"), stats);

        Label pdfInfo = Ui.label("", "body");
        Button download = Ui.btn("Scarica PDF", "btn-primary", "btn-big");
        try {
            pdf = ReportBuilder.writeToTemp(s);
            pdfInfo.setText("PDF generato: " + pdf);
        } catch (Exception ex) {
            Main.logError("Generazione PDF", ex);
            pdfInfo.setText("Errore nella generazione del PDF: " + ex.getMessage());
            pdfInfo.getStyleClass().add("ko");
            download.setDisable(true);
        }
        download.setOnAction(e -> save(app));
        Button again = Ui.btn("Nuova partita", "btn-big");
        again.setOnAction(e -> app.newGame());
        Button list = Ui.btn("Elenco domande", "btn-big");
        list.setOnAction(e -> app.showList());
        FlowPane actions = new FlowPane(12, 12, download, again, list);

        VBox pdfCard = Ui.card("pdf-card");
        pdfCard.getChildren().addAll(Ui.label("Riepilogo PDF", "card-title"), pdfInfo, actions);
        v.getChildren().addAll(scoreCard, statsCard, pdfCard);
        setCenter(Ui.scroll(v));
    }

    private static Label label(String t, String cls) {
        Label l = new Label(t);
        l.getStyleClass().add(cls);
        return l;
    }

    private void save(App app) {
        if (pdf == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle("Salva il PDF dei risultati");
        fc.setInitialFileName(pdf.getFileName().toString());
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Documento PDF", "*.pdf"));
        File home = new File(System.getProperty("user.home"));
        File docs = new File(home, "Documents");
        fc.setInitialDirectory(docs.isDirectory() ? docs : home);
        File target = fc.showSaveDialog(app.stage());
        if (target == null) return;
        try {
            Files.copy(pdf, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,
                    "PDF salvato in:\n" + target, javafx.scene.control.ButtonType.OK);
            a.setTitle("PDF salvato");
            a.setHeaderText("Salvataggio completato");
            app.style(a);
            a.showAndWait();
        } catch (Exception ex) {
            Main.logError("Salvataggio PDF", ex);
            app.error("Impossibile salvare il PDF: " + ex.getMessage());
        }
    }
}
