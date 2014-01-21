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
            String FIXED_VALUE = PREFIX + ":fixedValue";
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
            String ADVANCED = "advanced";
            String ADVANCED2 = "advance"; // some models have a misspelling
            String DISPLAY_NAME = "displayName";
            String DEFAULT_VALUE = "defaultValue";
            String ID = "id";
            String INDEX = "index";
            String FIXED_VALUE = "fixedValue";
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
