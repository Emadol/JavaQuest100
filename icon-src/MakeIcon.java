import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Genera icon.png (256) e icon.ico (16..256): "J" verde su sfondo nero. Uso: java MakeIcon.java <outDir> */
public class MakeIcon {
    static BufferedImage draw(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        double r = s * 0.22;
        Shape bg = new RoundRectangle2D.Double(0, 0, s, s, r * 2, r * 2);
        g.setColor(new Color(0x0a0a0a));
        g.fill(bg);
        g.setClip(bg);
        // bordo verde sottile
        g.setColor(new Color(0x00ff66));
        g.setStroke(new BasicStroke(Math.max(1f, s * 0.035f)));
        double b = s * 0.05;
        g.draw(new RoundRectangle2D.Double(b, b, s - 2 * b, s - 2 * b, r * 1.6, r * 1.6));
        // glow + lettera J
        Font f = new Font("Consolas", Font.BOLD, (int) (s * 0.72));
        if (!f.getFamily().equals("Consolas")) f = new Font(Font.MONOSPACED, Font.BOLD, (int) (s * 0.72));
        g.setFont(f);
        FontMetrics fm = g.getFontMetrics();
        int w = fm.stringWidth("J");
        float x = (s - w) / 2f;
        float y = (s - fm.getHeight()) / 2f + fm.getAscent() - s * 0.02f;
        for (int i = 6; i >= 1; i--) {
            g.setColor(new Color(0, 255, 102, 14));
            g.setStroke(new BasicStroke(i * s * 0.012f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.draw(f.createGlyphVector(g.getFontRenderContext(), "J").getOutline(x, y));
        }
        g.setColor(new Color(0x00ff66));
        g.drawString("J", x, y);
        g.dispose();
        return img;
    }

    public static void main(String[] a) throws Exception {
        File out = new File(a[0]);
        out.mkdirs();
        ImageIO.write(draw(256), "png", new File(out, "icon.png"));
        int[] sizes = {16, 24, 32, 48, 64, 128, 256};
        java.util.List<byte[]> pngs = new ArrayList<>();
        for (int s : sizes) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            ImageIO.write(draw(s), "png", bo);
            pngs.add(bo.toByteArray());
        }
        ByteArrayOutputStream ico = new ByteArrayOutputStream();
        ByteBuffer h = ByteBuffer.allocate(6 + 16 * sizes.length).order(ByteOrder.LITTLE_ENDIAN);
        h.putShort((short) 0).putShort((short) 1).putShort((short) sizes.length);
        int off = 6 + 16 * sizes.length;
        for (int i = 0; i < sizes.length; i++) {
            int s = sizes[i];
            h.put((byte) (s == 256 ? 0 : s)).put((byte) (s == 256 ? 0 : s)).put((byte) 0).put((byte) 0);
            h.putShort((short) 1).putShort((short) 32).putInt(pngs.get(i).length).putInt(off);
            off += pngs.get(i).length;
        }
        ico.write(h.array());
        for (byte[] p : pngs) ico.write(p);
        try (FileOutputStream fo = new FileOutputStream(new File(out, "icon.ico"))) { ico.writeTo(fo); }
        System.out.println("ok");
    }
}
