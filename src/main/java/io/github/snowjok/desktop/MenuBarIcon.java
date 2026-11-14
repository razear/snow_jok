package io.github.snowjok.desktop;

import java.awt.AWTException;
import java.awt.BasicStroke;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Menu;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ItemEvent;
import java.awt.geom.Line2D;
import java.awt.image.BaseMultiResolutionImage;
import java.awt.image.BufferedImage;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

import io.github.snowjok.i18n.Messages;
import io.github.snowjok.vehicle.CleaningVehicle;

final class MenuBarIcon
{
    private static final int SIZE = 18;
    private static final int[] SPEED_PRESETS = {25, 50, 75, 100, 125, 150, 200};
    private static final int[] DENSITY_PRESETS = {25, 50, 75, 100, 150, 200};

    private final TrayIcon icon;

    private MenuBarIcon(TrayIcon icon)
    {
        this.icon = icon;
    }

    static MenuBarIcon install(int speed, IntConsumer changeSpeed, int density, IntConsumer changeDensity,
            CleaningVehicle vehicle, Consumer<CleaningVehicle> changeVehicle,
            boolean autoClean, Consumer<Boolean> changeAutoClean, Runnable startCleaning, Runnable quit)
            throws AWTException
    {
        PopupMenu menu = new PopupMenu();
        menu.add(percentMenu(Messages.text("menu.speed"), SPEED_PRESETS, speed, changeSpeed));
        menu.add(percentMenu(Messages.text("menu.density"), DENSITY_PRESETS, density, changeDensity));
        Menu vehicles = new Menu(Messages.text("menu.vehicles"));
        for (CleaningVehicle option : CleaningVehicle.values())
        {
            addChoice(vehicles, option.label(), option == vehicle, () -> changeVehicle.accept(option));
        }
        menu.add(vehicles);
        menu.addSeparator();
        MenuItem clean = new MenuItem(Messages.text("menu.clean"));
        clean.addActionListener(event -> startCleaning.run());
        menu.add(clean);
        CheckboxMenuItem automatic = new CheckboxMenuItem(Messages.text("menu.autoClean"), autoClean);
        automatic.addItemListener(event -> changeAutoClean.accept(event.getStateChange() == ItemEvent.SELECTED));
        menu.add(automatic);
        menu.addSeparator();
        MenuItem exit = new MenuItem(Messages.text("menu.quit"));
        exit.addActionListener(event -> quit.run());
        menu.add(exit);

        TrayIcon icon = new TrayIcon(new BaseMultiResolutionImage(snowflake(1), snowflake(2)),
                Messages.text("app.name"), menu);
        SystemTray.getSystemTray().add(icon);
        return new MenuBarIcon(icon);
    }

    private static Menu percentMenu(String title, int[] values, int current, IntConsumer onChange)
    {
        Menu menu = new Menu(title);
        for (int value : values)
        {
            addChoice(menu, value + " %", value == current, () -> onChange.accept(value));
        }
        return menu;
    }

    private static void addChoice(Menu menu, String label, boolean selected, Runnable choose)
    {
        CheckboxMenuItem item = new CheckboxMenuItem(label, selected);
        item.addItemListener(event ->
        {
            for (int index = 0; index < menu.getItemCount(); index++)
            {
                CheckboxMenuItem choice = (CheckboxMenuItem)menu.getItem(index);
                choice.setState(choice == item);
            }
            choose.run();
        });
        menu.add(item);
    }

    static int supportedSpeed(int value)
    {
        return supportedValue(SPEED_PRESETS, value);
    }

    static int supportedDensity(int value)
    {
        return supportedValue(DENSITY_PRESETS, value);
    }

    private static int supportedValue(int[] values, int value)
    {
        for (int supported : values)
        {
            if (supported == value)
            {
                return value;
            }
        }
        return 100;
    }

    void remove()
    {
        SystemTray.getSystemTray().remove(icon);
    }

    private static BufferedImage snowflake(int scale)
    {
        BufferedImage image = new BufferedImage(SIZE * scale, SIZE * scale, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.scale(scale, scale);
        g.translate(SIZE / 2.0, SIZE / 2.0);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.45f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        for (int arm = 0; arm < 6; arm++)
        {
            g.draw(new Line2D.Double(0, 0, 0, -7));
            g.draw(new Line2D.Double(0, -4.3, -2, -6));
            g.draw(new Line2D.Double(0, -4.3, 2, -6));
            g.rotate(Math.TAU / 6);
        }
        g.dispose();
        return image;
    }
}
