package app;

import java.net.URL;
import java.util.List;
import java.util.Random;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** Applicazione JavaFX: gestisce la sessione e la navigazione fra le schermate. */
public final class App extends Application {
    private Stage stage;
    private Scene scene;
    private BorderPane root;
    private TopBar topBar;
    private Curriculum curriculum;
    private GameSession session;

    @Override
    public void start(Stage stage) {
        this.stage = stage;
        Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
            Main.logError("Errore UI", e);
            error("Si è verificato un errore: " + e);
        });
        curriculum = CurriculumLoader.loadDefault();
        session = new GameSession(curriculum, new Random());

        root = new BorderPane();
        root.getStyleClass().add("root-bg");
        topBar = new TopBar(this);
        root.setTop(topBar);
        Rectangle2D vb = Screen.getPrimary().getVisualBounds();
        scene = new Scene(root, Math.min(1200, vb.getWidth() * 0.92), Math.min(820, vb.getHeight() * 0.9));
        URL css = App.class.getResource("/css/style.css");
        if (css != null) scene.getStylesheets().add(css.toExternalForm());
        stage.setTitle("Java Quest 100");
        try (var in = App.class.getResourceAsStream("/icon.png")) {
            if (in != null) stage.getIcons().add(new javafx.scene.image.Image(in));
        } catch (Exception ignored) { }
        stage.setMinWidth(900);
        stage.setMinHeight(620);
        stage.setScene(scene);
        showHome();
        stage.show();
        stage.centerOnScreen();
        demo(getParameters().getRaw());
    }

    /** Opzione nascosta di sviluppo: --demo=lesson|exercise|quiz|list|results apre subito quella schermata. */
    private void demo(List<String> args) {
        for (String a : args) {
            if (!a.startsWith("--demo=")) continue;
            switch (a.substring(7)) {
                case "lesson" -> play();
                case "exercise" -> goExercise(session.currentLevel());
                case "quiz" -> goQuiz(session.currentLevel());
                case "list" -> showList();
                case "results" -> finishGame();
                default -> { }
            }
        }
    }

    // ---------------- accesso ----------------

    GameSession session() {
        return session;
    }

    Curriculum curriculum() {
        return curriculum;
    }

    Stage stage() {
        return stage;
    }

    void refreshTop() {
        topBar.refresh();
    }

    private void show(Node screen) {
        root.setCenter(screen);
        topBar.refresh();
    }

    // ---------------- navigazione ----------------

    void showHome() {
        show(new HomeScreen(this));
    }

    void newGame() {
        session = new GameSession(curriculum, new Random());
        play();
    }

    /** Riprende la partita dal punto in cui era rimasta (lezione, esercizio o quiz). */
    void play() {
        if (session.isFinished()) {
            finishGame();
            return;
        }
        GameSession.Level l = session.currentLevel();
        switch (session.step()) {
            case LESSON -> show(new LessonScreen(this, l));
            case EXERCISE -> show(new ExerciseScreen(this, l, false));
            case QUIZ -> show(new QuizScreen(this, l));
        }
    }

    void goExercise(GameSession.Level l) {
        session.setStep(GameSession.Step.EXERCISE);
        show(new ExerciseScreen(this, l, false));
    }

    void goQuiz(GameSession.Level l) {
        session.setStep(GameSession.Step.QUIZ);
        session.setQuizPos(0);
        show(new QuizScreen(this, l));
    }

    void levelCompleted() {
        if (session.advance()) finishGame();
        else play();
    }

    void showList() {
        show(new QuestionListScreen(this));
    }

    /** Modalità libera: apre l'esercizio di una fase scelta dall'elenco. */
    void openFree(Phase p) {
        show(new ExerciseScreen(this, session.prepare(p), true));
    }

    void confirmFinish() {
        if (session.isFinished()) {
            finishGame();
            return;
        }
        Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                "Vuoi terminare la partita e vedere i risultati?\nVerrà generato il PDF di riepilogo.",
                ButtonType.YES, ButtonType.NO);
        a.setTitle("Termina partita");
        a.setHeaderText("Termina");
        style(a);
        a.showAndWait().filter(b -> b == ButtonType.YES).ifPresent(b -> finishGame());
    }

    void finishGame() {
        session.finish();
        show(new ResultsScreen(this));
    }

    void error(String msg) {
        Platform.runLater(() -> {
            Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
            a.setTitle("Errore");
            style(a);
            a.showAndWait();
        });
    }

    void style(Alert a) {
        a.initOwner(stage);
        a.getDialogPane().getStylesheets().addAll(scene.getStylesheets());
        a.getDialogPane().getStyleClass().add("dialog");
    }
}
