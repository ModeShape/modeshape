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
package org.modeshape.sequencer.teiid;

import org.modeshape.common.i18n.I18n;

/**
 * The internationalized string constants for the <code>org.modeshape.sequencer.teiid*</code> packages.
 */
public final class TeiidI18n {
    public static I18n uuidNotValid;

    public static I18n modelSequencerTaskName;
    public static I18n vdbSequencerTaskName;
    public static I18n errorSequencingModelContent;
    public static I18n errorSequencingVdbContent;

    public static I18n readingEcoreFile;
    public static I18n errorReadingEcoreFile;
    public static I18n errorWritingCndFile;

    public static I18n errorReadingVdbFile;
    public static I18n errorClosingVdbFile;

    public static I18n invalidModelNodeType;
    public static I18n invalidNumberOfPropertyAttributes;
    public static I18n invalidVdbModelNodeType;
    public static I18n missingDataRoleName;
    public static I18n missingEntryPath;
    public static I18n missingImportVdbNameOrVersion;
    public static I18n missingPermissionResourceName;
    public static I18n missingPropertyNameOrValue;
    public static I18n missingTranslatorNameOrType;
    public static I18n missingVdbName;

    public static I18n illegalUnresolvedReference;
    public static I18n namespaceUriNotFoundInRegistry;
    public static I18n invalidVdbVersion;

    public static I18n errorReadingMedMetaclassMappings;

    private TeiidI18n() {
    }

    static {
        try {
            I18n.initialize(TeiidI18n.class);
        } catch (final Exception err) {
            // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
            System.err.println(err);
        }
    }
}
