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

import java.net.URL;
import java.util.Locale;

/**
 * A repository of localized property files used by the {@link I18n internationalization framework}.
 */
public interface LocalizationRepository {

    /**
     * Obtain the URL to the properties file containing the localized messages given the supplied bundle name. This method is
     * responsible for searching to find the most appropriate localized messages given the locale, but does not need to search
     * using the {@link Locale#getDefault() default locale} (as that is done by the {@link I18n#text(Object...) calling} method.
     * 
     * @param bundleName the name of the bundle of properties; never null
     * @param locale the locale for which the properties file URL is desired
     * @return the URL to the properties file containing the localized messages for the named bundle, or null if no such bundle
     *         could be found
     */
    URL getLocalizationBundle( String bundleName,
                               Locale locale );

}
