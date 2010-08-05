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
 * Annotation that can be used provide a human-readable category.
 */
@Documented
@Retention( RUNTIME )
@Target( {FIELD, METHOD, CONSTRUCTOR, PACKAGE, TYPE} )
public @interface Category {
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
