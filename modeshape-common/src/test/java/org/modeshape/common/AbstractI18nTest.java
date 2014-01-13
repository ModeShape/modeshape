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
package org.modeshape.common;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Set;
import org.junit.Test;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;

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

    protected static final String[] ANNOTATION_NAMES = {"Description", "Category", "Label"};

    /**
     * Utility method that can be used to verify that an I18n field exists for all of the I18n-related annotations on the supplied
     * object. I18n-related annotations include {@link Description}, {@link Label}, and {@link Category}.
     * 
     * @param annotated the object that has field or method annotations
     * @throws Exception if there is a problem
     */
    protected void verifyI18nForAnnotationsOnObject( Object annotated ) throws Exception {
        // Check the known annotations that work with I18ns ...
        Class<?> clazz = annotated.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            for (Annotation annotation : field.getAnnotations()) {
                verifyI18nForAnnotation(annotation, field);
            }
        }
        for (Method method : clazz.getDeclaredMethods()) {
            for (Annotation annotation : method.getAnnotations()) {
                verifyI18nForAnnotation(annotation, method);
            }
        }
    }

    protected void verifyI18nForAnnotation( Annotation annotation,
                                            Object annotatedObject ) throws Exception {
        String i18nIdentifier;
        Class<?> i18nClass;
        if (annotation instanceof Category) {
            Category cat = (Category)annotation;
            i18nClass = cat.i18n();
            i18nIdentifier = cat.value();
        } else if (annotation instanceof Description) {
            Description desc = (Description)annotation;
            i18nClass = desc.i18n();
            i18nIdentifier = desc.value();
        } else if (annotation instanceof Label) {
            Label label = (Label)annotation;
            i18nClass = label.i18n();
            i18nIdentifier = label.value();
        } else {
            return;
        }
        assertThat(i18nClass, is(notNullValue()));
        assertThat(i18nIdentifier, is(notNullValue()));
        try {
            Field fld = i18nClass.getField(i18nIdentifier);
            assertThat(fld, is(notNullValue()));
            // Now check the I18n field ...
            if (fld.getType() == I18n.class && (fld.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
                && (fld.getModifiers() & Modifier.STATIC) == Modifier.STATIC
                && (fld.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
                I18n i18n = (I18n)fld.get(null);
                if (i18n.hasProblem()) {
                    fail(i18n.problem());
                }
            }
        } catch (NoSuchFieldException e) {
            fail("Missing I18n field on " + i18nClass.getName() + " for " + annotation + " on " + annotatedObject);
        }
    }
}
