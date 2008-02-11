package org.jboss.dna.common;

import org.jboss.dna.common.util.I18n;

/**
 * @author John Verhaeg
 */
public final class CoreI18n {

	static {
		try {
			I18n.initialize(CoreI18n.class);
		} catch (final Exception err) {
			System.err.println(err);
		}
	}

	// Make sure the following I18n.java-related fields are defined before all other fields to ensure a valid error message is
	// produced in the event of a missing/duplicate/unused property

	public static I18n i18nArgumentMismatch;
	public static I18n i18nClassInterface;
	public static I18n i18nFieldFinal;
	public static I18n i18nFieldNotPublic;
	public static I18n i18nFieldNotStatic;
	public static I18n i18nPropertiesFileNotFound;
	public static I18n i18nPropertyDuplicate;
	public static I18n i18nPropertyMissing;
	public static I18n i18nPropertyUnused;

	// Core-related fields
}
