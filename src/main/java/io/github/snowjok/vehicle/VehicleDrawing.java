package io.github.snowjok.vehicle;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Path2D;

final class VehicleDrawing
{
    static final Color INK = new Color(0x182534);

    private VehicleDrawing()
    {
    }

    static void shadow(Graphics2D g, int left, int width)
    {
        g.setColor(new Color(17, 29, 46, 90));
        g.fillOval(left, 89, width, 9);
    }

    static void outlined(Graphics2D g, Shape shape, Color fill)
    {
        g.setColor(fill);
        g.fill(shape);
        g.setColor(INK);
        g.draw(shape);
    }

    static Path2D polygon(double... points)
    {
        Path2D path = new Path2D.Double();
        path.moveTo(points[0], points[1]);
        for (int i = 2; i < points.length; i += 2)
        {
            path.lineTo(points[i], points[i + 1]);
        }
        path.closePath();
        return path;
    }
}
