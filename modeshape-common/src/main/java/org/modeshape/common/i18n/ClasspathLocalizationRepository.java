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
package org.modeshape.common.i18n;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implementation of a {@link LocalizationRepository} that loads a properties file from the classpath of the supplied
 * {@link ClassLoader class loader}.
 * <p>
 * This repository for a property file by building locations of the form "path/to/class_locale.properties", where "path/to/class"
 * is created from the fully-qualified classname and all "." replaced with "/" characters, "locale" is the a variant of the locale
 * (first the full locale, then subsequently with the last segment removed). As soon as a property file is found, its URL is
 * returned immediately.
 * </p>
 * named with a name that matches
 */
public class ClasspathLocalizationRepository implements LocalizationRepository {

    private final ClassLoader classLoader;

    /**
     * Create a repository using the current thread's {@link Thread#getContextClassLoader() context class loader} or, if that is
     * null, the same class loader that loaded this class.
     */
    public ClasspathLocalizationRepository() {
        this(null);
    }

    /**
     * Create a repository using the supplied class loader. Null may be passed if the class loader should be obtained from the
     * current thread's {@link Thread#getContextClassLoader() context class loader} or, if that is null, the same class loader
     * that loaded this class.
     * 
     * @param classLoader the class loader to use; may be null
     */
    public ClasspathLocalizationRepository( ClassLoader classLoader ) {
        if (classLoader == null) classLoader = this.getClass().getClassLoader();
        this.classLoader = classLoader;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URL getLocalizationBundle( String bundleName,
                                      Locale locale ) {
        URL url = null;
        List<String> paths = getPathsToSearchForBundle(bundleName, locale);
        for (String path : paths) {
            url = this.classLoader.getResource(path);
            if (url != null) {
                return url;
            }
        }
        return url;
    }

    @Override
    public List<String> getPathsToSearchForBundle( String bundleName,
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
