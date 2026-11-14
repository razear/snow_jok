package io.github.snowjok.vehicle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import static io.github.snowjok.vehicle.VehicleDrawing.INK;
import static io.github.snowjok.vehicle.VehicleDrawing.outlined;
import static io.github.snowjok.vehicle.VehicleDrawing.polygon;
import static io.github.snowjok.vehicle.VehicleDrawing.shadow;

final class SnowplowArt
{
    private static final Color STEEL = new Color(0x547087);
    private static final Color ICE = new Color(0xa7e5ef);
    private static final Color WHITE = new Color(0xf5fbff);
    private static final Color GOLD = new Color(0xffce6e);

    private SnowplowArt()
    {
    }

    static void paint(Graphics2D g, boolean hovered, double travel)
    {
        shadow(g, 9, 153);
        outlined(g, new RoundRectangle2D.Double(40, 63, 122, 19, 7, 7), STEEL);
        outlined(g, polygon(35, 74, 35, 51, 49, 44, 57, 28, 89, 28, 99, 41, 99, 75),
                hovered ? new Color(0x60a8ff) : new Color(0x337cce));
        outlined(g, polygon(52, 49, 61, 34, 84, 34, 91, 42, 91, 49), ICE);
        g.setColor(STEEL);
        g.drawLine(73, 35, 73, 48);
        outlined(g, new RoundRectangle2D.Double(96, 44, 67, 32, 8, 8), new Color(0x285c97));
        g.setColor(ICE);
        g.drawLine(103, 50, 154, 50);
        g.setColor(GOLD);
        g.fillRoundRect(66, 23, 15, 6, 3, 3);
        g.fillRoundRect(35, 54, 11, 8, 3, 3);
        wheel(g, 65, travel);
        wheel(g, 138, travel);

        outlined(g, polygon(23, 70, 45, 71, 45, 79, 24, 83), STEEL);
        Path2D blade = new Path2D.Double();
        blade.moveTo(11, 60);
        blade.lineTo(34, 61);
        blade.quadTo(28, 79, 11, 88);
        blade.lineTo(5, 88);
        blade.quadTo(14, 76, 11, 60);
        blade.closePath();
        outlined(g, blade, GOLD);
        g.setColor(new Color(0x9a581a));
        g.drawLine(17, 65, 26, 66);
        g.drawLine(15, 72, 23, 73);
    }

    private static void wheel(Graphics2D g, int center, double travel)
    {
        outlined(g, new Ellipse2D.Double(center - 13, 68, 26, 26), INK);
        outlined(g, new Ellipse2D.Double(center - 8, 73, 16, 16), STEEL);
        double angle = travel / 13;
        int dx = (int)Math.round(Math.cos(angle) * 5);
        int dy = (int)Math.round(Math.sin(angle) * 5);
        g.setColor(WHITE);
        g.drawLine(center - dx, 81 - dy, center + dx, 81 + dy);
    }
}
