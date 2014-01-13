/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.common.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ClassUtil;
import org.modeshape.common.util.StringUtil;

/**
 * An internalized string object, which manages the initialization of internationalization (i18n) files, substitution of values
 * within i18n message placeholders, and dynamically reading properties from i18n property files.
 */
@ThreadSafe
public final class I18n implements I18nResource {

    /**
     * The first level of this map indicates whether an i18n class has been localized to a particular locale. The second level
     * contains any problems encountered during localization.
     *
     * Make sure this is always the first member in the class because it must be initialized *before* the Logger (see below).
     * Otherwise it's possible to trigger a NPE because of nested initializers.
     */
    static final ConcurrentMap<Locale, Map<Class<?>, Set<String>>> LOCALE_TO_CLASS_TO_PROBLEMS_MAP = new ConcurrentHashMap<Locale, Map<Class<?>, Set<String>>>();

    private static final Logger LOGGER = Logger.getLogger(I18n.class);

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
        return getLocalizationProblems(i18nClass, Locale.getDefault());
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
     * Initializes the internationalization fields declared on the supplied class. Internationalization fields must be public,
     * static, not final, and of type <code>I18n</code>. The supplied class must not be an interface (of course), but has no
     * restrictions as to what class it may extend or what interfaces it must implement.
     * 
     * @param i18nClass A class declaring one or more public, static, non-final fields of type <code>I18n</code>.
     */
    public static void initialize( Class<?> i18nClass ) {
        validateI18nClass(i18nClass);

        synchronized (i18nClass) {
            // Find all public static non-final String fields in the supplied class and instantiate an I18n object for each.
            try {
                for (Field fld : i18nClass.getDeclaredFields()) {
                    // Ensure field is of type I18n
                    if (fld.getType() == I18n.class) {
                        initializeI18nField(fld);
                    }
                }
                cleanupPreviousProblems(i18nClass);
            } catch (IllegalAccessException err) {
                // If this happens, it will happen with the first field visited in the above loop
                throw new IllegalArgumentException(CommonI18n.i18nClassNotPublic.text(i18nClass));
            }
        }
    }

    private static void cleanupPreviousProblems( Class<?> i18nClass ) {
        // Remove all entries for the supplied i18n class to indicate it has not been localized.
        for (Entry<Locale, Map<Class<?>, Set<String>>> entry : LOCALE_TO_CLASS_TO_PROBLEMS_MAP.entrySet()) {
            entry.getValue().remove(i18nClass);
        }
    }

    private static void initializeI18nField( Field fld ) throws IllegalAccessException {
        // Ensure field is public
        if ((fld.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
            throw new SystemFailureException(CommonI18n.i18nFieldNotPublic.text(fld.getName(), fld.getDeclaringClass()));
        }

        // Ensure field is static
        if ((fld.getModifiers() & Modifier.STATIC) != Modifier.STATIC) {
            throw new SystemFailureException(CommonI18n.i18nFieldNotStatic.text(fld.getName(), fld.getDeclaringClass()));
        }

        // Ensure field is not final
        if ((fld.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
            throw new SystemFailureException(CommonI18n.i18nFieldFinal.text(fld.getName(), fld.getDeclaringClass()));
        }

        // Ensure we can access field even if it's in a private class
        ClassUtil.makeAccessible(fld);

        // Initialize field. Do this every time the class is initialized (or re-initialized)
        fld.set(null, new I18n(fld.getName(), fld.getDeclaringClass()));
    }

    private static void validateI18nClass( Class<?> i18nClass ) {
        CheckArg.isNotNull(i18nClass, "i18nClass");
        if (i18nClass.isInterface()) {
            throw new IllegalArgumentException(CommonI18n.i18nClassInterface.text(i18nClass.getName()));
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
            final String localizationBaseName = i18nClass.getName();
            URL bundleUrl = ClasspathLocalizationRepository.getLocalizationBundle(i18nClass.getClassLoader(), localizationBaseName, locale);
            if (bundleUrl == null) {
                LOGGER.warn(CommonI18n.i18nBundleNotFoundInClasspath,
                            ClasspathLocalizationRepository.getPathsToSearchForBundle(localizationBaseName, locale));
                // Nothing was found, so try the default locale
                Locale defaultLocale = Locale.getDefault();
                if (!defaultLocale.equals(locale)) {
                    bundleUrl = ClasspathLocalizationRepository.getLocalizationBundle(i18nClass.getClassLoader(), localizationBaseName, defaultLocale);
                }
                // Return if no applicable localization file could be found
                if (bundleUrl == null) {
                    LOGGER.error(CommonI18n.i18nBundleNotFoundInClasspath,
                                 ClasspathLocalizationRepository.getPathsToSearchForBundle(localizationBaseName, defaultLocale));
                    LOGGER.error(CommonI18n.i18nLocalizationFileNotFound, localizationBaseName);
                    problems.add(CommonI18n.i18nLocalizationFileNotFound.text(localizationBaseName));
                    return;
                }
            }
            // Initialize i18n map
            Properties props = prepareBundleLoading(i18nClass, locale, bundleUrl, problems);

            try {
                InputStream propStream = bundleUrl.openStream();
                try {
                    props.load(propStream);
                    // Check for uninitialized fields
                    for (Field fld : i18nClass.getDeclaredFields()) {
                        if (fld.getType() == I18n.class) {
                            try {
                                I18n i18n = (I18n)fld.get(null);
                                if (i18n.localeToTextMap.get(locale) == null) {
                                    i18n.localeToProblemMap.put(locale,
                                                                CommonI18n.i18nPropertyMissing.text(fld.getName(), bundleUrl));
                                }
                            } catch (IllegalAccessException notPossible) {
                                // Would have already occurred in initialize method, but allowing for the impossible...
                                problems.add(notPossible.getMessage());
                            }
                        }
                    }
                } finally {
                    propStream.close();
                }
            } catch (IOException err) {
                problems.add(err.getMessage());
            }
        }
    }

    private static Properties prepareBundleLoading( final Class<?> i18nClass,
                                                    final Locale locale,
                                                    final URL bundleUrl,
                                                    final Set<String> problems ) {
        return new Properties() {
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
                        problems.add(CommonI18n.i18nFieldInvalidType.text(id, bundleUrl, getClass().getName()));
                    } else {
                        I18n i18n = (I18n)fld.get(null);
                        if (i18n.localeToTextMap.putIfAbsent(locale, text) != null) {
                            // Duplicate id encountered
                            String prevProblem = i18n.localeToProblemMap.putIfAbsent(locale,
                                                                                     CommonI18n.i18nPropertyDuplicate.text(id,
                                                                                                                           bundleUrl));
                            assert prevProblem == null;
                        }
                    }
                } catch (NoSuchFieldException err) {
                    // No corresponding field exists
                    problems.add(CommonI18n.i18nPropertyUnused.text(id, bundleUrl));
                } catch (IllegalAccessException notPossible) {
                    // Would have already occurred in initialize method, but allowing for the impossible...
                    problems.add(notPossible.getMessage());
                }

                return null;
            }
        };
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
        problem = CommonI18n.i18nLocalizationProblems.text(i18nClass, locale);
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
    @Override
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
    @Override
    public String text( Locale locale,
                        Object... arguments ) {
        try {
            String rawText = rawText(locale == null ? Locale.getDefault() : locale);
            return StringUtil.createString(rawText, arguments);
        } catch (IllegalArgumentException err) {
            throw new IllegalArgumentException(CommonI18n.i18nRequiredToSuppliedParameterMismatch.text(id,
                                                                                                       i18nClass,
                                                                                                       err.getMessage()));
        } catch (SystemFailureException err) {
            return '<' + err.getMessage() + '>';
        }
    }

    @Override
    public String toString() {
        try {
            return rawText(Locale.getDefault());
        } catch (SystemFailureException err) {
            return '<' + err.getMessage() + '>';
        }
    }
}
