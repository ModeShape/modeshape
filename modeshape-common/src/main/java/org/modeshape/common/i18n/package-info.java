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
/**
 * <p>
 * A simple framework for defining internationalized strings and obtaining the localized forms.
 * </p>
 * <p>
 * The {@link I18n} class represents an internationalized string with the ability to obtain the localized message
 * given a {@link java.util.Locale Locale} or, if not supplied, the {@link java.util.Locale#getDefault() default locale}.
 * </p>
 * <h3>Using the I18n objects</h3>
 * <p>
 * To use, simply create a class with a public static non-final I18n instance for each of the internationalized strings.
 * The name of the field is used as the message key in the localization files, so use a name that is meaningful
 * within the codebase. The class should also have a static initializer to populate the I18n instances.  For example,
 * here is a class that defines three internationalized strings:
 * <pre>
 * public final class MyAppI18n {
 *
 *   public static I18n errorImportingContent;
 *   public static I18n unableToFindSourceWithName;
 *   public static I18n executorIsShutdown;
 *
 *   static {
 *       try {
 *           I18n.initialize(MyAppI18n.class);
 *       } catch (final Exception err) {
 *           System.err.println(err);
 *       }
 *   }
 * }
 * </pre>
 * You can have as many of these classes in your application or library, so you have the flexibility to organize
 * your I18n objects to suit your needs. You could, for example, define one of these classes in each of your
 * applications modules, or you could even define one in each package.
 * </p>
 * <h3>Localization</h3>
 * <p>
 * The localized strings are loaded from property files based upon the name of the locale and the class containing
 * the I18n message objects (the one with the static intializer):
 * <pre>
 *   &lt;package&gt;.&lt;className&gt;_&lt;localeName&gt;.properties
 * </pre>
 * For example, here are the names of some localization bundles for the "<code>org.example.MyAppI18n</code>" class:
 * <ul>
 *  <li>"<code>org.example.MyAppI18n.properties_en</code>" is the bundle containing the English strings</li>
 *  <li>"<code>org.example.MyAppI18n.properties_fr</code>" is the bundle containing the French strings</li>
 *  <li>"<code>org.example.MyAppI18n.properties</code>" is the bundle used if no locale is specified</li>
 * </ul>
 * </p>
 * <p>
 * Each of these files is a simple properties file, where the property names (the keys) must match the I18n field names,
 * and the property values are the localized message.  So, given the "<code>org.example.MyAppI18n</code>" class above,
 * here is an example of a localization bundle:
 * <pre>
 * errorImportingContent = Error importing {0} content from {1}
 * unableToFindSourceWithName = Unable to find a source named "{0}"
 * executorIsShutdown = The executor is already shutdown
 * </pre>
 * Note that each of the localized messages may contain number parameters that are replaced with actual values
 * supplied to the {@link I18n#text(Object...)} or {@link I18n#text(java.util.Locale, Object...)} methods.
 * </p>
 * <h3>Localization Repositories</h3>
 * <p>
 * By default, the localization bundles must be found on the classpath. This makes sense for most applications that
 * include their localization bundles with their JARs or make them available on the classpath.
 * </p>
 * <p>
 * However, it is possible to access the localization bundles from other sources using the {@link LocalizationRepository}
 * interface that defines a single method for obtaining the {@link java.net.URL URL} for a given bundle name
 * and Locale. The resulting URL must be capable of accessing the bundle contents via the {@link java.net.URL#openStream()} method.
 * </p>
 * <p>
 * To specify that your localization repository should be used, simply call the {@link I18n#setLocalizationRepository(LocalizationRepository)}
 * method once within your application.
 * </p>
 * <h3>Testing the localizations</h3>
 * <p>
 * The framework provides several utility methods to obtain any problems the were found while loading the localized
 * messages.
 * <ul>
 *  <li>{@link I18n#getLocalizationProblemLocales(Class)} returns the set of Locale objects for which there were problems;</li>
 *  <li>{@link I18n#getLocalizationProblems(Class)} returns the set of problems found while loading all of the locales;</li>
 *  <li>{@link I18n#getLocalizationProblems(Class,java.util.Locale)} returns the set of problems found while loading the supplied locale</li>
 * </ul>
 * Problems include any missing messages in a localization file, extra messages in a localization file, or inability
 * to access the localization file.
 * </p>
 * <p>
 * These utility methods can be used in unit tests to ensure that all locale message bundles were loaded successfully.
 * In fact, this framework provides an abstract unit test that does exactly this.  To use simply create a concrete
 * subclass with a no-arg constructor that calls the abstract class's constructor with the {@link Class} containing
 * the {@link I18n} objects:
 * <pre>
 * public class MyAppI18nTest extends AbstractI18nTest {
 *    public MyAppI18nTest() {
 *        super(MyAppI18n.class);
 *    }
 * }
 * </pre>
 * </p>
 */

package org.modeshape.common.i18n;

