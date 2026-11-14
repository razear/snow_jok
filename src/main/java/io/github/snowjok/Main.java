package io.github.snowjok;

import java.awt.GraphicsEnvironment;
import java.util.Locale;
import java.util.prefs.Preferences;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import io.github.snowjok.desktop.DesktopSnow;
import io.github.snowjok.i18n.Messages;

public final class Main
{
    private Main()
    {
    }

    public static void main(String[] args)
    {
        if (!System.getProperty("os.name").toLowerCase(Locale.ROOT).startsWith("mac"))
        {
            System.err.println(Messages.text("error.platform"));
            return;
        }
        System.setProperty("apple.awt.UIElement", "true");
        if (GraphicsEnvironment.isHeadless())
        {
            System.err.println(Messages.text("error.headless"));
            return;
        }

        System.setProperty("apple.awt.application.name", "Snow Jok");
        System.setProperty("apple.awt.enableTemplateImages", "true");
        SwingUtilities.invokeLater(() ->
        {
            JComponent.setDefaultLocale(Messages.locale());
            try
            {
                new DesktopSnow(Preferences.userNodeForPackage(Main.class), Main::showError).start();
            }
            catch (RuntimeException | LinkageError failure)
            {
                showError(failure);
            }
        });
    }

    private static void showError(Throwable failure)
    {
        failure.printStackTrace();
        JOptionPane.showMessageDialog(null, Messages.text("error.startup"),
                "Snow Jok", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }
}
