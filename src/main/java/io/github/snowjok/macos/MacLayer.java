package io.github.snowjok.macos;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;

final class MacLayer
{
    private static final MemorySegment NIL = MemorySegment.NULL;
    // Java TYPE_INT_ARGB_PRE stores premultiplied ARGB as native-endian 32-bit pixels.
    private static final int ARGB_PRE = 0x2002;
    private static final int RESIZE_WIDTH_AND_HEIGHT = (1 << 1) | (1 << 4);
    private static final SymbolLookup GRAPHICS = framework("CoreGraphics");
    private static final SymbolLookup FOUNDATION = framework("CoreFoundation");
    private static final SymbolLookup QUARTZ = framework("QuartzCore");
    private static final MethodHandle CREATE_DATA = nativeCall(FOUNDATION, "CFDataCreate",
            FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, JAVA_LONG));
    private static final MethodHandle CREATE_PROVIDER = nativeCall(GRAPHICS, "CGDataProviderCreateWithCFData",
            FunctionDescriptor.of(ADDRESS, ADDRESS));
    private static final MethodHandle CREATE_COLOR_SPACE = nativeCall(GRAPHICS, "CGColorSpaceCreateDeviceRGB",
            FunctionDescriptor.of(ADDRESS));
    private static final MethodHandle CREATE_IMAGE = nativeCall(GRAPHICS, "CGImageCreate",
            FunctionDescriptor.of(ADDRESS, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_LONG,
                    ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_BOOLEAN, JAVA_INT));
    private static final MethodHandle RELEASE = nativeCall(FOUNDATION, "CFRelease",
            FunctionDescriptor.ofVoid(ADDRESS));
    private static final MethodHandle SEND_VOID = MacWindows.message(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS));
    private static final MethodHandle SEND_OBJECT = MacWindows.message(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, ADDRESS));
    private static final MethodHandle SEND_BOOL = MacWindows.message(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_BOOLEAN));
    private static final MemoryLayout RECT = MemoryLayout.structLayout(
            MemoryLayout.structLayout(JAVA_DOUBLE, JAVA_DOUBLE),
            MemoryLayout.structLayout(JAVA_DOUBLE, JAVA_DOUBLE));
    private static final MethodHandle SET_RECT = MacWindows.message(FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, RECT));
    private static final MethodHandle SET_FLAGS = MacWindows.message(
            FunctionDescriptor.ofVoid(ADDRESS, ADDRESS, JAVA_INT));

    private final MemorySegment layer;
    private BufferedImage displayed;

    MacLayer(MemorySegment window, Arena arena) throws Throwable
    {
        // Use a standalone sublayer so AppKit and AWT can still manage their backing view.
        var view = MacWindows.getObject(window, "contentView", arena);
        SEND_BOOL.invokeExact(view, MacWindows.selector("setWantsLayer:", arena), true);
        var parent = MacWindows.getObject(view, "layer", arena);
        var layerClass = MacWindows.objcClass("CALayer", arena);
        layer = MacWindows.getObject(layerClass, "new", arena);
        try
        {
            SET_FLAGS.invokeExact(layer, MacWindows.selector("setAutoresizingMask:", arena), RESIZE_WIDTH_AND_HEIGHT);
            SEND_OBJECT.invokeExact(parent, MacWindows.selector("addSublayer:", arena), layer);
        }
        catch (Throwable failure)
        {
            close(arena);
            throw failure;
        }
    }

    static void begin(Arena arena) throws Throwable
    {
        // Looking up QuartzCore above ensures its Objective-C classes have been loaded.
        var transaction = MacWindows.objcClass("CATransaction", arena);
        SEND_VOID.invokeExact(transaction, MacWindows.selector("begin", arena));
        SEND_BOOL.invokeExact(transaction, MacWindows.selector("setDisableActions:", arena), true);
    }

    static void commit(Arena arena) throws Throwable
    {
        SEND_VOID.invokeExact(MacWindows.objcClass("CATransaction", arena), MacWindows.selector("commit", arena));
    }

    void show(BufferedImage image, Arena arena) throws Throwable
    {
        if (displayed == image)
        {
            return;
        }
        if (displayed == null || displayed.getWidth() != image.getWidth() || displayed.getHeight() != image.getHeight())
        {
            MemorySegment bounds = arena.allocate(RECT);
            bounds.set(JAVA_DOUBLE, 16, image.getWidth());
            bounds.set(JAVA_DOUBLE, 24, image.getHeight());
            SET_RECT.invokeExact(layer, MacWindows.selector("setFrame:", arena), bounds);
        }

        int[] pixels = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();
        try (Arena bitmap = Arena.ofConfined())
        {
            MemorySegment data = (MemorySegment)CREATE_DATA.invokeExact(
                    NIL, bitmap.allocateFrom(JAVA_INT, pixels), pixels.length * 4L);
            MemorySegment provider = NIL;
            MemorySegment color = NIL;
            MemorySegment nativeImage = NIL;
            try
            {
                require(data);
                provider = (MemorySegment)CREATE_PROVIDER.invokeExact(data);
                require(provider);
                color = (MemorySegment)CREATE_COLOR_SPACE.invokeExact();
                require(color);
                nativeImage = (MemorySegment)CREATE_IMAGE.invokeExact((long)image.getWidth(),
                        (long)image.getHeight(), 8L, 32L, image.getWidth() * 4L, color, ARGB_PRE,
                        provider, NIL, false, 0);
                require(nativeImage);
                SEND_OBJECT.invokeExact(layer, MacWindows.selector("setContents:", arena), nativeImage);
                displayed = image;
            }
            finally
            {
                release(nativeImage);
                release(color);
                release(provider);
                release(data);
            }
        }
    }

    void close(Arena arena) throws Throwable
    {
        SEND_VOID.invokeExact(layer, MacWindows.selector("removeFromSuperlayer", arena));
        SEND_VOID.invokeExact(layer, MacWindows.selector("release", arena));
    }

    private static void require(MemorySegment pointer)
    {
        if (pointer.equals(NIL))
        {
            throw new IllegalStateException("Could not allocate a Core Graphics image");
        }
    }

    private static void release(MemorySegment pointer) throws Throwable
    {
        if (!pointer.equals(NIL))
        {
            RELEASE.invokeExact(pointer);
        }
    }

    private static SymbolLookup framework(String name)
    {
        return SymbolLookup.libraryLookup("/System/Library/Frameworks/" + name + ".framework/" + name,
                Arena.global());
    }

    private static MethodHandle nativeCall(SymbolLookup library, String name, FunctionDescriptor descriptor)
    {
        return Linker.nativeLinker().downcallHandle(library.findOrThrow(name), descriptor);
    }
}
