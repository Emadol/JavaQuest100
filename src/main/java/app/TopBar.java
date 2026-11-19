package app;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

/** Barra superiore: livello/100, avanzamento, punteggio, pulsanti Elenco, Home, Termina. */
final class TopBar extends HBox {
    private final App app;
    private final Label level = Ui.label("", "top-level");
    private final Label score = Ui.label("", "top-score");
    private final ProgressBar progress = new ProgressBar(0);

    TopBar(App app) {
        super(14);
        this.app = app;
        getStyleClass().add("top-bar");
        setAlignment(Pos.CENTER_LEFT);
        level.setMaxWidth(Double.MAX_VALUE);
        level.setWrapText(false);
        score.setWrapText(false);
        progress.getStyleClass().add("progress");
        progress.setPrefWidth(220);

        Label brand = Ui.label("JAVA QUEST 100", "brand");
        brand.setWrapText(false);
        Button list = Ui.btn("Elenco", "btn-top");
        list.setOnAction(e -> app.showList());
        Button home = Ui.btn("Home", "btn-top");
        home.setOnAction(e -> app.showHome());
        Button end = Ui.btn("Termina", "btn-top", "btn-danger");
        end.setOnAction(e -> app.confirmFinish());
        getChildren().addAll(brand, level, progress, score, Ui.hspacer(), list, home, end);
    }

    void refresh() {
        GameSession s = app.session();
        level.setText("Livello " + s.levelNumber() + "/" + s.totalLevels());
        progress.setProgress(s.totalLevels() == 0 ? 0 : (double) s.completedLevels() / s.totalLevels());
        score.setText("Punteggio: " + s.score());
    }
}
