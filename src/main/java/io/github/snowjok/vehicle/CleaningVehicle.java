package io.github.snowjok.vehicle;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import io.github.snowjok.i18n.Messages;

public enum CleaningVehicle
{
    EXCAVATOR,
    SNOWPLOW,
    SNOWMOBILE,
    SANTA_SLEIGH;

    public static final int WIDTH = 172;
    public static final int HEIGHT = 100;

    public String label()
    {
        return Messages.text("vehicle." + name());
    }

    public static CleaningVehicle fromPreference(String value)
    {
        for (CleaningVehicle vehicle : values())
        {
            if (vehicle.name().equals(value))
            {
                return vehicle;
            }
        }
        return EXCAVATOR;
    }

    public void paint(Graphics2D canvas, int direction, boolean driving, boolean hovered, double travel)
    {
        Graphics2D g = (Graphics2D)canvas.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (direction > 0)
        {
            g.translate(WIDTH, 0);
            g.scale(-1, 1);
        }

        switch (this)
        {
            case EXCAVATOR -> ExcavatorArt.paint(g, driving, hovered, travel);
            case SNOWPLOW -> SnowplowArt.paint(g, hovered, driving ? travel : 0);
            case SNOWMOBILE -> SnowmobileArt.paint(g, hovered, driving ? travel : 0);
            case SANTA_SLEIGH -> SantaSleighArt.paint(g, hovered, driving ? travel : 0);
        }
        g.dispose();
    }
}
