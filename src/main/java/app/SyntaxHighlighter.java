package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;

/**
 * Evidenziatore di sintassi Java minimale: tokenizza il codice e produce un TextFlow
 * di nodi Text con classi CSS "tok tok-&lt;tipo&gt;". I colori stanno nel CSS e dipendono
 * dalla classe "palette-N" del contenitore (una palette per categoria).
 */
public final class SyntaxHighlighter {

    public enum Type { PLAIN, KEYWORD, LITERAL, TYPE, METHOD, STRING, NUMBER, COMMENT, ANNOTATION, OPERATOR, PUNCT }

    public record Token(Type type, String text) {
    }

    private static final Set<String> KEYWORDS = Set.of(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally", "float",
            "for", "goto", "if", "implements", "import", "instanceof", "int", "interface", "long", "native",
            "new", "package", "private", "protected", "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw", "throws", "transient", "try", "void",
            "volatile", "while", "var", "record", "sealed", "permits", "yield", "module", "requires",
            "exports", "opens", "uses", "provides", "to", "with", "when", "non");
    private static final Set<String> LITERALS = Set.of("true", "false", "null");
    private static final String OPS = "+-*/%=<>!&|^~?:";

    private SyntaxHighlighter() {
    }

    public static List<Token> tokenize(String src) {
        List<Token> out = new ArrayList<>();
        if (src == null) return out;
        int i = 0, n = src.length();
        while (i < n) {
            char c = src.charAt(i);
            int j;
            Type t;
            if (c == '/' && i + 1 < n && src.charAt(i + 1) == '/') {
                j = src.indexOf('\n', i);
                if (j < 0) j = n;
                t = Type.COMMENT;
            } else if (c == '/' && i + 1 < n && src.charAt(i + 1) == '*') {
                j = src.indexOf("*/", i + 2);
                j = j < 0 ? n : j + 2;
                t = Type.COMMENT;
            } else if (src.startsWith("\"\"\"", i)) {
                j = src.indexOf("\"\"\"", i + 3);
                j = j < 0 ? n : j + 3;
                t = Type.STRING;
            } else if (c == '"' || c == '\'') {
                j = i + 1;
                while (j < n && src.charAt(j) != c && src.charAt(j) != '\n') {
                    if (src.charAt(j) == '\\') j++;
                    j++;
                }
                if (j < n && src.charAt(j) == c) j++;
                j = Math.min(j, n);
                t = Type.STRING;
            } else if (c == '@' && i + 1 < n && Character.isJavaIdentifierStart(src.charAt(i + 1))) {
                j = i + 1;
                while (j < n && (Character.isJavaIdentifierPart(src.charAt(j)) || src.charAt(j) == '.')) j++;
                t = Type.ANNOTATION;
            } else if (Character.isDigit(c) || (c == '.' && i + 1 < n && Character.isDigit(src.charAt(i + 1)))) {
                j = i + 1;
                while (j < n && (Character.isLetterOrDigit(src.charAt(j)) || src.charAt(j) == '_'
                        || (src.charAt(j) == '.' && j + 1 < n && Character.isDigit(src.charAt(j + 1))))) j++;
                t = Type.NUMBER;
            } else if (Character.isJavaIdentifierStart(c)) {
                j = i + 1;
                while (j < n && Character.isJavaIdentifierPart(src.charAt(j))) j++;
                String w = src.substring(i, j);
                int k = j;
                while (k < n && src.charAt(k) == ' ') k++;
                if (KEYWORDS.contains(w)) t = Type.KEYWORD;
                else if (LITERALS.contains(w)) t = Type.LITERAL;
                else if (k < n && src.charAt(k) == '(') t = Type.METHOD;
                else if (Character.isUpperCase(w.charAt(0))) t = Type.TYPE;
                else t = Type.PLAIN;
            } else if (Character.isWhitespace(c)) {
                j = i + 1;
                while (j < n && Character.isWhitespace(src.charAt(j))) j++;
                t = Type.PLAIN;
            } else if (OPS.indexOf(c) >= 0) {
                j = i + 1;
                while (j < n && OPS.indexOf(src.charAt(j)) >= 0 && j - i < 3) j++;
                t = Type.OPERATOR;
            } else {
                j = i + 1;
                t = Type.PUNCT;
            }
            out.add(new Token(t, src.substring(i, j)));
            i = j;
        }
        return out;
    }

    /** TextFlow colorato (classi CSS: code-flow, tok, tok-keyword, ...). */
    public static TextFlow highlight(String code) {
        TextFlow flow = new TextFlow();
        flow.getStyleClass().add("code-flow");
        for (Token tk : tokenize(code)) {
            Text t = new Text(tk.text());
            t.getStyleClass().addAll("tok", "tok-" + tk.type().name().toLowerCase());
            flow.getChildren().add(t);
        }
        return flow;
    }
}
