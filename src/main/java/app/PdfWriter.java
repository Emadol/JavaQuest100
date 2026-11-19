package app;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Generatore PDF minimale scritto a mano: font base-14 (Helvetica, Helvetica-Bold,
 * Helvetica-Oblique, Courier), pagine A4, testo con a capo automatico e paginazione,
 * escape di ( ) \ e conversione dei caratteri in WinAnsiEncoding.
 */
public final class PdfWriter {

    public enum Font {
        REGULAR("F1", "Helvetica"), BOLD("F2", "Helvetica-Bold"), MONO("F3", "Courier"), ITALIC("F4", "Helvetica-Oblique");
        final String res;
        final String base;

        Font(String res, String base) {
            this.res = res;
            this.base = base;
        }
    }

    public static final float PAGE_W = 595.28f;
    public static final float PAGE_H = 841.89f;
    public static final float MARGIN = 50f;
    private static final float BOTTOM = 60f;

    /** Larghezze Helvetica (1/1000 em) per i caratteri 32..126. */
    private static final int[] HELV = {
            278, 278, 355, 556, 556, 889, 667, 191, 333, 333, 389, 584, 278, 333, 278, 278,
            556, 556, 556, 556, 556, 556, 556, 556, 556, 556, 278, 278, 584, 584, 584, 556,
            1015, 667, 667, 722, 722, 667, 611, 778, 722, 278, 500, 667, 556, 833, 722, 778,
            667, 778, 722, 667, 611, 722, 667, 944, 667, 667, 611, 278, 278, 278, 469, 556,
            333, 556, 556, 500, 556, 556, 278, 556, 556, 222, 222, 500, 222, 833, 556, 556,
            556, 556, 333, 500, 278, 556, 500, 722, 500, 500, 500, 334, 260, 334, 584};

    private final List<ByteArrayOutputStream> pages = new ArrayList<>();
    private ByteArrayOutputStream page;
    private float y;
    private final String title;

    public PdfWriter(String title) {
        this.title = title;
        newPage();
    }

    // ---------------- API di layout ----------------

    public void newPage() {
        page = new ByteArrayOutputStream();
        pages.add(page);
        y = PAGE_H - MARGIN;
    }

    public void gap(float h) {
        y -= h;
        if (y < BOTTOM) newPage();
    }

    /** Paragrafo con a capo automatico (rispetta i \n). */
    public void paragraph(String text, Font font, float size, float indent, float[] rgb) {
        float maxW = PAGE_W - 2 * MARGIN - indent;
        float lead = size * 1.35f;
        for (String line : wrap(text, font, size, maxW)) {
            if (y - lead < BOTTOM) newPage();
            y -= lead;
            drawText(line, font, size, MARGIN + indent, y, rgb);
        }
    }

    public void paragraph(String text, Font font, float size) {
        paragraph(text, font, size, 0, null);
    }

    /** Blocco di codice monospazio con sfondo grigio chiaro. */
    public void code(String text, float size, float indent) {
        float maxW = PAGE_W - 2 * MARGIN - indent - 8;
        float lead = size * 1.3f;
        List<String> lines = wrap(text, Font.MONO, size, maxW);
        for (String line : lines) {
            if (y - lead < BOTTOM) newPage();
            y -= lead;
            write(String.format(Locale.ROOT, "0.94 0.94 0.94 rg %.2f %.2f %.2f %.2f re f\n",
                    MARGIN + indent, y - size * 0.35f, PAGE_W - 2 * MARGIN - indent, lead));
            drawText(line, Font.MONO, size, MARGIN + indent + 4, y, null);
        }
    }

    /** Linea orizzontale. */
    public void rule(float[] rgb) {
        gap(6);
        float[] c = rgb == null ? new float[]{0.6f, 0.6f, 0.6f} : rgb;
        write(String.format(Locale.ROOT, "%.3f %.3f %.3f RG 0.8 w %.2f %.2f m %.2f %.2f l S\n",
                c[0], c[1], c[2], MARGIN, y, PAGE_W - MARGIN, y));
        gap(6);
    }

    public int pageCount() {
        return pages.size();
    }

    // ---------------- misure e a capo ----------------

    public static float width(String s, Font f, float size) {
        float w = 0;
        for (int k = 0; k < s.length(); k++) w += charWidth(s.charAt(k), f);
        return w * size / 1000f;
    }

    private static float charWidth(char c, Font f) {
        if (f == Font.MONO) return 600;
        int w = (c >= 32 && c <= 126) ? HELV[c - 32] : 556;
        return f == Font.BOLD ? w * 1.06f : w;
    }

    static List<String> wrap(String text, Font font, float size, float maxW) {
        List<String> out = new ArrayList<>();
        String norm = normalize(text);
        for (String raw : norm.split("\n", -1)) {
            if (width(raw, font, size) <= maxW) {
                out.add(raw);
                continue;
            }
            String indent = font == Font.MONO ? raw.substring(0, raw.length() - raw.stripLeading().length()) + "  " : "";
            StringBuilder cur = new StringBuilder();
            for (String word : raw.split("(?<= )")) {
                if (width(cur + word, font, size) <= maxW) {
                    cur.append(word);
                    continue;
                }
                if (cur.length() > 0) {
                    out.add(cur.toString().stripTrailing());
                    cur.setLength(0);
                    cur.append(indent);
                }
                // parola più lunga della riga: spezza per caratteri
                for (char ch : word.toCharArray()) {
                    if (width(cur.toString() + ch, font, size) > maxW && cur.length() > indent.length()) {
                        out.add(cur.toString());
                        cur.setLength(0);
                        cur.append(indent);
                    }
                    cur.append(ch);
                }
            }
            if (cur.length() > 0) out.add(cur.toString().stripTrailing());
        }
        return out;
    }

    /** Sostituisce caratteri senza equivalente WinAnsi con alternative ASCII. */
    static String normalize(String s) {
        if (s == null) return "";
        return s.replace("\r\n", "\n").replace('\r', '\n').replace("\t", "    ")
                .replace("→", "->").replace("←", "<-").replace("⇒", "=>")
                .replace("≤", "<=").replace("≥", ">=").replace("≠", "!=")
                .replace("✓", "OK").replace("✔", "OK").replace("✗", "X").replace("✕", "X")
                .replace("▲", "^").replace("▼", "v").replace(" ", " ");
    }

    /** Codice WinAnsi (0-255) del carattere, o '?' se non rappresentabile. */
    static int winAnsi(char c) {
        if (c < 128 || (c >= 0xA0 && c <= 0xFF)) return c;
        return switch (c) {
            case '€' -> 0x80; case '‚' -> 0x82; case 'ƒ' -> 0x83; case '„' -> 0x84; case '…' -> 0x85;
            case '†' -> 0x86; case '‡' -> 0x87; case 'ˆ' -> 0x88; case '‰' -> 0x89; case 'Š' -> 0x8A;
            case '‹' -> 0x8B; case 'Œ' -> 0x8C; case 'Ž' -> 0x8E; case '‘' -> 0x91; case '’' -> 0x92;
            case '“' -> 0x93; case '”' -> 0x94; case '•' -> 0x95; case '–' -> 0x96; case '—' -> 0x97;
            case '˜' -> 0x98; case '™' -> 0x99; case 'š' -> 0x9A; case '›' -> 0x9B; case 'œ' -> 0x9C;
            case 'ž' -> 0x9E; case 'Ÿ' -> 0x9F;
            default -> '?';
        };
    }

    /** Stringa letterale PDF: escape di ( ) \ e caratteri non ASCII in ottale. */
    static String literal(String s) {
        StringBuilder b = new StringBuilder("(");
        for (int k = 0; k < s.length(); k++) {
            char ch = s.charAt(k);
            if (Character.isHighSurrogate(ch)) { b.append('?'); k++; continue; }
            int c = winAnsi(ch);
            if (c == '(' || c == ')' || c == '\\') b.append('\\').append((char) c);
            else if (c < 32 || c > 126) b.append(String.format("\\%03o", c));
            else b.append((char) c);
        }
        return b.append(')').toString();
    }

    private void drawText(String s, Font f, float size, float x, float yy, float[] rgb) {
        float[] c = rgb == null ? new float[]{0, 0, 0} : rgb;
        write(String.format(Locale.ROOT, "BT %.3f %.3f %.3f rg /%s %.1f Tf %.2f %.2f Td %s Tj ET\n",
                c[0], c[1], c[2], f.res, size, x, yy, literal(s)));
    }

    private void write(String s) {
        page.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    // ---------------- serializzazione ----------------

    public byte[] toBytes() {
        int n = pages.size();
        // footer con numero di pagina
        for (int p = 0; p < n; p++) {
            page = pages.get(p);
            String foot = title + "  -  pagina " + (p + 1) + " di " + n;
            write(String.format(Locale.ROOT, "BT 0.45 0.45 0.45 rg /F1 8.0 Tf %.2f 30.00 Td %s Tj ET\n", MARGIN, literal(foot)));
        }
        int fontCount = Font.values().length;
        int firstPage = 3 + fontCount;
        int infoId = firstPage + 2 * n;
        int total = infoId; // oggetti 1..infoId

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        long[] offsets = new long[total + 1];
        ascii(out, "%PDF-1.4\n");
        out.writeBytes(new byte[]{'%', (byte) 0xE2, (byte) 0xE3, (byte) 0xCF, (byte) 0xD3, '\n'});

        offsets[1] = out.size();
        ascii(out, "1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n");

        StringBuilder kids = new StringBuilder();
        for (int p = 0; p < n; p++) kids.append(firstPage + 2 * p).append(" 0 R ");
        offsets[2] = out.size();
        ascii(out, "2 0 obj\n<< /Type /Pages /Kids [" + kids.toString().trim() + "] /Count " + n + " >>\nendobj\n");

        StringBuilder fontRes = new StringBuilder();
        for (Font f : Font.values()) {
            int id = 3 + f.ordinal();
            fontRes.append('/').append(f.res).append(' ').append(id).append(" 0 R ");
            offsets[id] = out.size();
            ascii(out, id + " 0 obj\n<< /Type /Font /Subtype /Type1 /BaseFont /" + f.base
                    + " /Encoding /WinAnsiEncoding >>\nendobj\n");
        }

        for (int p = 0; p < n; p++) {
            int pid = firstPage + 2 * p;
            int cid = pid + 1;
            byte[] content = pages.get(p).toByteArray();
            offsets[pid] = out.size();
            ascii(out, pid + " 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "
                    + fmt(PAGE_W) + " " + fmt(PAGE_H) + "] /Resources << /Font << " + fontRes.toString().trim()
                    + " >> >> /Contents " + cid + " 0 R >>\nendobj\n");
            offsets[cid] = out.size();
            ascii(out, cid + " 0 obj\n<< /Length " + content.length + " >>\nstream\n");
            out.writeBytes(content);
            ascii(out, "\nendstream\nendobj\n");
        }

        offsets[infoId] = out.size();
        ascii(out, infoId + " 0 obj\n<< /Title " + literal(title) + " /Producer (Java Quest 100 PdfWriter) >>\nendobj\n");

        long xref = out.size();
        StringBuilder x = new StringBuilder("xref\n0 " + (total + 1) + "\n0000000000 65535 f \n");
        for (int k = 1; k <= total; k++) x.append(String.format("%010d 00000 n \n", offsets[k]));
        x.append("trailer\n<< /Size ").append(total + 1).append(" /Root 1 0 R /Info ").append(infoId)
                .append(" 0 R >>\nstartxref\n").append(xref).append("\n%%EOF\n");
        ascii(out, x.toString());
        return out.toByteArray();
    }

    public void save(Path file) throws IOException {
        if (file.getParent() != null) Files.createDirectories(file.getParent());
        Files.write(file, toBytes());
    }

    private static String fmt(float f) {
        return String.format(Locale.ROOT, "%.2f", f);
    }

    private static void ascii(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    /** Verifica strutturale (usata dal selftest): header, xref coerente, EOF. */
    public static String validate(byte[] pdf) {
        String s = new String(pdf, StandardCharsets.ISO_8859_1);
        if (!s.startsWith("%PDF-1.")) return "header mancante";
        if (!s.stripTrailing().endsWith("%%EOF")) return "%%EOF mancante";
        int sx = s.lastIndexOf("startxref");
        if (sx < 0) return "startxref mancante";
        int xref = Integer.parseInt(s.substring(sx + 9).trim().split("\\s+")[0]);
        if (!s.startsWith("xref", xref)) return "offset xref errato";
        String[] lines = s.substring(xref).split("\n");
        int count = Integer.parseInt(lines[1].trim().split(" ")[1]);
        for (int k = 1; k < count; k++) {
            int off = Integer.parseInt(lines[2 + k].substring(0, 10));
            if (!s.startsWith(k + " 0 obj", off)) return "offset oggetto " + k + " errato";
        }
        if (!s.contains("/Type /Page ")) return "nessuna pagina";
        return null;
    }
}
