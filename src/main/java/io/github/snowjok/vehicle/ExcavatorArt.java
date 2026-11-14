package io.github.snowjok.vehicle;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

import static io.github.snowjok.vehicle.VehicleDrawing.INK;
import static io.github.snowjok.vehicle.VehicleDrawing.polygon;

final class ExcavatorArt
{
    private static final Color STEEL = new Color(0x486174);
    private static final Color GOLD = new Color(0xe89223);
    private static final Color YELLOW = new Color(0xffc758);
    private static final Color LIGHT = new Color(0xffe7a0);

    private ExcavatorArt()
    {
    }

    static void paint(Graphics2D g, boolean driving, boolean hovered, double travel)
    {
        g.setColor(new Color(17, 29, 46, hovered ? 125 : 90));
        g.fillOval(61, 86, 103, 12);
        g.fillOval(6, 90, 46, 6);
        tracks(g, driving, travel);
        body(g, hovered);
        arm(g, driving, travel);
    }

    private static void tracks(Graphics2D g, boolean driving, double travel)
    {
        g.setColor(INK);
        g.fillRoundRect(63, 70, 101, 24, 24, 24);
        g.setColor(STEEL);
        g.drawRoundRect(66, 73, 95, 18, 18, 18);
        for (int x = 75; x <= 151; x += 15)
        {
            g.setColor(new Color(0x344454));
            g.fillOval(x - 5, 77, 10, 10);
            g.setColor(new Color(0x8194a1));
            g.fillOval(x - 2, 80, 4, 4);
        }
        int offset = driving ? (int)travel % 8 : 0;
        g.setColor(new Color(0x9cabb5));
        for (int x = 78; x <= 151; x += 8)
        {
            g.drawLine(x - offset, 72, x - offset + 3, 72);
            g.drawLine(x + offset - 3, 92, x + offset, 92);
        }
    }

    private static void body(Graphics2D g, boolean hovered)
    {
        g.setColor(INK);
        g.fillRoundRect(80, 62, 77, 14, 8, 8);
        g.setColor(GOLD);
        g.fillRoundRect(84, 66, 68, 6, 5, 5);

        g.setPaint(new GradientPaint(0, 41, YELLOW, 0, 69, GOLD));
        g.fillRoundRect(73, 41, 90, 27, 12, 12);
        g.setColor(INK);
        g.drawRoundRect(73, 41, 90, 27, 12, 12);
        g.setColor(LIGHT);
        g.drawLine(116, 45, 154, 45);
        g.setColor(new Color(0x714619));
        for (int x = 138; x <= 150; x += 6)
        {
            g.drawLine(x, 51, x, 58);
        }

        Path2D cab = polygon(82, 60, 82, 28, 92, 15, 119, 15, 124, 22, 124, 61);
        g.setColor(hovered ? LIGHT : YELLOW);
        g.fill(cab);
        g.setColor(INK);
        g.draw(cab);
        Path2D glass = polygon(88, 48, 88, 30, 95, 21, 116, 21, 118, 25, 118, 48);
        g.setPaint(new GradientPaint(91, 21, new Color(0xa9e3ef), 107, 49, new Color(0x32667d)));
        g.fill(glass);
        g.setColor(INK);
        g.draw(glass);
        g.drawLine(107, 22, 107, 48);
        g.setColor(new Color(221, 248, 255, 180));
        g.drawLine(91, 36, 101, 25);
        g.setColor(STEEL);
        g.drawLine(110, 54, 116, 54);

        g.setColor(INK);
        g.fillRoundRect(102, 9, 12, 8, 4, 4);
        g.setColor(hovered ? LIGHT : new Color(0xffad38));
        g.fillRoundRect(104, 8, 8, 6, 3, 3);
    }

    private static void arm(Graphics2D g, boolean driving, double travel)
    {
        int lift = driving ? (int)Math.round(Math.sin(travel * 0.09) * 2) : -8;
        int jointY = 72 + lift;
        Path2D boom = polygon(82, 55, 54, 25, 44, 23, 40, 31, 75, 63, 83, 65, 88, 60);
        g.setColor(GOLD);
        g.fill(boom);
        g.setColor(INK);
        g.draw(boom);
        g.setColor(LIGHT);
        g.drawLine(50, 29, 76, 54);

        Path2D stick = polygon(42, 24, 32, jointY, 40, jointY + 4, 52, 30, 48, 24);
        g.setColor(YELLOW);
        g.fill(stick);
        g.setColor(INK);
        g.draw(stick);
        g.setColor(STEEL);
        g.drawLine(67, 56, 48, 37);
        g.drawLine(47, 39, 39, jointY - 4);

        Path2D bucket = polygon(38, jointY - 1, 48, jointY + 5, 45, jointY + 15,
                36, jointY + 22, 16, jointY + 22, 7, jointY + 16,
                23, jointY + 14, 27, jointY + 4);
        g.setColor(GOLD);
        g.fill(bucket);
        g.setColor(INK);
        g.draw(bucket);
        g.setColor(STEEL);
        g.drawLine(19, jointY + 19, 35, jointY + 19);

        g.setColor(INK);
        g.fillOval(76, 54, 9, 9);
        g.fillOval(42, 25, 8, 8);
        g.fillOval(34, jointY - 2, 7, 7);
        g.setColor(LIGHT);
        g.fillOval(79, 57, 3, 3);
        g.fillOval(44, 27, 4, 4);
    }
}
