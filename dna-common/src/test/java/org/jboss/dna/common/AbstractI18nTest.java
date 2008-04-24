/*
 *
 */
package org.jboss.dna.common;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.junit.Test;

/**
 * @author John Verhaeg
 */
public abstract class AbstractI18nTest {

    private Class i18nClass;

    protected AbstractI18nTest( Class i18nClass ) {
        this.i18nClass = i18nClass;
    }

    @Test
    @SuppressWarnings( "unchecked" )
    public void shouldNotHaveLocalizationProblems() throws Exception {
        Method method = i18nClass.getDeclaredMethod("getLocalizationProblemLocales", (Class[])null);
        Set<Locale> locales = (Set<Locale>)method.invoke(null, (Object[])null);
        method = i18nClass.getDeclaredMethod("getLocalizationProblems", Locale.class);
        for (Locale locale : locales) {
            assertThat(((Set<String>)method.invoke(null, locale)).isEmpty(), is(true));
        }
    }

    @Test
    public void shouldNotHaveProblems() throws IllegalAccessException {
        for (Field fld : i18nClass.getDeclaredFields()) {
            if (fld.getType() == I18n.class && (fld.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC && (fld.getModifiers() & Modifier.STATIC) == Modifier.STATIC
                && (fld.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
                I18n i18n = (I18n)fld.get(null);
                if (i18n.hasProblem()) {
                    fail();
                }
            }
        }
    }
}
