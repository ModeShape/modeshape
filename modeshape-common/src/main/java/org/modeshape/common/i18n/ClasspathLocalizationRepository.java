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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.modeshape.common.util.CheckArg;

/**
 * Class that loads a properties file from the classpath of the supplied {@link ClassLoader class loader}.
 * <p>
 * This repository for a property file by building locations of the form "path/to/class_locale.properties", where "path/to/class"
 * is created from the fully-qualified classname and all "." replaced with "/" characters, "locale" is the a variant of the locale
 * (first the full locale, then subsequently with the last segment removed). As soon as a property file is found, its URL is
 * returned immediately.
 * </p>
 * named with a name that matches
 */
public final class ClasspathLocalizationRepository {

    private ClasspathLocalizationRepository() {
    }

    /**
     * Obtain the URL to the properties file containing the localized messages given the supplied bundle name. This method is
     * responsible for searching to find the most appropriate localized messages given the locale, but does not need to search
     * using the {@link Locale#getDefault() default locale} (as that is done by the {@link I18n#text(Object...) calling} method.
     * 
     * @param classLoader the classloader that should be used to load the localization bundles
     * @param bundleName the name of the bundle of properties; never null
     * @param locale the locale for which the properties file URL is desired
     * @return the URL to the properties file containing the localized messages for the named bundle, or null if no such bundle
     *         could be found
     */
    public static URL getLocalizationBundle( ClassLoader classLoader,
                                             String bundleName,
                                             Locale locale ) {
        CheckArg.isNotNull(classLoader, "classLoader");
        URL url = null;
        List<String> paths = getPathsToSearchForBundle(bundleName, locale);
        for (String path : paths) {
            url = classLoader.getResource(path);
            if (url != null) {
                return url;
            }
        }
        return url;
    }

    /**
     * Returns a list of paths (as string) of the different bundles searched in the
     * {@link ClasspathLocalizationRepository#getLocalizationBundle(ClassLoader, String, java.util.Locale)} method.
     * 
     * @param bundleName the name of the bundle of properties; never null
     * @param locale the locale for which the properties file URL is desired
     * @return a list of paths which the repository would look at.
     */
    static List<String> getPathsToSearchForBundle( String bundleName,
                                                   Locale locale ) {
        List<String> result = new ArrayList<String>();
        String pathPrefix = bundleName.replaceAll("\\.", "/");
        String localeVariant = '_' + locale.toString();
        int ndx = localeVariant.lastIndexOf('_');

        while (ndx >= 0) {
            String path = pathPrefix + localeVariant + ".properties";
            result.add(path);
            localeVariant = localeVariant.substring(0, ndx);
            ndx = localeVariant.lastIndexOf('_');
        }
        result.add(pathPrefix + localeVariant + ".properties");
        return result;
    }
}
