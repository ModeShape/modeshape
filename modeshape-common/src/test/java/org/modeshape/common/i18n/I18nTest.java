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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.SystemFailureException;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
public final class I18nTest {

    @BeforeClass
    public static void beforeClass() {
        Locale.setDefault(Locale.US);
    }

    @Before
    public void beforeEach() throws Exception {
        clearFields(TestI18n.class);
        clearFields(TestI18nDuplicateProperty.class);
        clearFields(TestI18nFinal.class);
        clearFields(TestI18nFinalField.class);
        clearFields(TestI18nInterface.class);
        clearFields(TestI18nMissingLocalization.class);
        clearFields(TestI18nMissingProperty.class);
        clearFields(TestI18nNotPublicField.class);
        clearFields(TestI18nNotStaticField.class);
        clearFields(TestI18nPrivate.class);
        clearFields(TestI18nUnusedProperty.class);
        for (Entry<Locale, Map<Class<?>, Set<String>>> localeToMapEntry : I18n.LOCALE_TO_CLASS_TO_PROBLEMS_MAP.entrySet()) {
            for (Iterator<Entry<Class<?>, Set<String>>> iter = localeToMapEntry.getValue().entrySet().iterator(); iter.hasNext();) {
                if (iter.next().getKey() != CommonI18n.class) {
                    iter.remove();
                }
            }
        }
    }

    private void clearFields( Class<?> i18nClass ) throws Exception {
        for (Field fld : i18nClass.getDeclaredFields()) {
            if (fld.getType() == I18n.class && (fld.getModifiers() & Modifier.PUBLIC) == Modifier.PUBLIC
                && (fld.getModifiers() & Modifier.STATIC) == Modifier.STATIC
                && (fld.getModifiers() & Modifier.FINAL) != Modifier.FINAL) {
                fld.set(null, null);
            }
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLocalizationProblemLocalesIfNoClassSupplied() {
        I18n.getLocalizationProblemLocales(null);
    }

    @Test
    public void shouldNeverReturnNullWhenGettingLocalizationProblemLocales() {
        assertThat(I18n.getLocalizationProblemLocales(TestI18n.class), notNullValue());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLocalizationProblemsForDefaultLocaleIfNoClassSupplied() {
        I18n.getLocalizationProblems(null);
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToGetLocalizationProblemsForSuppliedLocaleIfNoClassSupplied() {
        I18n.getLocalizationProblems(null, Locale.US);
    }

    @Test
    public void shouldNeverReturnNullWhenGettingLocalizationProblemsForDefaultLocale() {
        assertThat(I18n.getLocalizationProblems(TestI18n.class), notNullValue());
    }

    @Test
    public void shouldNeverReturnNullWhenGettingLocalizationProblemsForSuppliedLocale() {
        assertThat(I18n.getLocalizationProblems(TestI18n.class, Locale.US), notNullValue());
    }

    @Test
    public void shouldNotHaveLocalizationProblemsAfterInitializationButBeforeLocalization() {
        I18n.initialize(TestI18nUnusedProperty.class);
        assertThat(I18n.getLocalizationProblems(TestI18nUnusedProperty.class, null).isEmpty(), is(true));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nUnusedProperty.class).isEmpty(), is(true));
    }

    @Test
    public void shouldGetLocalizationProblemsForDefaultLocaleIfNoLocaleSupplied() {
        I18n.initialize(TestI18nUnusedProperty.class);
        TestI18nUnusedProperty.testMessage.text("test");
        assertThat(I18n.getLocalizationProblems(TestI18nUnusedProperty.class, null).isEmpty(), is(false));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInitializeIfNoClassSupplied() {
        I18n.initialize(null);
    }

    @Test( expected = SystemFailureException.class )
    public void shouldFailToInitializeFinalI18nField() {
        try {
            I18n.initialize(TestI18nFinalField.class);
        } catch (SystemFailureException err) {
            assertThat(err.getMessage(), is(CommonI18n.i18nFieldFinal.text("testMessage", TestI18nFinalField.class)));
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = SystemFailureException.class )
    public void shouldFailToInitializeNonPublicI18nField() {
        try {
            I18n.initialize(TestI18nNotPublicField.class);
        } catch (SystemFailureException err) {
            assertThat(err.getMessage(), is(CommonI18n.i18nFieldNotPublic.text("testMessage", TestI18nNotPublicField.class)));
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = SystemFailureException.class )
    public void shouldFailToInitializeNonStaticI18nField() {
        try {
            I18n.initialize(TestI18nNotStaticField.class);
        } catch (SystemFailureException err) {
            assertThat(err.getMessage(), is(CommonI18n.i18nFieldNotStatic.text("testMessage", TestI18nNotStaticField.class)));
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void shouldInitializeFinalClasses() {
        I18n.initialize(TestI18nFinal.class);
        assertThat(TestI18nFinal.testMessage, instanceOf(I18n.class));
    }

    @Test
    public void shouldInitializePrivateClasses() {
        I18n.initialize(TestI18nPrivate.class);
        assertThat(TestI18nPrivate.testMessage, instanceOf(I18n.class));
    }

    @Test
    public void shouldInitializeI18nFields() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage, instanceOf(I18n.class));
    }

    @Test
    public void shouldNotInitializeNonI18nFields() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.nonI18n, nullValue());
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailToInitializeInterfaces() {
        try {
            I18n.initialize(TestI18nInterface.class);
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage(), is(CommonI18n.i18nClassInterface.text(TestI18nInterface.class.getName())));
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void shouldProvideIdempotentInitialization() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage1.text("test"), is("test"));
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage1.text("test"), is("test"));
    }

    @Test
    public void shouldNotBeLocalizedAfterInitialization() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        assertThat(TestI18nDuplicateProperty.testMessage.localeToTextMap.get(Locale.getDefault()), nullValue());
        assertThat(TestI18nDuplicateProperty.testMessage.localeToProblemMap.get(Locale.getDefault()), nullValue());
    }

    @Test
    public void shouldHaveIdThatMatchesFieldName() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage.id(), is("testMessage"));
    }

    @Test
    public void shouldNotBeLocalizedIfAskedForId() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        TestI18nDuplicateProperty.testMessage.id();
        assertThat(TestI18nDuplicateProperty.testMessage.localeToTextMap.get(Locale.getDefault()), nullValue());
        assertThat(TestI18nDuplicateProperty.testMessage.localeToProblemMap.get(Locale.getDefault()), nullValue());
    }

    @Test
    public void shouldBeLocalizedIfAskedIfHasProblem() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        TestI18nDuplicateProperty.testMessage.hasProblem();
        assertThat(TestI18nDuplicateProperty.testMessage.localeToTextMap.get(Locale.getDefault()), notNullValue());
        assertThat(TestI18nDuplicateProperty.testMessage.localeToProblemMap.get(Locale.getDefault()), notNullValue());
    }

    @Test
    public void shouldBeLocalizedIfAskedForProblem() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        TestI18nDuplicateProperty.testMessage.problem();
        assertThat(TestI18nDuplicateProperty.testMessage.localeToTextMap.get(Locale.getDefault()), notNullValue());
        assertThat(TestI18nDuplicateProperty.testMessage.localeToProblemMap.get(Locale.getDefault()), notNullValue());
    }

    @Test
    public void shouldBeLocalizedIfConvertedToString() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        TestI18nDuplicateProperty.testMessage.toString();
        assertThat(TestI18nDuplicateProperty.testMessage.localeToTextMap.get(Locale.getDefault()), notNullValue());
        assertThat(TestI18nDuplicateProperty.testMessage.localeToProblemMap.get(Locale.getDefault()), notNullValue());
    }

    @Test
    public void shouldContainAngleBracketedProblemInTextIfMissingLocalization() {
        I18n.initialize(TestI18nMissingLocalization.class);
        String text = TestI18nMissingLocalization.testMessage.text();
        assertThat(text,
                   is('<' + CommonI18n.i18nLocalizationProblems.text(TestI18nMissingLocalization.class, Locale.getDefault()) + '>'));
        System.out.println("Text: " + text);
    }

    @Test
    public void shouldHaveProblemIfMissingLocalization() {
        I18n.initialize(TestI18nMissingLocalization.class);
        assertThat(TestI18nMissingLocalization.testMessage.hasProblem(), is(true));
        String problem = TestI18nMissingLocalization.testMessage.problem();
        assertThat(problem, is(CommonI18n.i18nLocalizationProblems.text(TestI18nMissingLocalization.class, Locale.getDefault())));
        System.out.println("Problem: " + problem);
    }

    @Test
    public void shouldHaveLocalicationProblemIfMissingLocalization() {
        I18n.initialize(TestI18nMissingLocalization.class);
        TestI18nMissingLocalization.testMessage.text();
        assertThat(I18n.getLocalizationProblems(TestI18nMissingLocalization.class).size(), is(1));
        assertThat(I18n.getLocalizationProblems(TestI18nMissingLocalization.class).iterator().next(),
                   is(CommonI18n.i18nLocalizationFileNotFound.text(TestI18nMissingLocalization.class.getName())));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nMissingLocalization.class).size(), is(1));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nMissingLocalization.class).iterator().next(),
                   is(Locale.getDefault()));
    }

    @Test
    public void shouldHaveTextIfPropertyDuplicate() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        String text = TestI18nDuplicateProperty.testMessage.text("test");
        assertThat(text.charAt(0), not('<'));
        System.out.println("Text: " + text);
    }

    @Test
    public void shouldHaveProblemIfPropertyDuplicate() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        assertThat(TestI18nDuplicateProperty.testMessage.hasProblem(), is(true));
        String problem = TestI18nDuplicateProperty.testMessage.problem();
        assertThat(problem, notNullValue());
        System.out.println("Problem: " + problem);
    }

    @Test
    public void shouldNotHaveLocalicationProblemIfPropertyDuplicate() {
        I18n.initialize(TestI18nDuplicateProperty.class);
        TestI18nDuplicateProperty.testMessage.text("test");
        assertThat(I18n.getLocalizationProblems(TestI18nDuplicateProperty.class).isEmpty(), is(true));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nDuplicateProperty.class).isEmpty(), is(true));
    }

    @Test
    public void shouldContainAngleBracketedProblemInTextIfPropertyMissing() {
        I18n.initialize(TestI18nMissingProperty.class);
        String text = TestI18nMissingProperty.testMessage1.text("test");
        assertThat(text.charAt(0), is('<'));
        System.out.println("Text: " + text);
    }

    @Test
    public void shouldHaveProblemIfPropertyMissing() {
        I18n.initialize(TestI18nMissingProperty.class);
        assertThat(TestI18nMissingProperty.testMessage1.hasProblem(), is(true));
        String problem = TestI18nMissingProperty.testMessage1.problem();
        assertThat(problem, notNullValue());
        System.out.println("Problem: " + problem);
    }

    @Test
    public void shouldNotHaveLocalicationProblemIfPropertyMissing() {
        I18n.initialize(TestI18nMissingProperty.class);
        TestI18nMissingProperty.testMessage1.text("test");
        assertThat(I18n.getLocalizationProblems(TestI18nMissingProperty.class).isEmpty(), is(true));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nMissingProperty.class).isEmpty(), is(true));
    }

    @Test
    public void shouldHaveLocalicationProblemIfPropertyUnused() {
        I18n.initialize(TestI18nUnusedProperty.class);
        TestI18nUnusedProperty.testMessage.text("test");
        assertThat(I18n.getLocalizationProblems(TestI18nUnusedProperty.class).size(), is(1));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nUnusedProperty.class).size(), is(1));
        assertThat(I18n.getLocalizationProblemLocales(TestI18nUnusedProperty.class).iterator().next(), is(Locale.getDefault()));
    }

    @Test
    public void shouldHaveTextMatchingLocalizationFilePropertyValue() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage.text(), is("Test Message"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailIfTooFewArgumentsSuppliedToText() {
        I18n.initialize(TestI18n.class);
        try {
            TestI18n.testMessage1.text();
        } catch (IllegalArgumentException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldFailIfTooManyArgumentsSuppliedToText() {
        I18n.initialize(TestI18n.class);
        try {
            TestI18n.testMessage1.text("Test", "Message");
        } catch (IllegalArgumentException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void shouldContainArgumentsInRightOrderInText() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage2.text("Test", "Message"), is("Message Test"));
    }

    @Test
    public void shouldAllowReuseOfArgumentsInText() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage3.text("Test", "Message"), is("Message Test Message"));
    }

    @Test
    public void shouldContainLocaleSpecificText() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage.text(Locale.FRENCH), is("Message de Test"));
    }

    @Test
    public void shouldContainTextForDefaultLocaleIfMissingLocalizationForSuppliedLocale() {
        I18n.initialize(TestI18n.class);
        assertThat(TestI18n.testMessage.text(Locale.CHINESE), is("Test Message"));
    }

    public static class TestI18n {

        public static I18n testMessage;
        public static I18n testMessage1;
        public static I18n testMessage2;
        public static I18n testMessage3;
        public static Object nonI18n;
    }

    private static class TestI18nDuplicateProperty {

        public static I18n testMessage;
    }

    public static final class TestI18nFinal {

        public static I18n testMessage;
    }

    public static class TestI18nFinalField {

        public static final I18n testMessage = null;
    }

    public static interface TestI18nInterface {

        I18n testMessage = null;
    }

    private static class TestI18nMissingProperty {

        @SuppressWarnings( "unused" )
        public static I18n testMessage;
        public static I18n testMessage1;
    }

    public static class TestI18nNotPublicField {

        static I18n testMessage;
    }

    public static class TestI18nNotStaticField {

        public I18n testMessage;
    }

    private static class TestI18nPrivate {

        public static I18n testMessage;
    }

    private static class TestI18nUnusedProperty {

        public static I18n testMessage;
    }

    private static class TestI18nMissingLocalization {

        public static I18n testMessage;
    }
}
