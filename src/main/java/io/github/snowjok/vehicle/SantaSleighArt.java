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

final class SantaSleighArt
{
    private static final Color WHITE = new Color(0xf5fbff);
    private static final Color GOLD = new Color(0xffce6e);
    private static final Color RED = new Color(0xc92d47);
    private static final Color BRIGHT_RED = new Color(0xf34c59);
    private static final Color GREEN = new Color(0x277a66);

    private SantaSleighArt()
    {
    }

    static void paint(Graphics2D g, boolean hovered, double travel)
    {
        shadow(g, 12, 151);
        g.setColor(INK);
        g.drawLine(52, 77, 46, 91);
        g.drawLine(128, 78, 134, 91);
        Path2D runner = new Path2D.Double();
        runner.moveTo(12, 80);
        runner.curveTo(7, 92, 23, 94, 39, 94);
        runner.lineTo(157, 94);
        g.setColor(new Color(0xa4652b));
        g.draw(runner);
        g.translate(0, -2);
        g.setColor(GOLD);
        g.draw(runner);
        g.translate(0, 2);

        outlined(g, new RoundRectangle2D.Double(116, 44, 24, 25, 4, 4), GREEN);
        outlined(g, new RoundRectangle2D.Double(138, 51, 20, 18, 4, 4), new Color(0x548ddd));
        g.setColor(GOLD);
        g.drawLine(127, 45, 127, 64);
        g.drawLine(118, 52, 138, 52);
        g.drawLine(147, 52, 147, 65);
        santa(g, Math.sin(travel * 0.09) * 4);

        Path2D body = new Path2D.Double();
        body.moveTo(19, 57);
        body.curveTo(27, 65, 38, 68, 57, 68);
        body.lineTo(146, 68);
        body.quadTo(157, 66, 160, 59);
        body.lineTo(163, 74);
        body.quadTo(157, 85, 138, 85);
        body.lineTo(51, 85);
        body.quadTo(24, 83, 19, 57);
        body.closePath();
        outlined(g, body, hovered ? BRIGHT_RED : RED);
        g.setColor(GOLD);
        g.drawLine(53, 75, 149, 75);
        g.fillOval(99, 72, 7, 7);
    }

    private static void santa(Graphics2D g, double wave)
    {
        outlined(g, new RoundRectangle2D.Double(63, 43, 41, 31, 18, 18), BRIGHT_RED);
        outlined(g, polygon(74, 47, 62, 47, 50, 41 + wave, 46, 48 + wave, 60, 57, 76, 57), RED);
        outlined(g, new Ellipse2D.Double(42, 39 + wave, 12, 11), WHITE);
        outlined(g, new Ellipse2D.Double(63, 22, 30, 30), new Color(0xffc3a3));
        outlined(g, polygon(62, 26, 68, 12, 81, 8, 99, 23, 91, 29), BRIGHT_RED);
        outlined(g, new RoundRectangle2D.Double(61, 21, 34, 9, 7, 7), WHITE);
        outlined(g, new Ellipse2D.Double(94, 20, 9, 9), WHITE);
        outlined(g, new Ellipse2D.Double(62, 36, 27, 18), WHITE);
        g.setColor(INK);
        g.fillOval(67, 31, 3, 3);
        g.setColor(new Color(0xffc3a3));
        g.fillOval(60, 33, 8, 6);
    }
}
