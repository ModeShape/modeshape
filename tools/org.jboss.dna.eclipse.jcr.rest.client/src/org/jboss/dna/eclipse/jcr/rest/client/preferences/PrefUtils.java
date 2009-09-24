/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.eclipse.jcr.rest.client.preferences;

import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.FILTERED_FILE_EXTENSIONS_PREFERENCE;
import static org.jboss.dna.eclipse.jcr.rest.client.IUiConstants.FILTERED_FOLDER_NAMES_PREFERENCE;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.prefUtilsPropertyFileNotFound;
import static org.jboss.dna.eclipse.jcr.rest.client.RestClientI18n.prefUtilsPropertyNotFound;
import java.io.InputStream;
import java.util.Properties;
import org.eclipse.jface.preference.IPreferenceStore;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.eclipse.jcr.rest.client.Activator;
import org.jboss.dna.eclipse.jcr.rest.client.Utils;
import org.jboss.dna.web.jcr.rest.client.Status;
import org.jboss.dna.web.jcr.rest.client.Status.Severity;

/**
 * The <code>PrefUtils</code> class provides common utilities relating to preferences. This class assumes the Eclipse runtime
 * platform is running.
 */
public final class PrefUtils {

    // =======================================================================================================================
    // Constants
    // =======================================================================================================================

    /**
     * The name of the properties file located in this package.
     */
    private static final String PREFERENCES_FILE = "dnaPrefs.properties"; //$NON-NLS-1$

    /**
     * The preference name for the delimiter that separates filtered file extensions when the preference value is stored.
     */
    private static final String FILE_EXT_DELIMITER_PREF_NAME = "fileExtension.delimiter"; //$NON-NLS-1$

    /**
     * The delimiter that separates filtered file extensions when the preference value is stored.
     */
    public static final char FILE_EXT_DELIMITER;

    /**
     * The preference name for characters that are <strong>NOT</strong> allowed to appear in a file extension.
     */
    private static final String FILE_EXT_INVALID_CHARS_PREF_NAME = "fileExtension.invalidChars"; //$NON-NLS-1$

    /**
     * The characters that are <strong>NOT</strong> allowed to appear in a file extension.
     */
    public static final String FILE_EXT_INVALID_CHARS;

    /**
     * The preference name for the delimiter that separates filtered folder names when the preference value is stored.
     */
    private static final String FOLDER_NAME_DELIMITER_PREF_NAME = "folderName.delimiter"; //$NON-NLS-1$

    /**
     * The delimiter that separates filtered folder names when the preference value is stored.
     */
    public static final char FOLDER_NAME_DELIMITER;

    /**
     * The preference name for characters that are <strong>NOT</strong> allowed to appear in a folder name.
     */
    private static final String FOLDER_NAME_INVALID_CHARS_PREF_NAME = "folderName.invalidChars"; //$NON-NLS-1$

    /**
     * The characters that are <strong>NOT</strong> allowed to appear in a folder name.
     */
    public static final String FOLDER_NAME_INVALID_CHARS;

    // =======================================================================================================================
    // Class Initializer
    // =======================================================================================================================

    static {
        InputStream is = null;
        Properties preferenceProperties = new Properties();

        // get the properties file
        try {
            is = PrefUtils.class.getResourceAsStream(PREFERENCES_FILE);
        } catch (Exception e) {
            Activator.getDefault().log(new Status(Severity.ERROR, e.getMessage(), e));
        }

        if (is == null) {
            Activator.getDefault().log(new Status(Severity.ERROR, prefUtilsPropertyFileNotFound.text(), null));
        } else {
            // load the properties file
            try {
                preferenceProperties.load(is);
            } catch (Exception e) {
                Activator.getDefault().log(new Status(Severity.ERROR, e.getMessage(), e));
            } finally {
                try {
                    is.close();
                } catch (Exception e) {
                    Activator.getDefault().log(new Status(Severity.ERROR, e.getMessage(), e));
                }
            }
        }

        // file extensions delimiter
        String temp = preferenceProperties.getProperty(FILE_EXT_DELIMITER_PREF_NAME);

        if ((temp != null) && (temp.length() > 0)) {
            FILE_EXT_DELIMITER = temp.charAt(0);
        } else {
            // make sure preference has a value
            FILE_EXT_DELIMITER = ',';
            Activator.getDefault().log(new Status(Severity.ERROR, prefUtilsPropertyNotFound.text(FILE_EXT_DELIMITER_PREF_NAME),
                                                  null));
        }

        // file extension invalid characters
        temp = preferenceProperties.getProperty(FILE_EXT_INVALID_CHARS_PREF_NAME);

        if ((temp != null) && (temp.length() > 0)) {
            FILE_EXT_INVALID_CHARS = temp;
        } else {
            // make sure preference has a value
            FILE_EXT_INVALID_CHARS = "*?<>|/\\:;."; //$NON-NLS-1$
            Activator.getDefault().log(new Status(Severity.ERROR,
                                                  prefUtilsPropertyNotFound.text(FILE_EXT_INVALID_CHARS_PREF_NAME), null));
        }

        // folder names delimiter
        temp = preferenceProperties.getProperty(FOLDER_NAME_DELIMITER_PREF_NAME);

        if ((temp != null) && (temp.length() > 0)) {
            FOLDER_NAME_DELIMITER = temp.charAt(0);
        } else {
            // make sure preference has a value
            FOLDER_NAME_DELIMITER = ',';
            Activator.getDefault().log(new Status(Severity.ERROR,
                                                  prefUtilsPropertyNotFound.text(FOLDER_NAME_DELIMITER_PREF_NAME), null));
        }

        // folder name invalid characters
        temp = preferenceProperties.getProperty(FOLDER_NAME_INVALID_CHARS_PREF_NAME);

        if ((temp != null) && (temp.length() > 0)) {
            FOLDER_NAME_INVALID_CHARS = temp;
        } else {
            // make sure preference has a value
            FOLDER_NAME_INVALID_CHARS = "*?<>|/\\:;"; //$NON-NLS-1$
            Activator.getDefault().log(new Status(Severity.ERROR,
                                                  prefUtilsPropertyNotFound.text(FOLDER_NAME_INVALID_CHARS_PREF_NAME), null));
        }
    }

    // =======================================================================================================================
    // Class Methods
    // =======================================================================================================================

    /**
     * @return the file extensions being filtered out of publishing operations (never null)
     */
    public static String[] getFilteredFileExtensions() {
        return getListPropertyValue(FILTERED_FILE_EXTENSIONS_PREFERENCE, FILE_EXT_DELIMITER, true);
    }

    /**
     * @return the folder names being filtered out of publishing operations (never null)
     */
    public static String[] getFilteredFolderNames() {
        return getListPropertyValue(FILTERED_FOLDER_NAMES_PREFERENCE, FOLDER_NAME_DELIMITER, true);
    }

    /**
     * @param propertyId the property name whose list values are being requested
     * @param delimiter the character separating the items in the property value
     * @param removeDuplicates a flag indicating if duplicate items should be removed
     * @return the property value items (never <code>null</code>)
     */
    public static String[] getListPropertyValue( String propertyId,
                                                 char delimiter,
                                                 boolean removeDuplicates ) {
        CheckArg.isNotNull(propertyId, "propertyId"); //$NON-NLS-1$
        return Utils.getTokens(getPreferenceStore().getString(propertyId), Character.toString(delimiter), removeDuplicates);
    }

    /**
     * @return the plugin preference store
     */
    public static IPreferenceStore getPreferenceStore() {
        return Activator.getDefault().getPreferenceStore();
    }

    /**
     * @param propertyId the property name being set (never <code>null</code>)
     * @param items the items used to create the property value (never <code>null</code>)
     * @param delimiter the character to use to separate the items
     */
    public static void setListPropertyValue( String propertyId,
                                             String[] items,
                                             char delimiter ) {
        CheckArg.isNotNull(propertyId, "propertyId"); //$NON-NLS-1$
        CheckArg.isNotNull(items, "items"); //$NON-NLS-1$
        getPreferenceStore().setValue(propertyId, Utils.combineTokens(items, delimiter));
    }

    // =======================================================================================================================
    // Constructors
    // =======================================================================================================================

    /**
     * Don't allow construction.
     */
    private PrefUtils() {
        // nothing to do
    }
}
