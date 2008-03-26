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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import java.util.Locale;
import org.jboss.dna.common.CoreI18n;
import org.jboss.dna.common.SystemFailureException;
import org.junit.Before;
import org.junit.Test;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
public final class I18nTest {

    @Before
    public void beforeEach() {
        I18n.initialize(TestI18n.class);
    }

    @Test
    public void getLocaleShouldReturnDefaultLocale() {
        assertThat(I18n.getLocale(), is(Locale.getDefault()));
    }

    @Test
    public void setLocaleShouldChangeReturnValueOfGetLocale() {
        I18n.setLocale(Locale.ITALY);
        assertThat(I18n.getLocale(), is(Locale.ITALY));
    }

    @Test
    public void setLocaleShouldUseTheDefaultLocaleIfArgumentIsNull() {
        setLocaleShouldChangeReturnValueOfGetLocale();
        I18n.setLocale(null);
        assertThat(I18n.getLocale(), is(Locale.getDefault()));
    }

    @Test
    public void getLocaleShouldReturnLocaleSetOnThread() throws Exception {
        final Exception[] errs = new Exception[1];
        Thread thread = new Thread() {

            @Override
            public void run() {
                try {
                    setLocaleShouldChangeReturnValueOfGetLocale();
                } catch (Exception err) {
                    errs[0] = err;
                }
            }
        };
        if (errs[0] != null) {
            throw errs[0];
        }
        thread.start();
        thread.join();
        assertThat(I18n.getLocale(), is(Locale.getDefault()));
    }

    @Test( expected = IllegalArgumentException.class )
    public void initializeShouldFailIfClassIsNull() {
        I18n.initialize(null);
    }

    @Test
    public void initializeShouldSkipNonI18nFields() {
        assertThat(TestI18n.nonI18n, is(nullValue()));
    }

    @Test( expected = SystemFailureException.class )
    public void initializeShouldFailIfI18nFieldIsFinal() {
        try {
            I18n.initialize(TestI18nFinalField.class);
        } catch (SystemFailureException err) {
            assertThat(err.getMessage(), is(CoreI18n.i18nFieldFinal.text("testMessage", TestI18nFinalField.class)));
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = RuntimeException.class )
    public void initializeShouldFailIfI18nFieldIsNotPublic() {
        try {
            I18n.initialize(TestI18nNotPublicField.class);
        } catch (RuntimeException err) {
            assertThat(err.getMessage(), is(CoreI18n.i18nFieldNotPublic.text("testMessage", TestI18nNotPublicField.class)));
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = RuntimeException.class )
    public void initializeShouldFailIfI18nFieldIsNotStatic() {
        try {
            I18n.initialize(TestI18nNotStaticField.class);
        } catch (RuntimeException err) {
            assertThat(err.getMessage(), is(CoreI18n.i18nFieldNotStatic.text("testMessage", TestI18nNotStaticField.class)));
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void initializeShouldAllowFinalClass() {
        I18n.initialize(TestI18nFinal.class);
    }

    @Test
    public void initializeShouldAllowPrivateClasses() {
        I18n.initialize(TestI18nPrivate.class);
        assertThat(TestI18nPrivate.testMessage, instanceOf(I18n.class));
    }

    @Test
    public void initializeShouldAssignI18nInstanceToI18nFields() {
        assertThat(TestI18n.testMessage, instanceOf(I18n.class));
    }

    @Test( expected = IllegalArgumentException.class )
    public void initializeShouldFailIfClassIsInterface() {
        try {
            I18n.initialize(TestI18nInterface.class);
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage(), is(CoreI18n.i18nClassInterface.text(TestI18nInterface.class.getName())));
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void initializeShouldBeIdempotent() {
        I18n.initialize(TestI18n.class);
    }

    @Test
    public void i18nIdShouldMatchFieldName() throws Exception {
        assertThat(TestI18n.testMessage.id, is("testMessage"));
    }

    @Test( expected = RuntimeException.class )
    public void i18nTextShouldFailIfPropertyDuplicate() throws Exception {
        I18n.initialize(TestI18nDuplicateProperty.class);
        try {
            TestI18nDuplicateProperty.testMessage.text();
        } catch (RuntimeException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = RuntimeException.class )
    public void i18nTextShouldFailIfPropertyMissing() throws Exception {
        I18n.initialize(TestI18nMissingProperty.class);
        try {
            TestI18nMissingProperty.testMessage.text();
        } catch (RuntimeException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = RuntimeException.class )
    public void i18nTextShouldFailIfPropertyUnused() throws Exception {
        I18n.initialize(TestI18nUnusedProperty.class);
        try {
            TestI18nUnusedProperty.testMessage.text();
        } catch (RuntimeException err) {
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void i18nTextShouldMatchPropertiesFile() throws Exception {
        assertThat(TestI18n.testMessage.text(), is("Test Message"));
    }

    @Test( expected = IllegalArgumentException.class )
    public void i18nTextShouldFailIfTooFewArgumentsSpecified() throws Exception {
        try {
            TestI18n.testMessage1.text();
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage(), is(CoreI18n.i18nArgumentsMismatchedParameter.text(0, "testMessage1", 1, "{0}", "{0}")));
            System.err.println(err);
            throw err;
        }
    }

    @Test( expected = IllegalArgumentException.class )
    public void i18nTextShouldFailIfTooManyArgumentsSpecified() throws Exception {
        try {
            TestI18n.testMessage1.text("Test", "Message");
        } catch (IllegalArgumentException err) {
            assertThat(err.getMessage(), is(CoreI18n.i18nArgumentsMismatchedParameter.text(2, "testMessage1", 1, "{0}", "Test")));
            System.err.println(err);
            throw err;
        }
    }

    @Test
    public void i18nTextShouldContainArgumentsInRightOrder() throws Exception {
        assertThat(TestI18n.testMessage2.text("Test", "Message"), is("Message Test"));
    }

    @Test
    public void i18nTextShouldAllowReuseOfArguments() throws Exception {
        assertThat(TestI18n.testMessage3.text("Test", "Message"), is("Message Test Message"));
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
}
