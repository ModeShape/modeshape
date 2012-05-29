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

import java.util.Locale;

/**
 * Interface which should be implemented by any i18n compliant resource.
 *
 * @author Horia Chiorean
 */
public interface I18nResource {

    /**
     * Get the localized text for the {@link Locale#getDefault() current (default) locale}, replacing the parameters in the text
     * with those supplied.
     *
     * @param arguments the arguments for the parameter replacement; may be <code>null</code> or empty
     * @return the localized text
     */
    public String text(Object...arguments);

    /**
     * Get the localized text for the supplied locale, replacing the parameters in the text with those supplied.
     *
     * @param locale the locale, or <code>null</code> if the {@link Locale#getDefault() current (default) locale} should be used
     * @param arguments the arguments for the parameter replacement; may be <code>null</code> or empty
     * @return the localized text
     */
    public String text(Locale locale, Object...arguments);
}
