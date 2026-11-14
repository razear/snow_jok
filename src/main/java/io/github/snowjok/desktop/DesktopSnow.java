package io.github.snowjok.desktop;

import java.awt.AlphaComposite;
import java.awt.AWTException;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

import io.github.snowjok.i18n.Messages;
import io.github.snowjok.macos.MacWindows;
import io.github.snowjok.snow.SnowField;
import io.github.snowjok.vehicle.CleaningVehicle;

public final class DesktopSnow
{
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Color FLAKE_EDGE = new Color(0x587b9d);
    private static final Color FLAKE_FILL = new Color(0xf7fcff);
    private static final double LANDING_FADE_DISTANCE = 32;
    private static final int MARGIN = 8;
    private static final double SPEED = 250;
    private static final String SNOW_SPEED_KEY = "snowSpeedPercent";
    private static final String SNOW_DENSITY_KEY = "snowDensityPercent";
    private static final String VEHICLE_KEY = "cleaningVehicle";
    private static final String AUTO_CLEAN_KEY = "autoClean";
    private static final double AUTO_CLEAN_HEIGHT = SnowField.MAX_DEPTH * 0.60;

    private final JFrame snowWindow = overlay("Snow Jok • snow");
    private final JFrame bankWindow = overlay("Snow Jok • snowbank");
    private final JFrame vehicleWindow = overlay("Snow Jok • vehicle");
    private final Random random = new Random();
    private final SnowCanvas snowCanvas = new SnowCanvas();
    private final JPanel vehicleCanvas = new JPanel();
    private final List<Spray> spray = new ArrayList<>();
    private final Timer timer = new Timer(32, event -> tick());
    private final Preferences preferences;
    private final Consumer<Throwable> showError;

    private int snowSpeed;
    private int snowDensity;
    private CleaningVehicle vehicle;
    private SnowField snow;
    private MenuBarIcon menuBarIcon;
    private MacWindows macWindows;
    private BufferedImage bankImage;
    private BufferedImage vehicleImage;
    private double[] paintedDepth;
    private boolean vehicleImageDirty = true;
    private boolean overlayPositionDirty = true;
    private Rectangle workArea;
    private double x;
    private int direction = -1;
    private boolean driving;
    private boolean autoClean;
    private boolean hovered;
    private double travel;
    private long lastTick;
    private double layoutElapsed;

    public DesktopSnow(Preferences preferences, Consumer<Throwable> showError)
    {
        this.preferences = preferences;
        this.showError = showError;
        snowSpeed = MenuBarIcon.supportedSpeed(preferences.getInt(SNOW_SPEED_KEY, 100));
        snowDensity = MenuBarIcon.supportedDensity(preferences.getInt(SNOW_DENSITY_KEY, 100));
        vehicle = CleaningVehicle.fromPreference(preferences.get(VEHICLE_KEY, CleaningVehicle.EXCAVATOR.name()));
        autoClean = preferences.getBoolean(AUTO_CLEAN_KEY, false);
    }

    public void start()
    {
        try
        {
            snowWindow.setContentPane(snowCanvas);
            vehicleCanvas.setOpaque(false);
            vehicleWindow.setContentPane(vehicleCanvas);
            installControls();
            layout(screenBounds());
            // Create the native windows before showing them so AppKit can set their Space behavior first.
            snowWindow.addNotify();
            bankWindow.addNotify();
            vehicleWindow.addNotify();
            macWindows = new MacWindows(snowWindow.getTitle(), bankWindow.getTitle(), vehicleWindow.getTitle(), failure ->
            {
                if (failure != null)
                {
                    stop();
                    showError.accept(failure);
                    return;
                }
                try
                {
                    menuBarIcon = MenuBarIcon.install(snowSpeed, this::setSnowSpeed, snowDensity,
                            this::setSnowDensity, vehicle, this::setVehicle,
                            autoClean, this::setAutoClean, this::startCleaning, this::stop);
                }
                catch (AWTException failureToAddIcon)
                {
                    stop();
                    showError.accept(failureToAddIcon);
                    return;
                }
                snowWindow.setVisible(true);
                bankWindow.setVisible(true);
                vehicleWindow.setVisible(true);
                lastTick = System.nanoTime();
                presentOverlays();
                timer.start();
            }, failure ->
            {
                if (macWindows != null)
                {
                    stop();
                    showError.accept(failure);
                }
            });
            macWindows.configure();
        }
        catch (RuntimeException | LinkageError failure)
        {
            stop();
            throw failure;
        }
    }

    private static JFrame overlay(String title)
    {
        JFrame window = new JFrame(title);
        window.setUndecorated(true);
        window.setResizable(false);
        window.setBackground(TRANSPARENT);
        window.setFocusableWindowState(false);
        window.setAutoRequestFocus(false);
        window.setAlwaysOnTop(true);
        window.getRootPane().putClientProperty("Window.shadow", false);
        return window;
    }

    private void installControls()
    {
        vehicleCanvas.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        vehicleCanvas.getAccessibleContext().setAccessibleName(vehicle.label());

        JPopupMenu menu = new JPopupMenu();
        JMenuItem quit = new JMenuItem(Messages.text("menu.vehicleQuit"));
        quit.addActionListener(event -> stop());
        menu.add(quit);
        vehicleCanvas.setComponentPopupMenu(menu);
        vehicleCanvas.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent event)
            {
                if (SwingUtilities.isLeftMouseButton(event) && !event.isPopupTrigger())
                {
                    startCleaning();
                }
            }

            @Override
            public void mouseEntered(MouseEvent event)
            {
                hovered = true;
                vehicleImageDirty = true;
            }

            @Override
            public void mouseExited(MouseEvent event)
            {
                hovered = false;
                vehicleImageDirty = true;
            }
        });
    }

    private void setSnowSpeed(int percent)
    {
        snowSpeed = percent;
        preferences.putInt(SNOW_SPEED_KEY, percent);
    }

    private void setSnowDensity(int percent)
    {
        snowDensity = percent;
        snow.setDensity(percent);
        preferences.putInt(SNOW_DENSITY_KEY, percent);
    }

    private void setVehicle(CleaningVehicle selected)
    {
        if (vehicle != selected)
        {
            vehicle = selected;
            vehicleCanvas.getAccessibleContext().setAccessibleName(vehicle.label());
            vehicleImageDirty = true;
            preferences.put(VEHICLE_KEY, vehicle.name());
        }
    }

    private void setAutoClean(boolean enabled)
    {
        autoClean = enabled;
        preferences.putBoolean(AUTO_CLEAN_KEY, enabled);
    }

    private void startCleaning()
    {
        driving = true;
    }

    private void stop()
    {
        timer.stop();
        if (macWindows != null)
        {
            macWindows.close();
            macWindows = null;
        }
        if (menuBarIcon != null)
        {
            menuBarIcon.remove();
            menuBarIcon = null;
        }
        vehicleWindow.dispose();
        bankWindow.dispose();
        snowWindow.dispose();
    }

    private static Rectangle screenBounds()
    {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
                .getDefaultConfiguration().getBounds();
    }

    private void layout(Rectangle bounds)
    {
        if (workArea == null)
        {
            x = rightLimit(bounds.width);
        }
        else
        {
            x = Math.clamp(x, MARGIN, rightLimit(bounds.width));
        }
        if (workArea == null || workArea.width != bounds.width || workArea.height != bounds.height)
        {
            snow = new SnowField(bounds.width, bounds.height, snowDensity);
            spray.clear();
            paintedDepth = null;
        }
        workArea = bounds;
        snowWindow.setBounds(bounds);
        int bankHeight = Math.min(SnowField.MAX_DEPTH, bounds.height);
        bankWindow.setBounds(bounds.x, bounds.y + bounds.height - bankHeight, bounds.width, bankHeight);
        vehicleWindow.setBounds(bounds.x + (int)Math.round(x),
                bounds.y + bounds.height - CleaningVehicle.HEIGHT, CleaningVehicle.WIDTH, CleaningVehicle.HEIGHT);
        overlayPositionDirty = true;
    }

    private void tick()
    {
        long now = System.nanoTime();
        double elapsed = (now - lastTick) / 1_000_000_000.0;
        lastTick = now;
        layoutElapsed += elapsed;
        if (layoutElapsed >= 1)
        {
            layoutElapsed = 0;
            Rectangle bounds = screenBounds();
            if (!bounds.equals(workArea))
            {
                layout(bounds);
            }
        }

        double delta = Math.min(elapsed, 0.06);
        snow.advance(delta, snowSpeed / 100.0);
        for (int index = spray.size() - 1; index >= 0; index--)
        {
            Spray next = spray.get(index).advance(delta);
            if (next.life() <= 0)
            {
                spray.remove(index);
            }
            else
            {
                spray.set(index, next);
            }
        }
        if (autoClean && !driving && snow.exceedsHeight(AUTO_CLEAN_HEIGHT))
        {
            startCleaning();
        }
        if (driving)
        {
            drive(delta);
            vehicleImageDirty = true;
        }
        snowCanvas.repaint();
        presentOverlays();
    }

    private void presentOverlays()
    {
        boolean bankChanged = !Arrays.equals(paintedDepth, snow.depth());
        if (bankChanged)
        {
            bankImage = new BufferedImage(workArea.width, bankWindow.getHeight(), BufferedImage.TYPE_INT_ARGB_PRE);
            paintSnowbank(bankImage, snow);
            paintedDepth = snow.depth().clone();
        }
        if (vehicleImageDirty)
        {
            vehicleImage = new BufferedImage(CleaningVehicle.WIDTH, CleaningVehicle.HEIGHT,
                    BufferedImage.TYPE_INT_ARGB_PRE);
            Graphics2D g = vehicleImage.createGraphics();
            vehicle.paint(g, direction, driving, hovered, travel);
            g.dispose();
        }
        if (bankChanged || vehicleImageDirty || overlayPositionDirty)
        {
            // The bottom of the primary display is y=0 in AppKit screen coordinates.
            macWindows.present(new MacWindows.OverlayFrame(bankImage, vehicleImage, workArea.x + x, 0));
            vehicleImageDirty = false;
            overlayPositionDirty = false;
        }
    }

    private void drive(double delta)
    {
        double right = rightLimit(workArea.width);
        double oldX = x;
        x = Math.clamp(x + direction * SPEED * delta, MARGIN, right);
        travel += Math.abs(x - oldX);

        double front = direction < 0 ? x + MARGIN : x + CleaningVehicle.WIDTH - MARGIN;
        double from = Math.min(x, oldX);
        double to = Math.max(x, oldX) + CleaningVehicle.WIDTH;
        boolean reachedEdge = direction < 0 ? x <= MARGIN : x >= right;
        if (reachedEdge)
        {
            if (direction < 0)
            {
                from = 0;
            }
            else
            {
                to = workArea.width;
            }
        }
        double removed = snow.clear(from, to);
        if (removed > 0)
        {
            scatter(front, removed);
        }
        if (reachedEdge)
        {
            driving = false;
            direction = -direction;
        }
    }

    private static int rightLimit(int width)
    {
        return Math.max(MARGIN, width - CleaningVehicle.WIDTH - MARGIN);
    }

    private void scatter(double front, double amount)
    {
        int count = (int)Math.clamp(amount / 8, 1, 12);
        for (int index = 0; index < count; index++)
        {
            spray.add(new Spray(front + random.nextDouble(-5, 5),
                    workArea.height - random.nextDouble(5, 17),
                    direction * random.nextDouble(38, 130), -random.nextDouble(35, 112),
                    random.nextDouble(1, 4), random.nextDouble(0.20, 0.42)));
        }
    }

    private record Spray(double x, double y, double vx, double vy, double radius, double life)
    {
        Spray advance(double delta)
        {
            return new Spray(x + vx * delta, y + vy * delta, vx, vy + 230 * delta, radius, life - delta);
        }
    }

    private final class SnowCanvas extends JComponent
    {
        private BufferedImage frame;

        @Override
        protected void paintComponent(Graphics graphics)
        {
            int width = getWidth();
            int height = getHeight();
            if (snow == null || width == 0 || height == 0)
            {
                return;
            }
            if (frame == null || frame.getWidth() != width || frame.getHeight() != height)
            {
                frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB_PRE);
            }

            Graphics2D g = frame.createGraphics();
            prepareFrame(g, width, height);
            for (SnowField.Flake flake : snow.flakes())
            {
                double gap = height - snow.surfaceDepth(flake.x()) - flake.y() - flake.radius();
                double visibility = Math.clamp(gap / LANDING_FADE_DISTANCE, 0, 1);
                int alpha = (int)Math.round(flake.opacity() * visibility);
                if (alpha > 0)
                {
                    dot(g, flake.x(), flake.y(), flake.radius(), alpha);
                }
            }
            for (Spray particle : spray)
            {
                dot(g, particle.x(), particle.y(), particle.radius(),
                        (int)Math.min(245, particle.life() * 1_350));
            }
            g.dispose();

            paintFrame(graphics, frame);
        }
    }

    private static void prepareFrame(Graphics2D g, int width, int height)
    {
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }

    private static void paintFrame(Graphics graphics, BufferedImage image)
    {
        // Replace the previous frame, including pixels that have become transparent.
        Graphics2D output = (Graphics2D)graphics.create();
        output.setComposite(AlphaComposite.Src);
        output.drawImage(image, 0, 0, null);
        output.dispose();
    }

    private static void paintSnowbank(BufferedImage bank, SnowField snow)
    {
        int width = bank.getWidth();
        int height = bank.getHeight();
        int[] pixels = ((DataBufferInt)bank.getRaster().getDataBuffer()).getData();
        for (int x = 0; x < width; x++)
        {
            double surface = Math.max(0, height - snow.surfaceDepth(x + 0.5));
            int edgeY = (int)surface;
            for (int y = 0; y < height; y++)
            {
                int red = 242 - 73 * y / height;
                int green = 250 - 43 * y / height;
                int blue = 255 - 21 * y / height;
                int pixel = 0;
                if (y > edgeY)
                {
                    pixel = 0xff000000 | (red << 16) | (green << 8) | blue;
                }
                else if (y == edgeY)
                {
                    // Store the exact fractional coverage of the topmost snow pixel.
                    int alpha = (int)Math.round(255 * (y + 1 - surface));
                    pixel = (alpha << 24) | (red * alpha / 255 << 16)
                            | (green * alpha / 255 << 8) | blue * alpha / 255;
                }
                pixels[y * width + x] = pixel;
            }
        }
    }

    private static void dot(Graphics2D g, double x, double y, double radius, int alpha)
    {
        int diameter = (int)Math.ceil(radius * 2);
        int left = (int)Math.round(x - radius);
        int top = (int)Math.round(y - radius);
        g.setComposite(AlphaComposite.SrcOver.derive(alpha / 255f));
        g.setColor(FLAKE_EDGE);
        g.fillOval(left - 1, top - 1, diameter + 2, diameter + 2);
        g.setColor(FLAKE_FILL);
        g.fillOval(left, top, diameter, diameter);
        g.setComposite(AlphaComposite.SrcOver);
    }
}
