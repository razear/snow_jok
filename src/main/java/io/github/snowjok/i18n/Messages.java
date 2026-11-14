package io.github.snowjok.i18n;

import java.util.Locale;
import java.util.ResourceBundle;

public final class Messages
{
    private static final Locale LOCALE = "ru".equals(Locale.getDefault(Locale.Category.DISPLAY).getLanguage())
            ? Locale.forLanguageTag("ru") : Locale.ENGLISH;
    private static final ResourceBundle STRINGS = ResourceBundle.getBundle(
            Messages.class.getName(), LOCALE,
            ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));

    private Messages()
    {
    }

    public static Locale locale()
    {
        return LOCALE;
    }

    public static String text(String key)
    {
        return STRINGS.getString(key);
    }
}
