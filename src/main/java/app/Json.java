package app;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Parser JSON minimale: restituisce Map, List, String, Double, Boolean o null. */
public final class Json {
    private final String s;
    private int i;

    private Json(String s) {
        this.s = s;
    }

    public static Object parse(String text) {
        if (text.startsWith("﻿")) text = text.substring(1);
        Json j = new Json(text);
        j.ws();
        Object v = j.value();
        j.ws();
        if (j.i < j.s.length()) throw j.err("contenuto inatteso");
        return v;
    }

    private IllegalArgumentException err(String m) {
        int line = 1;
        for (int k = 0; k < Math.min(i, s.length()); k++) if (s.charAt(k) == '\n') line++;
        return new IllegalArgumentException("JSON non valido: " + m + " (riga " + line + ", pos " + i + ")");
    }

    private void ws() {
        while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++;
    }

    private Object value() {
        if (i >= s.length()) throw err("fine inattesa");
        char c = s.charAt(i);
        switch (c) {
            case '{': return object();
            case '[': return array();
            case '"': return string();
            case 't': literal("true"); return Boolean.TRUE;
            case 'f': literal("false"); return Boolean.FALSE;
            case 'n': literal("null"); return null;
            default: return number();
        }
    }

    private void literal(String w) {
        if (!s.startsWith(w, i)) throw err("atteso " + w);
        i += w.length();
    }

    private Map<String, Object> object() {
        Map<String, Object> m = new LinkedHashMap<>();
        i++;
        ws();
        if (peek() == '}') { i++; return m; }
        while (true) {
            ws();
            if (peek() != '"') throw err("attesa chiave");
            String k = string();
            ws();
            if (peek() != ':') throw err("atteso ':'");
            i++;
            ws();
            m.put(k, value());
            ws();
            char c = peek();
            i++;
            if (c == '}') return m;
            if (c != ',') throw err("atteso ',' o '}'");
        }
    }

    private List<Object> array() {
        List<Object> l = new ArrayList<>();
        i++;
        ws();
        if (peek() == ']') { i++; return l; }
        while (true) {
            ws();
            l.add(value());
            ws();
            char c = peek();
            i++;
            if (c == ']') return l;
            if (c != ',') throw err("atteso ',' o ']'");
        }
    }

    private char peek() {
        if (i >= s.length()) throw err("fine inattesa");
        return s.charAt(i);
    }

    private String string() {
        i++; // apertura "
        StringBuilder b = new StringBuilder();
        while (true) {
            if (i >= s.length()) throw err("stringa non chiusa");
            char c = s.charAt(i++);
            if (c == '"') return b.toString();
            if (c != '\\') { b.append(c); continue; }
            if (i >= s.length()) throw err("escape incompleto");
            char e = s.charAt(i++);
            switch (e) {
                case '"': b.append('"'); break;
                case '\\': b.append('\\'); break;
                case '/': b.append('/'); break;
                case 'b': b.append('\b'); break;
                case 'f': b.append('\f'); break;
                case 'n': b.append('\n'); break;
                case 'r': b.append('\r'); break;
                case 't': b.append('\t'); break;
                case 'u':
                    if (i + 4 > s.length()) throw err("escape unicode incompleto");
                    b.append((char) Integer.parseInt(s.substring(i, i + 4), 16));
                    i += 4;
                    break;
                default: throw err("escape sconosciuto \\" + e);
            }
        }
    }

    private Double number() {
        int st = i;
        while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
        if (st == i) throw err("carattere inatteso '" + s.charAt(i) + "'");
        try {
            return Double.parseDouble(s.substring(st, i));
        } catch (NumberFormatException ex) {
            throw err("numero non valido");
        }
    }
}
