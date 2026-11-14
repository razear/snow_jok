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

final class SnowmobileArt
{
    private static final Color STEEL = new Color(0x547087);
    private static final Color ICE = new Color(0xa7e5ef);
    private static final Color WHITE = new Color(0xf5fbff);
    private static final Color GOLD = new Color(0xffce6e);
    private static final Color RED = new Color(0xc92d47);

    private SnowmobileArt()
    {
    }

    static void paint(Graphics2D g, boolean hovered, double travel)
    {
        shadow(g, 12, 150);
        outlined(g, new RoundRectangle2D.Double(67, 72, 92, 21, 20, 20), INK);
        g.setColor(STEEL);
        int tread = (int)travel % 12;
        for (int x = 79; x <= 146; x += 12)
        {
            g.fillOval(x, 77, 9, 9);
            g.drawLine(x - tread / 2, 89, x + 5 - tread / 2, 89);
        }
        g.setColor(STEEL);
        g.drawLine(50, 67, 43, 89);
        g.drawLine(56, 67, 48, 89);
        Path2D ski = new Path2D.Double();
        ski.moveTo(10, 83);
        ski.quadTo(11, 93, 27, 93);
        ski.lineTo(60, 93);
        g.setColor(INK);
        g.draw(ski);

        outlined(g, polygon(40, 68, 50, 55, 69, 52, 84, 60, 110, 62, 144, 60, 155, 68, 145, 80,
                75, 80, 64, 74, 42, 75), hovered ? new Color(0x61e3d8) : new Color(0x279d9e));
        outlined(g, polygon(56, 54, 58, 44, 66, 49, 71, 54), ICE);
        g.setColor(WHITE);
        g.drawLine(72, 68, 135, 68);
        outlined(g, polygon(91, 45, 108, 51, 118, 64, 107, 68, 97, 57, 87, 55), STEEL);
        outlined(g, polygon(77, 36, 86, 31, 99, 41, 103, 53, 90, 56, 83, 45, 74, 49, 69, 44), RED);
        outlined(g, new Ellipse2D.Double(70, 19, 21, 20), GOLD);
        g.setColor(INK);
        g.fillRoundRect(70, 27, 13, 7, 5, 5);
        g.drawLine(66, 49, 74, 46);
    }
}
