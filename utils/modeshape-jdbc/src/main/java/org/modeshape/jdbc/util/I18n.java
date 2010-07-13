/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
* See the AUTHORS.txt file in the distribution for a full listing of 
* individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jdbc.util;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.modeshape.jdbc.JdbcI18n;
import org.modeshape.jdbc.util.SystemFailureException;
import org.modeshape.jdbc.util.CheckArg;
import org.modeshape.jdbc.util.ClassUtil;
import org.modeshape.jdbc.util.StringUtil;

/**
 * An internalized string object, which manages the initialization of internationalization (i18n) files, substitution of values
 * within i18n message placeholders, and dynamically reading properties from i18n property files.
 */
public final class I18n {

    private static final LocalizationRepository DEFAULT_LOCALIZATION_REPOSITORY = new ClasspathLocalizationRepository();

    /**
     * The first level of this map indicates whether an i18n class has been localized to a particular locale. The second level
     * contains any problems encountered during localization.
     */
    static final ConcurrentMap<Locale, Map<Class<?>, Set<String>>> LOCALE_TO_CLASS_TO_PROBLEMS_MAP = new ConcurrentHashMap<Locale, Map<Class<?>, Set<String>>>();

    private static LocalizationRepository localizationRepository = DEFAULT_LOCALIZATION_REPOSITORY;

    /**
     * Note, calling this method will <em>not</em> trigger localization of the supplied internationalization class.
     * 
     * @param i18nClass The internalization class for which localization problem locales should be returned.
     * @return The locales for which localization problems were encountered while localizing the supplied internationalization
     *         class; never <code>null</code>.
     */
    public static Set<Locale> getLocalizationProblemLocales( Class<?> i18nClass ) {
        CheckArg.isNotNull(i18nClass, "i18nClass");
        Set<Locale> locales = new HashSet<Locale>(LOCALE_TO_CLASS_TO_PROBLEMS_MAP.size());
        for (Entry<Locale, Map<Class<?>, Set<String>>> localeEntry : LOCALE_TO_CLASS_TO_PROBLEMS_MAP.entrySet()) {
            for (Entry<Class<?>, Set<String>> classEntry : localeEntry.getValue().entrySet()) {
                if (!classEntry.getValue().isEmpty()) {
                    locales.add(localeEntry.getKey());
                    break;
                }
            }
        }
        return locales;
    }

    /**
     * Note, calling this method will <em>not</em> trigger localization of the supplied internationalization class.
     * 
     * @param i18nClass The internalization class for which localization problems should be returned.
     * @return The localization problems encountered while localizing the supplied internationalization class to the default
     *         locale; never <code>null</code>.
     */
    public static Set<String> getLocalizationProblems( Class<?> i18nClass ) {
        return getLocalizationProblems(i18nClass, null);
    }

    /**
     * Note, calling this method will <em>not</em> trigger localization of the supplied internationalization class.
     * 
     * @param i18nClass The internalization class for which localization problems should be returned.
     * @param locale The locale for which localization problems should be returned. If <code>null</code>, the default locale will
     *        be used.
     * @return The localization problems encountered while localizing the supplied internationalization class to the supplied
     *         locale; never <code>null</code>.
     */
    public static Set<String> getLocalizationProblems( Class<?> i18nClass,
                                                       Locale locale ) {
        CheckArg.isNotNull(i18nClass, "i18nClass");
        Map<Class<?>, Set<String>> classToProblemsMap = LOCALE_TO_CLASS_TO_PROBLEMS_MAP.get(locale == null ? Locale.getDefault() : locale);
        if (classToProblemsMap == null) {
            return Collections.emptySet();
        }
        Set<String> problems = classToProblemsMap.get(i18nClass);
        if (problems == null) {
            return Collections.emptySet();
        }
        return problems;
    }

    /**
     * Get the repository of localized messages. By default, this instance uses a {@link ClasspathLocalizationRepository} that
     * uses this class' classloader.
     * 
     * @return localizationRepository
     */
    public static LocalizationRepository getLocalizationRepository() {
        return localizationRepository;
    }

    /**
     * Set the repository of localized messages. If <code>null</code>, a {@link ClasspathLocalizationRepository} instance that
     * uses this class loader will be used.
     * 
     * @param localizationRepository the localization repository to use; may be <code>null</code> if the default repository should
     *        be used.
     */
    public static void setLocalizationRepository( LocalizationRepository localizationRepository ) {
        I18n.localizationRepository = localizationRepository != null ? localizationRepository : DEFAULT_LOCALIZATION_REPOSITORY;
    }

    /**
     * Initializes the internationalization fields declared on the supplied class. Internationalization fields must be public,
     * static, not final, and of type <code>I18n</code>. The supplied class must not be an interface (of course), but has no
     * restrictions as to what class it may extend or what interfaces it must implement.
     * 
     * @param i18nClass A class declaring one or more public, static, non-final fields of type <code>I18n</code>.
     */
    public static void initialize( Class<?> i18nClass ) {
        CheckArg.isNotNull(i18nClass, "i18nClass");
        if (i18nClass.isInterface()) {
            throw new IllegalArgumentException(JdbcI18n.i18nClassInterface.text(i18nClass.getName()));
        }

        synchronized (i18nClass) {
            // Find all public static non-final String fields in the supplied class and instantiate an I18n object for each.
            try {
                for (Field fld : i18nClass.getDeclaredFields()) {

                    // Ensure field is of type I18n
                    if (fld.getType() == I18n.class) {

                        // Ensure field is public
                        if ((fld.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
                            throw new SystemFailureException(JdbcI18n.i18nFieldNotPublic.text(fld.getName(), i18nClass));
                        }

                        // Ensure field is static
                        if ((fld.getModifiers() & Modifier.STATIC) != Modifier.STATIC) {
                            throw new SystemFailureException(JdbcI18n.i18nFieldNotStatic.text(fld.getName(), i18nClass));
                        }

                        // Ensure field is not final
                        if ((fld.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
                            throw new SystemFailureException(JdbcI18n.i18nFieldFinal.text(fld.getName(), i18nClass));
                        }

                        // Ensure we can access field even if it's in a private class
                        ClassUtil.makeAccessible(fld);

                        // Initialize field. Do this every time the class is initialized (or re-initialized)
                        fld.set(null, new I18n(fld.getName(), i18nClass));
                    }
                }

                // Remove all entries for the supplied i18n class to indicate it has not been localized.
                for (Entry<Locale, Map<Class<?>, Set<String>>> entry : LOCALE_TO_CLASS_TO_PROBLEMS_MAP.entrySet()) {
                    entry.getValue().remove(i18nClass);
                }
            } catch (IllegalAccessException err) {
                // If this happens, it will happen with the first field visited in the above loop
                throw new IllegalArgumentException(JdbcI18n.i18nClassNotPublic.text(i18nClass));
            }
        }
    }

    /**
     * Synchronized on the supplied internalization class.
     * 
     * @param i18nClass The internalization class being localized
     * @param locale The locale to which the supplied internationalization class should be localized.
     */
    private static void localize( final Class<?> i18nClass,
                                  final Locale locale ) {
        assert i18nClass != null;
        assert locale != null;
        // Create a class-to-problem map for this locale if one doesn't exist, else get the existing one.
        Map<Class<?>, Set<String>> classToProblemsMap = new ConcurrentHashMap<Class<?>, Set<String>>();
        Map<Class<?>, Set<String>> existingClassToProblemsMap = LOCALE_TO_CLASS_TO_PROBLEMS_MAP.putIfAbsent(locale,
                                                                                                            classToProblemsMap);
        if (existingClassToProblemsMap != null) {
            classToProblemsMap = existingClassToProblemsMap;
        }
        // Check if already localized outside of synchronization block for 99% use-case
        if (classToProblemsMap.get(i18nClass) != null) {
            return;
        }
        synchronized (i18nClass) {
            // Return if the supplied i18n class has already been localized to the supplied locale, despite the check outside of
            // the synchronization block (1% use-case), else create a class-to-problems map for the class.
            Set<String> problems = classToProblemsMap.get(i18nClass);
            if (problems == null) {
                problems = new CopyOnWriteArraySet<String>();
                classToProblemsMap.put(i18nClass, problems);
            } else {
                return;
            }
            // Get the URL to the localization properties file ...
            final LocalizationRepository repos = getLocalizationRepository();
            final String localizationBaseName = i18nClass.getName();
            URL url = repos.getLocalizationBundle(localizationBaseName, locale);
            if (url == null) {
                // Nothing was found, so try the default locale
                Locale defaultLocale = Locale.getDefault();
                if (!defaultLocale.equals(locale)) {
                    url = repos.getLocalizationBundle(localizationBaseName, defaultLocale);
                }
                // Return if no applicable localization file could be found
                if (url == null) {
                    problems.add(JdbcI18n.i18nLocalizationFileNotFound.text(localizationBaseName));
                    return;
                }
            }
            // Initialize i18n map
            final URL finalUrl = url;
            final Set<String> finalProblems = problems;
            Properties props = new Properties() {

                /**
                 */
                private static final long serialVersionUID = 3920620306881072843L;

                @Override
                public synchronized Object put( Object key,
                                                Object value ) {
                    String id = (String)key;
                    String text = (String)value;

                    try {
                        Field fld = i18nClass.getDeclaredField(id);
                        if (fld.getType() != I18n.class) {
                            // Invalid field type
                            finalProblems.add(JdbcI18n.i18nFieldInvalidType.text(id, finalUrl, getClass().getName()));
                        } else {
                            I18n i18n = (I18n)fld.get(null);
                            if (i18n.localeToTextMap.putIfAbsent(locale, text) != null) {
                                // Duplicate id encountered
                                String prevProblem = i18n.localeToProblemMap.putIfAbsent(locale,
                                                                                         JdbcI18n.i18nPropertyDuplicate.text(id,
                                                                                                                               finalUrl));
                                assert prevProblem == null;
                            }
                        }
                    } catch (NoSuchFieldException err) {
                        // No corresponding field exists
                        finalProblems.add(JdbcI18n.i18nPropertyUnused.text(id, finalUrl));
                    } catch (IllegalAccessException notPossible) {
                        // Would have already occurred in initialize method, but allowing for the impossible...
                        finalProblems.add(notPossible.getMessage());
                    }

                    return null;
                }
            };

            try {
                InputStream propStream = url.openStream();
                try {
                    props.load(propStream);
                    // Check for uninitialized fields
                    for (Field fld : i18nClass.getDeclaredFields()) {
                        if (fld.getType() == I18n.class) {
                            try {
                                I18n i18n = (I18n)fld.get(null);
                                if (i18n.localeToTextMap.get(locale) == null) {
                                    i18n.localeToProblemMap.put(locale, JdbcI18n.i18nPropertyMissing.text(fld.getName(), url));
                                }
                            } catch (IllegalAccessException notPossible) {
                                // Would have already occurred in initialize method, but allowing for the impossible...
                                finalProblems.add(notPossible.getMessage());
                            }
                        }
                    }
                } finally {
                    propStream.close();
                }
            } catch (IOException err) {
                finalProblems.add(err.getMessage());
            }
        }
    }

    private final String id;
    private final Class<?> i18nClass;
    final ConcurrentHashMap<Locale, String> localeToTextMap = new ConcurrentHashMap<Locale, String>();
    final ConcurrentHashMap<Locale, String> localeToProblemMap = new ConcurrentHashMap<Locale, String>();

    private I18n( String id,
                  Class<?> i18nClass ) {
        this.id = id;
        this.i18nClass = i18nClass;
    }

    /**
     * @return This internationalization object's ID, which will match both the name of the relevant static field in the
     *         internationalization class and the relevant property name in the associated localization files.
     */
    public String id() {
        return id;
    }

    /**
     * @return <code>true</code> if a problem was encountered while localizing this internationalization object to the default
     *         locale.
     */
    public boolean hasProblem() {
        return (problem() != null);
    }

    /**
     * @param locale The locale for which to check whether a problem was encountered.
     * @return <code>true</code> if a problem was encountered while localizing this internationalization object to the supplied
     *         locale.
     */
    public boolean hasProblem( Locale locale ) {
        return (problem(locale) != null);
    }

    /**
     * @return The problem encountered while localizing this internationalization object to the default locale, or
     *         <code>null</code> if none was encountered.
     */
    public String problem() {
        return problem(null);
    }

    /**
     * @param locale The locale for which to return the problem.
     * @return The problem encountered while localizing this internationalization object to the supplied locale, or
     *         <code>null</code> if none was encountered.
     */
    public String problem( Locale locale ) {
        if (locale == null) {
            locale = Locale.getDefault();
        }
        localize(i18nClass, locale);
        // Check for field/property error
        String problem = localeToProblemMap.get(locale);
        if (problem != null) {
            return problem;
        }
        // Check if text exists
        if (localeToTextMap.get(locale) != null) {
            // If so, no problem exists
            return null;
        }
        // If we get here, which will be at most once, there was at least one global localization error, so just return a message
        // indicating to look them up.
        problem = JdbcI18n.i18nLocalizationProblems.text(i18nClass, locale);
        localeToProblemMap.put(locale, problem);
        return problem;
    }

    private String rawText( Locale locale ) {
        assert locale != null;
        localize(i18nClass, locale);
        // Check if text exists
        String text = localeToTextMap.get(locale);
        if (text != null) {
            return text;
        }
        // If not, there was a problem, so throw it within an exception so upstream callers can tell the difference between normal
        // text and problem text.
        throw new SystemFailureException(problem(locale));
    }

    /**
     * Get the localized text for the {@link Locale#getDefault() current (default) locale}, replacing the parameters in the text
     * with those supplied.
     * 
     * @param arguments the arguments for the parameter replacement; may be <code>null</code> or empty
     * @return the localized text
     */
    public String text( Object... arguments ) {
        return text(null, arguments);
    }

    /**
     * Get the localized text for the supplied locale, replacing the parameters in the text with those supplied.
     * 
     * @param locale the locale, or <code>null</code> if the {@link Locale#getDefault() current (default) locale} should be used
     * @param arguments the arguments for the parameter replacement; may be <code>null</code> or empty
     * @return the localized text
     */
    public String text( Locale locale,
                        Object... arguments ) {
        try {
            String rawText = rawText(locale == null ? Locale.getDefault() : locale);
            return StringUtil.createString(rawText, arguments);
        } catch (IllegalArgumentException err) {
            throw new IllegalArgumentException(JdbcI18n.i18nRequiredToSuppliedParameterMismatch.text(id,
                                                                                                       i18nClass,
                                                                                                       err.getMessage()));
        } catch (SystemFailureException err) {
            return '<' + err.getMessage() + '>';
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        try {
            return rawText(Locale.getDefault());
        } catch (SystemFailureException err) {
            return '<' + err.getMessage() + '>';
        }
    }
}
