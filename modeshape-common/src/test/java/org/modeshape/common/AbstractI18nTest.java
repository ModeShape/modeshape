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
package org.modeshape.common;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Set;
import org.modeshape.common.i18n.I18n;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public abstract class AbstractI18nTest {

    private Class<?> i18nClass;

    protected AbstractI18nTest( Class<?> i18nClass ) {
        this.i18nClass = i18nClass;
    }

    @Test
    public void shouldNotHaveProblems() throws Exception {
        for (Field fld : i18nClass.getDeclaredFields()) {
            if (fld.getType() == I18n.class && (fld.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
                && (fld.getModifiers() & Modifier.STATIC) == Modifier.STATIC
                && (fld.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
                I18n i18n = (I18n)fld.get(null);
                if (i18n.hasProblem()) {
                    fail(i18n.problem());
                }
            }
        }
        // Check for global problems after checking field problems since global problems are detected lazily upon field usage
        Set<Locale> locales = I18n.getLocalizationProblemLocales(i18nClass);
        if (!locales.isEmpty()) {
            for (Locale locale : locales) {
                Set<String> problems = I18n.getLocalizationProblems(i18nClass, locale);
                try {
                    assertThat(problems.isEmpty(), is(true));
                } catch (AssertionError error) {
                    fail(problems.iterator().next());
                }
            }
        }
    }
}
