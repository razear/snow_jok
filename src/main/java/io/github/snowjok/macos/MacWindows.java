package io.github.snowjok.macos;

import java.awt.image.BufferedImage;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import static java.lang.foreign.ValueLayout.ADDRESS;
import static java.lang.foreign.ValueLayout.JAVA_BOOLEAN;
import static java.lang.foreign.ValueLayout.JAVA_DOUBLE;
import static java.lang.foreign.ValueLayout.JAVA_INT;
import static java.lang.foreign.ValueLayout.JAVA_LONG;

public final class MacWindows
{
    // CGWindowLevel.h and NSWindow.h
    private static final int DOCK_WINDOW_LEVEL_KEY = 7;
    private static final int FLOATING_WINDOW_LEVEL_KEY = 5;
    private static final long MANAGED_SPACE = 1L << 2;
    private static final long FULL_SCREEN_NONE = 1L << 9;
    private static final Linker LINKER = Linker.nativeLinker();
    private static final SymbolLookup OBJC =
            SymbolLookup.libraryLookup("/usr/lib/libobjc.A.dylib", Arena.global());
    private static final SymbolLookup SYSTEM =
            SymbolLookup.libraryLookup("/usr/lib/libSystem.B.dylib", Arena.global());
    private static final MethodHandle GET_CLASS = LINKER.downcallHandle(
            OBJC.findOrThrow("objc_getClass"), FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle GET_SELECTOR = LINKER.downcallHandle(
            OBJC.findOrThrow("sel_registerName"), FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MemorySegment SEND = OBJC.findOrThrow("objc_msgSend");
    private static final MethodHandle GET_OBJECT = LINKER.downcallHandle(
            SEND, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle GET_COUNT = LINKER.downcallHandle(
            SEND, FunctionDescriptor.of(JAVA_LONG, ADDRESS, ADDRESS));
    private static final MethodHandle GET_ITEM = LINKER.downcallHandle(
            SEND, FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle SET_BOOLEAN = LINKER.downcallHandle(
            SEND, FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_BOOLEAN));
    private static final MethodHandle SET_INTEGER = LINKER.downcallHandle(
            SEND, FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle DISPATCH = LINKER.downcallHandle(
            SYSTEM.findOrThrow("dispatch_async_f"), FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle WINDOW_LEVEL = LINKER.downcallHandle(
            SymbolLookup.libraryLookup("/System/Library/Frameworks/CoreGraphics.framework/CoreGraphics", Arena.global())
                    .findOrThrow("CGWindowLevelForKey"), FunctionDescriptor.of(JAVA_INT, JAVA_INT));
    private static final MemoryLayout POINT = MemoryLayout.structLayout(JAVA_DOUBLE, JAVA_DOUBLE);
    private static final MethodHandle SET_POSITION = message(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, POINT));

    private final String snowTitle;
    private final String bankTitle;
    private final String vehicleTitle;
    private final Consumer<Throwable> onConfigured;
    private final Consumer<Throwable> onRenderingFailed;
    private MemorySegment renderEntry;
    private MacLayer bankLayer;
    private MacLayer vehicleLayer;
    private MemorySegment vehicleWindow;
    private OverlayFrame pending;
    private boolean scheduled;
    private boolean closed;

    public record OverlayFrame(BufferedImage bank, BufferedImage vehicle, double x, double bottom)
    {
    }

    public MacWindows(String snowTitle, String bankTitle, String vehicleTitle, Consumer<Throwable> onConfigured,
            Consumer<Throwable> onRenderingFailed)
    {
        this.snowTitle = snowTitle;
        this.bankTitle = bankTitle;
        this.vehicleTitle = vehicleTitle;
        this.onConfigured = onConfigured;
        this.onRenderingFailed = onRenderingFailed;
    }

    public void configure()
    {
        try
        {
            MethodHandle callback = MethodHandles.lookup().findVirtual(MacWindows.class,
                    "configureOnMainThread", MethodType.methodType(void.class, MemorySegment.class));
            MethodHandle render = MethodHandles.lookup().findVirtual(MacWindows.class,
                    "renderOnMainThread", MethodType.methodType(void.class, MemorySegment.class));
            // AppKit windows may only be changed on the macOS main thread.
            MemorySegment entry = LINKER.upcallStub(callback.bindTo(this),
                    FunctionDescriptor.ofVoid(ADDRESS), Arena.global());
            renderEntry = LINKER.upcallStub(render.bindTo(this), FunctionDescriptor.ofVoid(ADDRESS), Arena.global());
            dispatch(entry);
        }
        catch (Throwable failure)
        {
            throw new IllegalStateException("Could not schedule AppKit window setup", failure);
        }
    }

    public synchronized void present(OverlayFrame frame)
    {
        if (!closed)
        {
            pending = frame;
            scheduleRender();
        }
    }

    public synchronized void close()
    {
        closed = true;
        pending = null;
        if (renderEntry != null)
        {
            scheduleRender();
        }
    }

    private void scheduleRender()
    {
        if (!scheduled)
        {
            scheduled = true;
            try
            {
                dispatch(renderEntry);
            }
            catch (Throwable failure)
            {
                scheduled = false;
                throw new IllegalStateException("Could not schedule overlay rendering", failure);
            }
        }
    }

    private static void dispatch(MemorySegment entry) throws Throwable
    {
        DISPATCH.invokeExact(SYSTEM.findOrThrow("_dispatch_main_q"), MemorySegment.NULL, entry);
    }

    private void renderOnMainThread(MemorySegment context)
    {
        OverlayFrame frame;
        boolean closing;
        synchronized (this)
        {
            frame = pending;
            pending = null;
            scheduled = false;
            closing = closed;
        }
        try (Arena arena = Arena.ofConfined())
        {
            if (closing)
            {
                if (bankLayer != null)
                {
                    bankLayer.close(arena);
                    bankLayer = null;
                }
                if (vehicleLayer != null)
                {
                    vehicleLayer.close(arena);
                    vehicleLayer = null;
                }
                return;
            }
            if (frame == null)
            {
                return;
            }
            MacLayer.begin(arena);
            try
            {
                bankLayer.show(frame.bank(), arena);
                vehicleLayer.show(frame.vehicle(), arena);
                var origin = arena.allocate(POINT);
                origin.set(JAVA_DOUBLE, 0, frame.x());
                origin.set(JAVA_DOUBLE, 8, frame.bottom());
                SET_POSITION.invokeExact(vehicleWindow, selector("setFrameOrigin:", arena), origin);
            }
            finally
            {
                MacLayer.commit(arena);
            }
        }
        catch (Throwable failure)
        {
            SwingUtilities.invokeLater(() -> onRenderingFailed.accept(failure));
        }
    }

    private void configureOnMainThread(MemorySegment context)
    {
        try (Arena arena = Arena.ofConfined())
        {
            MemorySegment appClass = objcClass("NSApplication", arena);
            MemorySegment app = getObject(appClass, "sharedApplication", arena);
            MemorySegment windows = getObject(app, "windows", arena);
            long count = (long)GET_COUNT.invokeExact(windows, selector("count", arena));
            MemorySegment objectAt = selector("objectAtIndex:", arena);
            MemorySegment title = selector("title", arena);
            MemorySegment utf8 = selector("UTF8String", arena);
            MemorySegment ignoreMouse = selector("setIgnoresMouseEvents:", arena);
            MemorySegment setLevel = selector("setLevel:", arena);
            MemorySegment setBehavior = selector("setCollectionBehavior:", arena);
            int dockLevel = (int)WINDOW_LEVEL.invokeExact(DOCK_WINDOW_LEVEL_KEY);
            int floatingLevel = (int)WINDOW_LEVEL.invokeExact(FLOATING_WINDOW_LEVEL_KEY);
            long bankLevel = Math.max(dockLevel, floatingLevel) + 1L;

            int configured = 0;
            for (long index = 0; index < count; index++)
            {
                MemorySegment window = (MemorySegment)GET_ITEM.invokeExact(windows, objectAt, index);
                MemorySegment name = (MemorySegment)GET_OBJECT.invokeExact(window, title);
                MemorySegment bytes = (MemorySegment)GET_OBJECT.invokeExact(name, utf8);
                if (bytes.equals(MemorySegment.NULL))
                {
                    continue;
                }

                String windowTitle = bytes.reinterpret(Long.MAX_VALUE).getString(0);
                if (snowTitle.equals(windowTitle))
                {
                    SET_BOOLEAN.invokeExact(window, ignoreMouse, true);
                }
                else if (bankTitle.equals(windowTitle))
                {
                    SET_BOOLEAN.invokeExact(window, ignoreMouse, true);
                    SET_INTEGER.invokeExact(window, setLevel, bankLevel);
                    bankLayer = new MacLayer(window, arena);
                }
                else if (vehicleTitle.equals(windowTitle))
                {
                    SET_BOOLEAN.invokeExact(window, ignoreMouse, false);
                    SET_INTEGER.invokeExact(window, setLevel, bankLevel + 1);
                    vehicleWindow = window;
                    vehicleLayer = new MacLayer(window, arena);
                }
                else
                {
                    continue;
                }
                // Elevated window levels are transient by default. Keep each overlay in its own Space.
                SET_INTEGER.invokeExact(window, setBehavior, MANAGED_SPACE | FULL_SCREEN_NONE);
                configured++;
            }
            if (configured != 3)
            {
                throw new IllegalStateException("The three Snow Jok AppKit windows were not found");
            }
            SwingUtilities.invokeLater(() -> onConfigured.accept(null));
        }
        catch (Throwable failure)
        {
            // A Java exception must never escape a native upcall.
            SwingUtilities.invokeLater(() -> onConfigured.accept(failure));
        }
    }

    static MethodHandle message(FunctionDescriptor descriptor)
    {
        return LINKER.downcallHandle(SEND, descriptor);
    }

    static MemorySegment objcClass(String name, Arena arena) throws Throwable
    {
        return (MemorySegment)GET_CLASS.invokeExact(arena.allocateFrom(name));
    }

    static MemorySegment getObject(MemorySegment receiver, String message, Arena arena) throws Throwable
    {
        return (MemorySegment)GET_OBJECT.invokeExact(receiver, selector(message, arena));
    }

    static MemorySegment selector(String name, Arena arena) throws Throwable
    {
        return (MemorySegment)GET_SELECTOR.invokeExact(arena.allocateFrom(name));
    }
}
