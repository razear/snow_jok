package io.github.snowjok.snow;

import java.util.Arrays;
import java.util.Random;

public final class SnowField
{
    private static final int COLUMN_WIDTH = 14;
    public static final int MAX_DEPTH = 90;
    private static final double MAX_GROWTH_PER_SECOND = 12;

    public record Flake(double x, double y, double radius, double speed, double drift, double phase, int opacity)
    {
    }

    private final Random random = new Random();
    private final int width;
    private final int height;
    private final double[] depth;
    private final double[] settledDepth;
    private final int baseDensity;
    private Flake[] flakes = new Flake[0];
    private double time;

    public SnowField(int width, int height, int densityPercent)
    {
        this.width = width;
        this.height = height;
        depth = new double[(width + COLUMN_WIDTH - 1) / COLUMN_WIDTH];
        settledDepth = new double[depth.length];
        baseDensity = (int)Math.clamp(width * (double)height / 5_000, 65, 620);
        setDensity(densityPercent);
    }

    public void setDensity(int percent)
    {
        int count = Math.round(baseDensity * percent / 100f);
        int previousCount = flakes.length;
        if (count == previousCount)
        {
            return;
        }
        flakes = Arrays.copyOf(flakes, count);
        for (int index = previousCount; index < count; index++)
        {
            flakes[index] = newFlake(random.nextDouble(2));
        }
    }

    public void advance(double delta, double speedMultiplier)
    {
        time += delta;
        for (int index = 0; index < depth.length; index++)
        {
            depth[index] = Math.min(settledDepth[index], depth[index] + MAX_GROWTH_PER_SECOND * delta);
        }
        for (int index = 0; index < flakes.length; index++)
        {
            Flake flake = flakes[index];
            double wind = 9 + Math.sin(time * 1.5 + flake.phase()) * flake.drift();
            double x = flake.x() + wind * delta;
            if (x < 0)
            {
                x += width;
            }
            else if (x >= width)
            {
                x -= width;
            }
            double y = flake.y() + flake.speed() * speedMultiplier * delta;
            int column = (int)x / COLUMN_WIDTH;
            if (y + flake.radius() >= height - surfaceDepth(x))
            {
                deposit(column, 0.65 + flake.radius() * 0.9);
                flakes[index] = newFlake(0);
            }
            else
            {
                flakes[index] = new Flake(x, y, flake.radius(), flake.speed(),
                        flake.drift(), flake.phase(), flake.opacity());
            }
        }
    }

    private Flake newFlake(double delay)
    {
        double radius = random.nextDouble(1.2, 3.8);
        double speed = 105 + radius * 38 + random.nextDouble(48);
        return new Flake(random.nextDouble(width), -radius - 1 - delay * speed, radius, speed,
                random.nextDouble(7, 38), random.nextDouble(Math.TAU), random.nextInt(200, 256));
    }

    private void deposit(int column, double amount)
    {
        settledDepth[column] = Math.min(MAX_DEPTH, settledDepth[column] + amount);
        if (column > 0)
        {
            settledDepth[column - 1] = Math.min(MAX_DEPTH, settledDepth[column - 1] + amount * 0.26);
        }
        if (column + 1 < depth.length)
        {
            settledDepth[column + 1] = Math.min(MAX_DEPTH, settledDepth[column + 1] + amount * 0.26);
        }
    }

    private double columnCenter(int index)
    {
        return Math.min(width, (index + 0.5) * COLUMN_WIDTH);
    }

    public double surfaceDepth(double x)
    {
        int left = (int)Math.floor(x / COLUMN_WIDTH - 0.5);
        if (left < 0)
        {
            return depth[0];
        }
        if (left >= depth.length - 1)
        {
            return depth[depth.length - 1];
        }
        double start = columnCenter(left);
        double fraction = (x - start) / (columnCenter(left + 1) - start);
        // Blend smoothly between the adjacent column heights.
        double blend = fraction * fraction * (3 - 2 * fraction);
        return Math.fma(depth[left + 1] - depth[left], blend, depth[left]);
    }

    public double clear(double from, double to)
    {
        int first = Math.max(0, (int)Math.floor(from / COLUMN_WIDTH));
        int last = Math.min(depth.length - 1, (int)Math.floor(to / COLUMN_WIDTH));
        double removed = 0;
        for (int index = first; index <= last; index++)
        {
            removed += settledDepth[index];
            settledDepth[index] = 0;
            depth[index] = 0;
        }
        return removed;
    }

    public boolean exceedsHeight(double height)
    {
        for (double column : depth)
        {
            if (column > height)
            {
                return true;
            }
        }
        return false;
    }

    public double[] depth()
    {
        return depth;
    }

    public Flake[] flakes()
    {
        return flakes;
    }
}
