/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.i18n;

import java.net.URL;
import java.util.Locale;

/**
 * A repository of localized property files used by the {@link I18n internationalization framework}.
 * @author Randall Hauch
 */
public interface LocalizationRepository {

    /**
     * Obtain the URL to the properties file containing the localized messages given the supplied bundle name. This method is
     * responsible for searching to find the most appropriate localized messages given the locale, but does not need to search
     * using the {@link Locale#getDefault() default locale} (as that is done by the {@link I18n#text(Object...) calling} method.
     * @param bundleName the name of the bundle of properties; never null
     * @param locale the locale for which the properties file URL is desired
     * @return the URL to the properties file containing the localized messages for the named bundle, or null if no such bundle
     * could be found
     */
    URL getLocalizationBundle( String bundleName, Locale locale );

}
