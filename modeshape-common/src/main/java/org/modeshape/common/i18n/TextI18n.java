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
import org.modeshape.common.util.StringUtil;

/**
 * A pass-through implementation of {@link org.modeshape.common.i18n.I18nResource} which uses an underlying text as the real
 * value, ignoring any kind of internationalization.
 */
public final class TextI18n implements I18nResource {
    private static final String BLANK = "";

    private final String text;

    public TextI18n( String text ) {
        this.text = StringUtil.isBlank(text) ? BLANK : text;
    }

    @Override
    public String text( Object... arguments ) {
        return StringUtil.createString(text, arguments);
    }

    @Override
    public String text( Locale locale,
                        Object... arguments ) {
        return StringUtil.createString(text, arguments);
    }
}
