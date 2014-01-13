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
package org.modeshape.common.annotation;

import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import org.modeshape.common.i18n.I18n;

/**
 * Annotation that can be used provide a human-readable label.
 */
@Documented
@Retention( RUNTIME )
@Target( {FIELD, METHOD, CONSTRUCTOR, PACKAGE, TYPE} )
public @interface Label {
    /**
     * The label for the element. This value can either be the literal label, or it can be the {@link I18n#id() identifier} of an
     * {@link I18n} instance on the non-null {@link #i18n()} I18n container class.
     * 
     * @return the issue
     */
    String value();

    /**
     * The class that contains the {@link I18n} instances used for internationalization. This may be null if the description is a
     * literal value.
     * 
     * @return the class that contains the I18n instance identified by the {@link #value()}
     */
    Class<?> i18n();
}
