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
