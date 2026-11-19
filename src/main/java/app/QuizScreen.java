package app;

import java.util.ArrayList;
import java.util.List;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/** Mini-quiz di 3 domande a scelta multipla (una alla volta). */
final class QuizScreen extends BorderPane {
    private final App app;
    private final GameSession.Level level;
    private final Phase phase;
    private final VBox body = new VBox(12);

    QuizScreen(App app, GameSession.Level level) {
        this.app = app;
        this.level = level;
        this.phase = level.phase();
        VBox v = Ui.screen();
        v.getStyleClass().addAll("quiz", "palette-" + phase.paletteIndex());
        v.getChildren().add(Ui.header("Fase " + phase.id() + " · " + phase.title(), "Mini-quiz · " + phase.category(),
                (phase.help().isBlank() ? "" : phase.help() + "\n\n")
                        + "Scegli una sola risposta per domanda: +5 se corretta, −2 se sbagliata. "
                        + "Dopo la risposta compare la spiegazione."));
        v.getChildren().add(body);
        showQuestion();
        setCenter(Ui.scroll(v));
    }

    private void showQuestion() {
        GameSession s = app.session();
        int pos = s.quizPos();
        body.getChildren().clear();
        if (pos >= level.quiz().size()) {
            showSummary();
            return;
        }
        Phase.QuizQuestion q = level.quiz().get(pos);
        VBox card = Ui.card("quiz-card");
        card.getChildren().addAll(Ui.label("Domanda " + (pos + 1) + " di " + level.quiz().size(), "card-title"),
                Ui.richText(q.q()));

        Label explain = Ui.label("", "explain");
        explain.setVisible(false);
        explain.setManaged(false);
        Button next = Ui.btn(pos + 1 < level.quiz().size() ? "Domanda successiva  →" : "Concludi livello  →", "btn-primary", "btn-big");
        next.setVisible(false);
        next.setOnAction(e -> showQuestion());

        List<Button> buttons = new ArrayList<>();
        char letter = 'A';
        VBox options = new VBox(8);
        for (int k = 0; k < q.options().size(); k++) {
            Button b = Ui.btn(letter++ + ")  " + q.options().get(k), "option");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setWrapText(true);
            b.setAlignment(Pos.CENTER_LEFT);
            final int choice = k;
            b.setOnAction(e -> {
                GameSession.AnswerRecord r = s.recordQuiz(phase, q, choice, false);
                s.setQuizPos(pos + 1);
                for (int j = 0; j < buttons.size(); j++) {
                    Button o = buttons.get(j);
                    o.setDisable(true);
                    if (j == q.answer()) o.getStyleClass().add("ok");
                    else if (j == choice) o.getStyleClass().add("ko");
                }
                explain.setText((r.correct() ? "Esatto! " : "Sbagliato. La risposta corretta è: " + q.correctOption() + ". ")
                        + (r.points() != 0 ? "(" + (r.points() > 0 ? "+" : "") + r.points() + " punti)  " : "")
                        + q.explain());
                explain.getStyleClass().add(r.correct() ? "ok" : "ko");
                explain.setVisible(true);
                explain.setManaged(true);
                next.setVisible(true);
                next.requestFocus();
                app.refreshTop();
            });
            buttons.add(b);
            options.getChildren().add(b);
        }
        card.getChildren().addAll(options, explain);
        HBox act = new HBox(next);
        act.setAlignment(Pos.CENTER_RIGHT);
        body.getChildren().addAll(card, act);
    }

    private void showSummary() {
        long ok = app.session().records().stream()
                .filter(r -> r.phaseId() == phase.id() && r.kind() == GameSession.Kind.QUIZ && r.correct()).count();
        VBox card = Ui.card("quiz-card");
        card.getChildren().addAll(Ui.label("Livello completato!", "card-title"),
                Ui.label("Risposte corrette in questo quiz: " + Math.min(ok, level.quiz().size()) + " / " + level.quiz().size(), "body"),
                Ui.label("Punteggio attuale: " + app.session().score(), "body", "accent"));
        boolean last = app.session().levelNumber() >= app.session().totalLevels();
        Button next = Ui.btn(last ? "Vedi i risultati  →" : "Livello successivo  →", "btn-primary", "btn-big");
        next.setOnAction(e -> app.levelCompleted());
        HBox act = new HBox(next);
        act.setAlignment(Pos.CENTER_RIGHT);
        body.getChildren().addAll(card, act);
        next.requestFocus();
    }
}
