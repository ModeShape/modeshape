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

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.SystemFailureException;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.ClassUtil;
import org.jboss.dna.common.util.HashCode;

/**
 * Manages the initialization of internationalization (i18n) files, substitution of values within i18n message placeholders, and
 * dynamically reading properties from i18n property files.
 *
 * @author John Verhaeg
 * @author Randall Hauch
 */
@ThreadSafe
public final class I18n {

	private static final Pattern PARAMETER_COUNT_PATTERN = Pattern.compile("\\{(\\d+)\\}");
	private static final Object[] EMPTY_ARGUMENTS = new Object[] {};
	private static final LocalizationRepository DEFAULT_LOCALIZATION_REPOSITORY = new ClasspathLocalizationRepository();
	private static final ConcurrentMap<Localization, Map<String, String>> LOCALIZATION_2_ID_2_ERROR_MAP = new ConcurrentHashMap<Localization, Map<String, String>>();
	private static final ConcurrentMap<Localization, Map<String, String>> LOCALIZATION_2_ID_2_TEXT_MAP = new ConcurrentHashMap<Localization, Map<String, String>>();

	private static LocalizationRepository localizationRepository = DEFAULT_LOCALIZATION_REPOSITORY;

	/**
	 * @param i18nClass An internalization class for which errors should be returned.
	 * @return The errors encountered while initializing the specified internationalization class for the default locale mapped to
	 *         the applicable internalization IDs; never <code>null</code>.
	 */
	public static Map<String, String> getErrorsForDefaultLocale( Class i18nClass ) {
		return getErrorsForLocale(i18nClass, null);
	}

	/**
	 * @param i18nClass An internalization class for which errors should be returned.
	 * @param locale The locale for which errors should be returned. If <code>null</code>, errors for the the default locale
	 *        will be returned.
	 * @return The errors encountered while initializing the specified internationalization class for the specified locale mapped
	 *         to the applicable internalization IDs; never <code>null</code>.
	 */
	public static Map<String, String> getErrorsForLocale( Class i18nClass,
	                                                      Locale locale ) {
		ArgCheck.isNotNull(i18nClass, "i18nClass");
		Map<String, String> errors = LOCALIZATION_2_ID_2_ERROR_MAP.get(new Localization(
		                                                                                locale == null ? Locale.getDefault() : locale,
		                                                                                i18nClass));
		if (errors == null) {
			errors = Collections.emptyMap();
		}
		return errors;
	}

	/**
	 * Get the repository of localized messages. By default, this instance uses a {@link ClasspathLocalizationRepository} that
	 * uses this class' classloader.
	 *
	 * @return localizationRepository
	 */
	public static LocalizationRepository getLocalizationRepository() {
		return localizationRepository;
	}

	/**
	 * Set the repository of localized messages. If null, a {@link ClasspathLocalizationRepository} instance that uses this class
	 * loader will be used.
	 *
	 * @param localizationRepository the localization repository to use; may be null if the default repository should be used.
	 */
	public static void setLocalizationRepository( LocalizationRepository localizationRepository ) {
		I18n.localizationRepository = localizationRepository != null ? localizationRepository : DEFAULT_LOCALIZATION_REPOSITORY;
	}

	/**
	 * Initializes the internationalization fields declared on the specified class. Internationalization fields must be public,
	 * static, not final, and of type <code>I18n</code>. The specified class must not be an interface (of course), but has no
	 * restrictions as to what class it may extend or what interfaces it must implement.
	 *
	 * @param i18nClass A class declaring one or more public, static, non-final fields of type <code>I18n</code>.
	 */
	public static void initialize( Class i18nClass ) {
		ArgCheck.isNotNull(i18nClass, "i18nClass");
		if (i18nClass.isInterface()) {
			throw new IllegalArgumentException(CommonI18n.i18nClassInterface.text(i18nClass.getName()));
		}

		// Find all public static non-final String fields in the specified class and instantiate an I18n object for each.
		try {
			for (Field fld : i18nClass.getDeclaredFields()) {

				// Ensure field is of type I18n
				if (fld.getType() == I18n.class) {

					// Ensure field is not final
					if ((fld.getModifiers() & Modifier.FINAL) == Modifier.FINAL) {
						throw new SystemFailureException(CommonI18n.i18nFieldFinal.text(fld.getName(), i18nClass));
					}

					// Ensure field is public
					if ((fld.getModifiers() & Modifier.PUBLIC) != Modifier.PUBLIC) {
						throw new SystemFailureException(CommonI18n.i18nFieldNotPublic.text(fld.getName(), i18nClass));
					}

					// Ensure field is static
					if ((fld.getModifiers() & Modifier.STATIC) != Modifier.STATIC) {
						throw new SystemFailureException(CommonI18n.i18nFieldNotStatic.text(fld.getName(), i18nClass));
					}

					// Ensure we can access field even if it's in a private class
					ClassUtil.makeAccessible(fld);

					// Initialize field. Do this every time the class is initialized (or re-initialized)
					fld.set(null, new I18n(fld.getName(), i18nClass));
				}
			}
		} catch (IllegalAccessException err) {
			throw new IllegalArgumentException(CommonI18n.i18nClassNotPublic.text(i18nClass.getName()));
		}
	}

	/**
	 * Synchronized on the locale being loaded (this blocks loading all other locales, but not reads for existing locales)
	 *
	 * @param i18nClass The internalization class being initialized
	 * @param localization
	 * @return
	 */
	private synchronized static Map<String, String> initializeIdToTextMap( final Localization localization ) {
		Map<String, String> id2TextMap = new HashMap<String, String>();

		// Put in the new map and see if there's already one there ...
		Map<String, String> existingId2TextMap = LOCALIZATION_2_ID_2_TEXT_MAP.putIfAbsent(localization, id2TextMap);
		// If there is already an existing map, then someone beat us to the punch and there's nothing to do ...
		if (existingId2TextMap != null) {
			return existingId2TextMap;
		}
		// We're the first to put in the map for this locale, so populate the one we created...

		// Get the URL to the localization properties file ...
		final LocalizationRepository repos = getLocalizationRepository();
		final String bundleName = localization.i18nClass.getName();
		URL url = null;
		Locale locale = new Locale(localization.language, localization.country, localization.variant);
		url = repos.getLocalizationBundle(bundleName, locale);

		// Try the default locale (if it is different than the supplied locale) ...
		if (url == null) {
			// Nothing was found, so try the default locale
			Locale defaultLocale = Locale.getDefault();
			if (!defaultLocale.equals(locale)) {
				url = repos.getLocalizationBundle(bundleName, defaultLocale);
			}
		}

		String bundleMsg = null;
		if (url == null) {
			// Record no variant of the i18n properties file for the specified class found
			bundleMsg = CommonI18n.i18nPropertiesFileNotFound.text(bundleName);
		} else {
			// Initialize i18n map
			final Map<String, String> finalMap = id2TextMap;
			final URL finalUrl = url;
			Properties props = new Properties() {

				@Override
				public synchronized Object put( Object key,
				                                Object value ) {
					String id = (String)key;
					String text = (String)value;

					try {
						Field fld = localization.i18nClass.getDeclaredField(id);
						if (fld.getType() != I18n.class) {
							// Invalid field type
							mapErrorMessage(localization, id, CommonI18n.i18nFieldInvalidType.text(id,
							                                                                     finalUrl,
							                                                                     getClass().getName()));
						}
					} catch (NoSuchFieldException err) {
						// No corresponding field exists
						mapErrorMessage(localization, id, CommonI18n.i18nPropertyUnused.text(id, finalUrl));
					}

					if (finalMap.put(id, text) != null) {
						// Duplicate id encountered
						mapErrorMessage(localization, id, CommonI18n.i18nPropertyDuplicate.text(id, finalUrl));
					}

					return null;
				}
			};

			try {
				InputStream propStream = url.openStream();
				try {
					props.load(propStream);
				} finally {
					propStream.close();
				}
			} catch (IOException err) {
				bundleMsg = err.getMessage();
			}
		}

		// Check for uninitialized fields
		for (Field fld : localization.i18nClass.getDeclaredFields()) {
			if (fld.getType() == I18n.class && id2TextMap.get(fld.getName()) == null) {
				mapErrorMessage(localization,
				                fld.getName(),
				                bundleMsg == null ? CommonI18n.i18nPropertyMissing.text(fld.getName(), url) : bundleMsg);
			}
		}

		return id2TextMap;
	}

	static void mapErrorMessage( Localization localization,
	                             String id,
	                             String message ) {
		Map<String, String> id2ErrorMap = new ConcurrentHashMap<String, String>();
		Map<String, String> existingId2ErrorMap = LOCALIZATION_2_ID_2_ERROR_MAP.putIfAbsent(localization, id2ErrorMap);
		if (existingId2ErrorMap != null) {
			id2ErrorMap = existingId2ErrorMap;
		}
		message = id2ErrorMap.put(id, message);
		assert message == null;
	}

	/**
	 * Substitute the arguments into the message, ensuring that the number of arguments matches the number of parameters in the
	 * text.
	 *
	 * @param id the id of the internationalization object
	 * @param text
	 * @param arguments
	 * @return the text with parameters replaced
	 */
	protected static String replaceParameters( String id,
	                                           String text,
	                                           Object... arguments ) {
		if (arguments == null) arguments = EMPTY_ARGUMENTS;
		Matcher matcher = PARAMETER_COUNT_PATTERN.matcher(text);
		StringBuffer newText = new StringBuffer();
		int argCount = 0;
		boolean err = false;
		while (matcher.find()) {
			int ndx = Integer.valueOf(matcher.group(1));
			if (argCount <= ndx) {
				argCount = ndx + 1;
			}
			if (ndx >= arguments.length) {
				err = true;
				matcher.appendReplacement(newText, matcher.group());
			} else {
				Object arg = arguments[ndx];
				if (arg != null) {
					matcher.appendReplacement(newText, Matcher.quoteReplacement(arg.toString()));
				} else {
					matcher.appendReplacement(newText, Matcher.quoteReplacement("null"));
				}
			}
		}
		if (err || argCount < arguments.length) {
			I18n msg = null;
			if (id != null) {
				if (arguments.length == 1) {
					msg = CommonI18n.i18nArgumentMismatchedParameters;
				} else if (argCount == 1) {
					msg = CommonI18n.i18nArgumentsMismatchedParameter;
				} else {
					msg = CommonI18n.i18nArgumentsMismatchedParameters;
				}
				throw new IllegalArgumentException(msg.text(arguments.length, id, argCount, text, newText.toString()));
			}
			if (arguments.length == 1) {
				msg = CommonI18n.i18nReplaceArgumentMismatchedParameters;
			} else if (argCount == 1) {
				msg = CommonI18n.i18nReplaceArgumentsMismatchedParameter;
			} else {
				msg = CommonI18n.i18nReplaceArgumentsMismatchedParameters;
			}
			throw new IllegalArgumentException(msg.text(arguments.length, argCount, text, newText.toString()));
		}
		matcher.appendTail(newText);

		return newText.toString();
	}

	/**
	 * Substitute the arguments into the message, ensuring that the number of arguments matches the number of parameters in the
	 * text.
	 *
	 * @param text
	 * @param arguments
	 * @return the text with parameters replaced
	 */
	public static String replaceParameters( String text,
	                                        Object... arguments ) {
		return replaceParameters(null, text, arguments);
	}

	final String id;
	private final Class i18nClass;

	private I18n( String id,
	              Class i18nClass ) {
		this.id = id;
		this.i18nClass = i18nClass;
	}

	private String rawText( Locale locale ) {
		assert locale != null;
		Map<String, String> id2TextMap = null;
		final Localization localization = new Localization(locale, i18nClass);
		id2TextMap = LOCALIZATION_2_ID_2_TEXT_MAP.get(localization);
		if (id2TextMap == null) {
			id2TextMap = initializeIdToTextMap(localization);
		}

		return id2TextMap.get(id);
	}

	/**
	 * Get the internationalized text localized to the {@link Locale#getDefault() current (default) locale}, replacing the
	 * parameters in the text with those supplied.
	 *
	 * @param arguments the arguments for the parameter replacement; may be null or empty
	 * @return the localized text
	 */
	public String text( Object... arguments ) {
		return text(null, arguments);
	}

	/**
	 * Get the internationalized text localized to the supplied locale, replacing the parameters in the text with those supplied.
	 *
	 * @param locale the locale, or null if the {@link Locale#getDefault() current (default) locale} should be used
	 * @param arguments the arguments for the parameter replacement; may be null or empty
	 * @return the localized text
	 */
	public String text( Locale locale,
	                    Object... arguments ) {
		if (locale == null) {
			locale = Locale.getDefault();
		}
		String rawText = rawText(locale);
		if (rawText == null) {
			Map<String, String> id2ErrorMap = LOCALIZATION_2_ID_2_ERROR_MAP.get(new Localization(locale, i18nClass));
			if (id2ErrorMap != null) {
				String msg = id2ErrorMap.get(id);
				if (msg != null) {
					return '<' + msg + '>';
				}
			}
		}
		return replaceParameters(id, rawText, arguments);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String toString() {
		return rawText(Locale.getDefault());
	}

	@Immutable
	private static class Localization {

		final String language;
		final String country;
		final String variant;
		final Class i18nClass;

		Localization( Locale locale,
		              Class i18nClass ) {
			this.language = locale.getLanguage();
			this.country = locale.getCountry();
			this.variant = locale.getVariant();
			this.i18nClass = i18nClass;
		}

		/**
		 * <p>
		 * {@inheritDoc}
		 * </p>
		 *
		 * @see java.lang.Object#hashCode()
		 */
		@Override
		public int hashCode() {
			return HashCode.compute(country, i18nClass, language, variant);
		}

		/**
		 * <p>
		 * {@inheritDoc}
		 * </p>
		 *
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals( Object obj ) {
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			Localization other = (Localization)obj;
			if (country == null) {
				if (other.country != null) return false;
			} else if (!country.equals(other.country)) {
				return false;
			}
			if (i18nClass == null) {
				if (other.i18nClass != null) return false;
			} else if (!i18nClass.equals(other.i18nClass)) {
				return false;
			}
			if (language == null) {
				if (other.language != null) return false;
			} else if (!language.equals(other.language)) {
				return false;
			}
			if (variant == null) {
				if (other.variant != null) return false;
			} else if (!variant.equals(other.variant)) {
				return false;
			}
			return true;
		}
	}
}
