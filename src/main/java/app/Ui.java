package app;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/** Piccole fabbriche di nodi con classi CSS semantiche (il look è tutto in css/style.css). */
final class Ui {
    private Ui() {
    }

    static Button btn(String text, String... classes) {
        Button b = new Button(text);
        b.getStyleClass().add("btn");
        b.getStyleClass().addAll(classes);
        b.setFocusTraversable(true);
        return b;
    }

    static Label label(String text, String... classes) {
        Label l = new Label(text);
        l.setWrapText(true);
        l.setMaxWidth(Double.MAX_VALUE);
        l.getStyleClass().addAll(classes);
        return l;
    }

    static Region hspacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    static VBox card(String... extra) {
        VBox v = new VBox(10);
        v.getStyleClass().add("card");
        v.getStyleClass().addAll(extra);
        return v;
    }

    /** Testo di lezione: `codice` fra backtick diventa inline-code. */
    static TextFlow richText(String text) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("lesson-flow");
        String[] parts = text.split("`", -1);
        for (int k = 0; k < parts.length; k++) {
            if (parts[k].isEmpty()) continue;
            Text t = new Text(parts[k]);
            t.getStyleClass().add(k % 2 == 1 ? "inline-code" : "lesson-text");
            flow.getChildren().add(t);
        }
        return flow;
    }

    /** Blocco di codice colorato con la palette della categoria. */
    static VBox codeBlock(String code, int palette) {
        VBox box = new VBox(SyntaxHighlighter.highlight(code));
        box.getStyleClass().addAll("code-area", "palette-" + palette);
        return box;
    }

    static ScrollPane scroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("screen-scroll");
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        return sp;
    }

    /**
     * Intestazione di schermata con titolo, sottotitolo e pulsante AIUTO che mostra/nasconde
     * un pannello help-box.
     */
    static VBox header(String title, String subtitle, String help) {
        Label t = label(title, "title");
        VBox titles = new VBox(4, t);
        if (subtitle != null && !subtitle.isBlank()) titles.getChildren().add(label(subtitle, "subtitle"));
        HBox.setHgrow(titles, Priority.ALWAYS);

        VBox helpBox = new VBox(6, label("AIUTO", "help-title"), label(help, "help-text"));
        helpBox.getStyleClass().add("help-box");
        helpBox.setVisible(false);
        helpBox.setManaged(false);

        Button helpBtn = btn("AIUTO", "btn-help");
        helpBtn.setOnAction(e -> {
            boolean show = !helpBox.isVisible();
            helpBox.setVisible(show);
            helpBox.setManaged(show);
            helpBtn.setText(show ? "Chiudi aiuto" : "AIUTO");
        });
        HBox row = new HBox(12, titles, helpBtn);
        row.setAlignment(Pos.TOP_LEFT);
        VBox v = new VBox(10, row, helpBox);
        v.getStyleClass().add("screen-header");
        return v;
    }

    /** Contenitore standard di una schermata. */
    static VBox screen() {
        VBox v = new VBox(16);
        v.getStyleClass().add("screen");
        return v;
    }
}
