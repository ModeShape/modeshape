package org.jboss.dna.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.dna.common.CoreI18n;

/**
 * Manages the initialization of internationalization (i18n) files, substitution of values within i18n message placeholders, and
 * dynamically reading properties from i18n property files.
 * @author John Verhaeg
 */
public final class I18n {

    private static ThreadLocal<Locale> locale = new ThreadLocal<Locale>() {

        @Override
        protected Locale initialValue() {
            return Locale.getDefault();
        }
    };

    /**
     * @return The locale for the current thread, or the default locale if none has been set.
     * @see #setLocale(Locale)
     */
    public static Locale getLocale() {
        return locale.get();
    }

    /**
     * Initializes the internationalization fields declared on the specified class. Internationalization fields must be public,
     * static, not final, and of type <code>I18n</code>. The specified class must not be an interface (of course), but has no
     * restrictions as to what class it may extend or what interfaces it must implement.
     * @param i18nClass A class declaring one or more public, static, non-final fields of type <code>I18n</code>.
     */
    public static void initialize( Class i18nClass ) {
        ArgCheck.isNotNull(i18nClass, "i18nClass");
        if (i18nClass.isInterface()) {
            throw new IllegalArgumentException(CoreI18n.i18nClassInterface.text(i18nClass.getName()));
        }

        // Find all public static non-final String fields in the specified class and instantiate an I18n object for each.
        Map<Locale, Map<String, String>> locale2Id2TextMap = new HashMap<Locale, Map<String, String>>();
        try {
            for (Field fld : i18nClass.getDeclaredFields()) {

                // Ensure field is of type I18n
                if (fld.getType() == I18n.class) {

                    // Ensure field is not final
                    if ((fld.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
                        throw new RuntimeException(CoreI18n.i18nFieldFinal.text(fld.getName(), i18nClass));
                    }

                    // Ensure field is public
                    if ((fld.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                        throw new RuntimeException(CoreI18n.i18nFieldNotPublic.text(fld.getName(), i18nClass));
                    }

                    // Ensure field is static
                    if ((fld.getModifiers() & Modifier.STATIC) != Modifier.STATIC) {
                        throw new RuntimeException(CoreI18n.i18nFieldNotStatic.text(fld.getName(), i18nClass));
                    }

                    // Ensure we can access field even if it's in a private class
                    ClassUtil.makeAccessible(fld);

                    // Initialize field
                    if (fld.get(null) == null) {
                        fld.set(null, new I18n(fld.getName(), i18nClass, locale2Id2TextMap));
                    }
                }
            }
        } catch (IllegalAccessException err) {
            throw new RuntimeException(err);
        }
    }

    /**
     * @param locale The locale to set for the current thread. The default locale will be set for the thread if this argument is
     * <code>null</code>.
     */
    public static void setLocale( Locale locale ) {
        I18n.locale.set(locale == null ? Locale.getDefault() : locale);
    }

    public final String id;
    Class i18nClass;
    private Map<Locale, Map<String, String>> locale2Id2TextMap;

    private I18n( String id, Class i18nClass, Map<Locale, Map<String, String>> locale2Id2TextMap ) {
        this.id = id;
        this.i18nClass = i18nClass;
        this.locale2Id2TextMap = locale2Id2TextMap;
    }

    public String text( Object... arguments ) {
        Locale locale = getLocale();
        Map<String, String> id2TextMap = locale2Id2TextMap.get(locale);
        if (id2TextMap == null) {

            // Determine the name of the properties file associated with the specified class
            String pathPfx = '/' + i18nClass.getName().replaceAll("\\.", "/");
            URL url = null;
            String variant = '_' + locale.toString();
            do {
                url = i18nClass.getResource(pathPfx + variant + ".properties");
                if (url == null) {
                    int ndx = variant.lastIndexOf('_');
                    if (ndx < 0) {
                        break;
                    }
                    variant = variant.substring(0, ndx);
                }
            } while (url == null);

            // Abort if no variant of the i18n properties file for the specified class found
            if (url == null) {
                throw new RuntimeException(CoreI18n.i18nPropertiesFileNotFound.text(i18nClass));
            }

            // Initialize i18n map
            id2TextMap = new HashMap<String, String>();
            final URL finalUrl = url;
            final Map<String, String> finalMap = id2TextMap;
            Properties props = new Properties() {

                @Override
                public synchronized Object put( Object key, Object value ) {
                    String id = (String)key;
                    String text = (String)value;

                    // Throw error if no corresponding field exists
                    try {
                        Field fld = i18nClass.getDeclaredField(id);
                        if (fld.getType() != I18n.class) {
                            throw new RuntimeException(CoreI18n.i18nPropertyUnused.text(id, finalUrl));
                        }
                    } catch (Exception err) {
                        throw new RuntimeException(CoreI18n.i18nPropertyUnused.text(id, finalUrl));
                    }

                    if (finalMap.put(id, text) != null) {

                        // Throw error if duplicate id encountered
                        throw new RuntimeException(CoreI18n.i18nPropertyDuplicate.text(id, finalUrl));
                    }

                    return null;
                }
            };

            try {
                InputStream propStream = url.openStream();
                try {
                    props.load(propStream);
                } finally {
                    propStream.close();
                }
            } catch (IOException err) {
                throw new RuntimeException(err);
            }

            // Check for uninitialized fields
            for (Field fld : i18nClass.getDeclaredFields()) {
                if (fld.getType() == I18n.class && id2TextMap.get(fld.getName()) == null) {
                    throw new RuntimeException(CoreI18n.i18nPropertyMissing.text(fld.getName(), url));
                }
            }

            locale2Id2TextMap.put(locale, id2TextMap);
        }
        String text = id2TextMap.get(id);

        // Ensure substitution argument count matches method argument count
        Matcher matcher = Pattern.compile("\\{(\\d+)\\}").matcher(text);
        StringBuffer newText = new StringBuffer();
        int argCount = 0;
        boolean err = false;
        while (matcher.find()) {
            int ndx = Integer.valueOf(matcher.group(1));
            if (argCount <= ndx) {
                argCount = ndx + 1;
            }
            if (ndx >= arguments.length) {
                err = true;
                matcher.appendReplacement(newText, matcher.group());
            } else {
                matcher.appendReplacement(newText, Matcher.quoteReplacement(arguments[ndx].toString()));
            }
        }
        if (err || argCount < arguments.length) {
            throw new IllegalArgumentException(CoreI18n.i18nArgumentMismatch.text(arguments.length == 1 ? arguments.length + " argument was" : arguments.length + " arguments were", id,
                                                                                  argCount == 1 ? argCount + " is" : argCount + " are", text, newText.toString()));
        }
        matcher.appendTail(newText);

        return newText.toString();
    }
}
