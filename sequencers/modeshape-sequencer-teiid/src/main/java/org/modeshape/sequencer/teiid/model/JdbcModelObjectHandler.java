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
package org.modeshape.sequencer.teiid.model;

import static org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.Namespace.URI;
import javax.jcr.Node;
import org.modeshape.common.util.CheckArg;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon.ModelId;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.modeshape.sequencer.teiid.xmi.XmiPart;

/**
 * The model object handler for the JDBC namespace.
 */
public final class JdbcModelObjectHandler extends ModelObjectHandler {

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#getQName(org.modeshape.sequencer.teiid.xmi.XmiPart)
     */
    @Override
    protected String getQName( final XmiPart xmiPart ) {
        // transform model namespace prefix into the the JCR namespace prefix
        return (JcrId.NS_PREFIX + ':' + xmiPart.getName());
    }

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node parentNode ) throws Exception {
        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "outputNode");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "JDBC URI");

        LOGGER.debug("==== JdbcModelObjectHandler:process:element={0}", element.getName());
        final String type = element.getName();

        if (ModelId.SOURCE.equals(type)) {
            // - jdbcs:name (string)
            final Node sourceNode = addNode(parentNode, element, URI, JcrId.SOURCE);
            processSource(element, sourceNode);

            // process import settings
            for (final XmiElement childElement : element.getChildren()) {
                if (ModelId.IMPORT_SETTINGS.equals(childElement.getName())) {
                    process(childElement, parentNode);
                    break; // only one
                }

                LOGGER.debug("**** JDBC Source child element type of '{0}' was not processed", childElement.getName());
            }
        } else if (ModelId.IMPORT_SETTINGS.equals(type)) {
            final Node importSettingNode = addNode(parentNode, element, URI, JcrId.IMPORTED);
            processImportSetting(element, importSettingNode);

            // process children
            for (final XmiElement childElement : element.getChildren()) {
                final String childType = childElement.getName();

                if (ModelId.EXCLUDED_OBJECT_PATHS.equals(childType)) {
                    // - jdbcs:excludedObjectPaths (string) multiple
                    addPropertyValue(importSettingNode, JcrId.EXCLUDED_OBJECT_PATHS, childElement.getValue());
                } else if (ModelId.INCLUDED_CATALOG_PATHS.equals(childType)) {
                    // - jdbcs:includedCatalogPaths (string) multiple
                    addPropertyValue(importSettingNode, JcrId.INCLUDED_CATALOG_PATHS, childElement.getValue());
                } else if (ModelId.INCLUDED_SCHEMA_PATHS.equals(childType)) {
                    // - jdbcs:includedSchemaPaths (string) multiple
                    addPropertyValue(importSettingNode, JcrId.INCLUDED_SCHEMA_PATHS, childElement.getValue());
                } else if (ModelId.INCLUDED_TABLE_TYPES.equals(childType)) {
                    // - jdbcs:includedTableTypes (string) multiple
                    addPropertyValue(importSettingNode, JcrId.INCLUDED_TABLE_TYPES, childElement.getValue());
                } else {
                    LOGGER.debug("**** JDBC Import Settings child element type of '{0}' was not processed", childElement.getName());
                }
            }
        } else {
            LOGGER.debug("**** JDBC type of '{0}' was not processed", type);
        }
    }

    private void processImportSetting( final XmiElement importSettingElement,
                                       final Node importSettingNode ) throws Exception {
        // - jdbcs:createCatalogsInModel (boolean) = 'true'
        setBooleanProperty(importSettingNode,
                           JcrId.CREATE_CATALOGS_IN_MODEL,
                           importSettingElement.getAttributeValue(ModelId.CREATE_CATALOGS_IN_MODEL, URI));

        // - jdbcs:createSchemasInModel (boolean) = 'true'
        setBooleanProperty(importSettingNode,
                           JcrId.CREATE_SCHEMAS_IN_MODEL,
                           importSettingElement.getAttributeValue(ModelId.CREATE_SCHEMAS_IN_MODEL, URI));

        // - jdbcs:convertCaseInModel (string) < 'NONE', 'TO_UPPERCASE', 'TO_LOWERCASE'
        setProperty(importSettingNode,
                    JcrId.CONVERT_CASE_IN_MODEL,
                    importSettingElement.getAttributeValue(ModelId.CONVERT_CASE_IN_MODEL, URI));

        // - jdbcs:generateSourceNamesInModel (string) = 'UNQUALIFIED' < 'NONE', 'UNQUALIFIED', 'FULLY_QUALIFIED'
        setProperty(importSettingNode,
                    JcrId.GENERATE_SOURCE_NAMES_IN_MODEL,
                    importSettingElement.getAttributeValue(ModelId.GENERATE_SOURCE_NAMES_IN_MODEL, URI));

        // - jdbcs:includeForeignKeys (boolean) = 'true'
        setBooleanProperty(importSettingNode,
                           JcrId.INCLUDE_FOREIGN_KEYS,
                           importSettingElement.getAttributeValue(ModelId.INCLUDE_FOREIGN_KEYS, URI));

        // - jdbcs:includeIndexes (boolean) = 'true'
        setBooleanProperty(importSettingNode,
                           JcrId.INCLUDE_INDEXES,
                           importSettingElement.getAttributeValue(ModelId.INCLUDE_INDEXES, URI));

        // - jdbcs:includeProcedures (boolean) = 'false'
        setBooleanProperty(importSettingNode,
                           JcrId.INCLUDE_PROCEDURES,
                           importSettingElement.getAttributeValue(ModelId.INCLUDE_PROCEDURES, URI));

        // - jdbcs:includeApproximateIndexes (boolean) = 'true'
        setBooleanProperty(importSettingNode,
                           JcrId.INCLUDE_APPROXIMATE_INDEXES,
                           importSettingElement.getAttributeValue(ModelId.INCLUDE_APPROXIMATE_INDEXES, URI));

        // - jdbcs:includeUniqueIndexes (boolean) = 'false'
        setBooleanProperty(importSettingNode,
                           JcrId.INCLUDE_UNIQUE_INDEXES,
                           importSettingElement.getAttributeValue(ModelId.INCLUDE_UNIQUE_INDEXES, URI));
    }

    private void processSource( final XmiElement sourceElement,
                                final Node sourceNode ) throws Exception {
        // - jdbcs:driverName (string)
        setProperty(sourceNode, JcrId.DRIVER_NAME, sourceElement.getAttributeValue(ModelId.DRIVER_NAME, URI));

        // - jdbcs:driverClass (string)
        setProperty(sourceNode, JcrId.DRIVER_CLASS, sourceElement.getAttributeValue(ModelId.DRIVER_CLASS, URI));

        // - jdbcs:username (string)
        setProperty(sourceNode, JcrId.USER_NAME, sourceElement.getAttributeValue(ModelId.USER_NAME, URI));

        // - jdbcs:url (string)
        setProperty(sourceNode, JcrId.URL, sourceElement.getAttributeValue(ModelId.URL, URI));
    }
}
