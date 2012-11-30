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
package org.modeshape.sequencer.teiid.lexicon;

import static org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon.Namespace.PREFIX;

/**
 * Constants associated with the Model Extenension Definition (MED) namespace used in reading XMI models and writing JCR nodes.
 */
public interface ModelExtensionDefinitionLexicon {

    /**
     * JCR identifiers relating to the MED namespace.
     */
    public interface JcrId {
        String PROPERTY_DEFINITION = PREFIX + ":propertyDefinition";
        String LOCALIZED = PREFIX + ":localized";
        String LOCALE = PREFIX + ":locale";
        String TRANSLATION = PREFIX + ":translation";
        String LOCALIZED_DESCRIPTION = PREFIX + ":localizedDescription";
        String LOCALIZED_NAME = PREFIX + ":localizedName";
        String EXTENDED_METACLASS = PREFIX + ":extendedMetaclass";
        String MODEL_EXTENSION_DEFINITION = PREFIX + ":modelExtensionDefinition";
        String EXTENDED_METAMODEL = PREFIX + ":metamodel";
        String NAMESPACE_PREFIX = PREFIX + ":namespacePrefix";
        String NAMESPACE_URI = PREFIX + ":namespaceUri";
        String VERSION = PREFIX + ":version";
        String DESCRIPTION = PREFIX + ":description";
        String MODEL_TYPES = PREFIX + ":modelTypes";

        /**
         * Constants related to model extension definition property definitions in JCR repositories.
         */
        public interface Property {
            String ADVANCED = PREFIX + ":advance";
            String DISPLAY_NAME = PREFIX + ":displayName";
            String DEFAULT_VALUE = PREFIX + ":defaultValue";
            String INDEX = PREFIX + ":index";
            String MODIFIABLE = PREFIX + ":modifiable";
            String MASKED = PREFIX + ":masked";
            String REQUIRED = PREFIX + ":required";
            String RUNTIME_TYPE = PREFIX + ":runtimeType";
        }
    }

    /**
     * Constants related to the model extension definition namespace in XMI models.
     */
    public interface ModelId {
        String MODEL_EXTENSION_DEFINITION = "modelExtensionDefinition";
        String METAMODEL = "metamodel";
        String NAMESPACE_PREFIX = "namespacePrefix";
        String NAMESPACE_URI = "namespaceUri";
        String VERSION = "version";
        String DESCRIPTION = "description";
        String MODEL_TYPES = "modelTypes";
        String EXTENDED_METACLASS = "extendedMetaclass";
        String PROPERTY_DEFINITION = "propertyDefinition";

        /**
         * Constants related to model extension definition property definitions in XMI models.
         */
        public interface Property {
            String ADVANCED = "advance";
            String DISPLAY_NAME = "displayName";
            String DEFAULT_VALUE = "defaultValue";
            String ID = "id";
            String INDEX = "index";
            String MODIFIABLE = "modifiable";
            String MASKED = "masked";
            String REQUIRED = "required";
            String RUNTIME_TYPE = "runtimeType";
        }
    }

    /**
     * The URI and prefix constants of the MED namespace.
     */
    public interface Namespace {
        String PREFIX = "modelExtensionDefinition";
        String URI = "http://www.jboss.org/teiiddesigner/ext/2012";
    }

    public class Utils {

        /**
         * @param key the MED key whose JCR name is being constructed (cannot be <code>null</code> or empty)
         * @return the MED JCR name (never <code>null</code> or empty
         */
        public static String constructJcrName( final String key ) {
            return (ModelExtensionDefinitionLexicon.Namespace.PREFIX + ':' + key);
        }

        /**
         * @param key the key being checked (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the key is a model annotation tag key for a MED extended metaclass
         */
        public static boolean isModelMedMetaclassTagKey( final String key ) {
            return key.startsWith(ModelId.EXTENDED_METACLASS + ':');
        }

        /**
         * @param key the key being checked (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the key is a model annotation tag key for the MED model types
         */
        public static boolean isModelMedModelTypesTagKey( final String key ) {
            return ModelId.MODEL_TYPES.equals(key);
        }

        /**
         * @param key the key being checked (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the key is a model annotation tag key for a MED property definition's descriptions
         */
        public static boolean isModelMedPropertyDefinitionDescriptionTagKey( final String key ) {
            return ModelId.DESCRIPTION.equals(key);
        }

        /**
         * @param key the key being checked (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the key is a model annotation tag key for a MED property definition's display names
         */
        public static boolean isModelMedPropertyDefinitionDisplayNameTagKey( final String key ) {
            return ModelId.Property.DISPLAY_NAME.equals(key);
        }

        /**
         * @param key the key being checked (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the key is a model annotation tag key for a MED property definition
         */
        public static boolean isModelMedPropertyDefinitionTagKey( final String key ) {
            return key.startsWith(ModelId.PROPERTY_DEFINITION + ':');
        }

        /**
         * @param key the key being checked (cannot be <code>null</code> or empty)
         * @return <code>true</code> if the key is an model annotation tag key for the start of a MED
         */
        public static boolean isModelMedTagKey( final String key ) {
            return key.startsWith(ModelId.MODEL_EXTENSION_DEFINITION + ':');
        }
    }
}
