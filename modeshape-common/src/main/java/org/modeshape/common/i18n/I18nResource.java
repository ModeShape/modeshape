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
