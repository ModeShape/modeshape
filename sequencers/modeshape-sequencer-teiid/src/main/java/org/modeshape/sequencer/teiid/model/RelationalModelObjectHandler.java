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
package org.modeshape.sequencer.teiid.model;

import static org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.Namespace.URI;
import java.util.Arrays;
import java.util.Collection;
import javax.jcr.Node;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.ModelId;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.lexicon.XsiLexicon;
import org.modeshape.sequencer.teiid.model.ReferenceResolver.UnresolvedReference;
import org.modeshape.sequencer.teiid.xmi.XmiElement;

/**
 * The model object handler for the {@link org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.Namespace#URI relational}
 * namespace.
 */
public final class RelationalModelObjectHandler extends ModelObjectHandler {

    /**
     * @see org.modeshape.sequencer.teiid.model.ModelObjectHandler#process(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      javax.jcr.Node)
     */
    @Override
    protected void process( final XmiElement element,
                            final Node parentNode ) throws Exception {
        // Note: As of Sep 2011 Schema and Catalog no long can be created using Designer

        CheckArg.isNotNull(element, "element");
        CheckArg.isNotNull(parentNode, "node");
        CheckArg.isEquals(element.getNamespaceUri(), "namespace URI", URI, "relational URI");

        if (DEBUG) {
            debug("==== RelationalModelObjectHandler:process:element=" + element.getName());
        }

        final String type = element.getName();

        if (ModelId.BASE_TABLE.equals(type)) {
            final Node tableNode = addNode(parentNode, element, URI, JcrId.BASE_TABLE);
            processTable(element, tableNode);
        } else if (ModelId.ACCESS_PATTERNS.equals(type) || ModelId.ACCESS_PATTERN.equals(type)) {
            final Node accessPatternNode = addNode(parentNode, element, URI, JcrId.ACCESS_PATTERN);
            processAccessPattern(element, accessPatternNode);
        } else if (ModelId.CATALOG.equals(type)) {
            final Node catalogNode = addNode(parentNode, element, URI, JcrId.CATALOG);
            processCatalog(element, catalogNode);
        } else if (ModelId.INDEXES.equals(type) || ModelId.INDEX.equals(type)) {
            final Node indexNode = addNode(parentNode, element, URI, JcrId.INDEX);
            processIndex(element, indexNode);
        } else if (ModelId.COLUMNS.equals(type)) {
            final Node columnNode = addNode(parentNode, element, URI, JcrId.COLUMN);
            processColumn(element, columnNode);
        } else if (ModelId.FOREIGN_KEYS.equals(type)) {
            final Node foreignKeyNode = addNode(parentNode, element, URI, JcrId.FOREIGN_KEY);
            processForeignKey(element, foreignKeyNode);
        } else if (ModelId.PRIMARY_KEY.equals(type)) {
            final Node primaryKeyNode = addNode(parentNode, element, URI, JcrId.PRIMARY_KEY);
            processUniqueKey(element, primaryKeyNode);
        } else if (ModelId.PROCEDURE_PARAMETER.equals(type)) {
            final Node procedureParameterNode = addNode(parentNode, element, URI, JcrId.PROCEDURE_PARAMETER);
            processProcedureParameter(element, procedureParameterNode);
        } else if (ModelId.PROCEDURE_RESULT.equals(type)) {
            final Node procedureResultNode = addNode(parentNode, element, URI, JcrId.PROCEDURE_RESULT);
            processColumnSet(element, procedureResultNode);
        } else if (ModelId.PROCEDURES.equals(type) || ModelId.PROCEDURE.equals(type)) {
            final Node procedureNode = addNode(parentNode, element, URI, JcrId.PROCEDURE);
            processProcedure(element, procedureNode);
        } else if (ModelId.SCHEMAS.equals(type) || ModelId.SCHEMA.equals(type)) {
            final Node schemaNode = addNode(parentNode, element, URI, JcrId.SCHEMA);
            processSchema(element, schemaNode);
        } else if (ModelId.TABLES.equals(type)) {
            Node tableNode = null;
            final String xsiType = element.getAttributeValue(XsiLexicon.ModelId.TYPE, XsiLexicon.Namespace.URI);
            final String[] parts = xsiType.split(":");

            if ((parts.length == 2) && ModelId.BASE_TABLE.equals(parts[1])) {
                tableNode = addNode(parentNode, element, URI, JcrId.BASE_TABLE);
                processTable(element, tableNode);
            } else if ((parts.length == 2) && ModelId.TABLES_VIEW.equals(parts[1])) {
                tableNode = addNode(parentNode, element, URI, JcrId.VIEW);
                processTable(element, tableNode);
            } else {
                if (DEBUG) {
                    debug("**** relational '" + ModelId.TABLES + "' type + of '" + xsiType + "' was not processed");
                }
            }
        } else if (ModelId.TYPE.equals(type)) {
            processType(element, parentNode);
        } else if (ModelId.UNIQUE_CONSTRAINT.equals(type)) {
            final Node uniqueConstraintNode = addNode(parentNode, element, URI, JcrId.UNIQUE_CONSTRAINT);
            processUniqueKey(element, uniqueConstraintNode);
        } else if (ModelId.VIEW.equals(type)) {
            final Node viewNode = addNode(parentNode, element, URI, JcrId.VIEW);
            processTable(element, viewNode);
        } else {
            if (DEBUG) {
                debug("**** relational type of " + type + " was not processed");
            }
        }
    }

    private void processAccessPattern( final XmiElement accessPatternElement,
                                       final Node accessPatternNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(accessPatternElement, accessPatternNode);

        // - relational:columns (UNDEFINED) multiple
        processColumnsAttribute(accessPatternElement, accessPatternNode);
    }

    private void processAccessPatternsAttribute( final XmiElement columnElement,
                                                 final Node columnNode ) throws Exception {
        final String accessPatterns = columnElement.getAttributeValue(ModelId.COLUMNS, URI);

        if (!StringUtil.isBlank(accessPatterns)) {
            final ReferenceResolver resolver = getResolver();
            final ValueFactory valueFactory = columnNode.getSession().getValueFactory();

            for (final String accessPatternRef : accessPatterns.split("\\s")) {
                final String accessPatternUuid = resolver.resolveInternalReference(accessPatternRef);
                final Node accessPatternNode = resolver.getNode(accessPatternUuid);
                UnresolvedReference unresolved = null;

                // - relational:accessPatterns (weak reference) multiple
                if (accessPatternNode == null) {
                    unresolved = resolver.addUnresolvedReference(accessPatternUuid);
                    unresolved.addReferencerReference(columnElement.getUuid(), JcrId.ACCESS_PATTERNS);
                } else {
                    if (!accessPatternNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        accessPatternNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    final Value weakReference = valueFactory.createValue(accessPatternNode, true);
                    addPropertyValue(columnNode, JcrId.ACCESS_PATTERNS, weakReference);
                }

                // - relational:accessPatternHrefs (string) multiple
                addPropertyValue(columnNode, JcrId.ACCESS_PATTERN_HREFS, accessPatternRef);

                // - relational:accessPatternXmiUuids (string) multiple
                addPropertyValue(columnNode, JcrId.ACCESS_PATTERN_XMI_UUIDS, accessPatternUuid);

                // - relational:accessPatternNames (string) multiple
                if (accessPatternNode != null) {
                    addPropertyValue(columnNode, JcrId.ACCESS_PATTERN_NAMES, accessPatternNode.getName());
                } else if (unresolved != null) {
                    unresolved.addResolvedName(columnElement.getUuid(), JcrId.ACCESS_PATTERN_NAMES);
                } else {
                    assert false;
                }
            }
        }
    }

    private void processCatalog( final XmiElement catalogElement,
                                 final Node catalogNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(catalogElement, catalogNode);

        // no properties to process so just process children
        processChildren(catalogElement, catalogNode);
    }

    private void processChildren( final XmiElement element,
                                  final Node parentNode,
                                  final String... childTypes ) throws Exception {
        Collection<String> types = null;

        if (childTypes != null) {
            types = Arrays.asList(childTypes);
        }

        for (final XmiElement kid : element.getChildren()) {
            if ((types == null) || types.isEmpty()) {
                process(kid, parentNode);
            } else if (types.contains(kid.getName())) {
                process(kid, parentNode);
            }
        }
    }

    private void processColumn( final XmiElement columnElement,
                                final Node columnNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(columnElement, columnNode);

        // - relational:nativeType (string)
        setProperty(columnNode, JcrId.NATIVE_TYPE, columnElement.getAttributeValue(ModelId.NATIVE_TYPE, URI));

        // - relational:length (long)
        setProperty(columnNode, JcrId.LENGTH, columnElement.getAttributeValue(ModelId.LENGTH, URI));

        // - relational:fixedLength (boolean)
        setProperty(columnNode, JcrId.FIXED_LENGTH, columnElement.getAttributeValue(ModelId.FIXED_LENGTH, URI));

        // - relational:precision (long)
        setProperty(columnNode, JcrId.PRECISION, columnElement.getAttributeValue(ModelId.PRECISION, URI));

        // - relational:scale (long)
        setProperty(columnNode, JcrId.SCALE, columnElement.getAttributeValue(ModelId.SCALE, URI));

        // - relational:nullable (string) = 'NULLABLE' < 'NO_NULLS', 'NULLABLE', 'NULLABLE_UNKNOWN'
        setProperty(columnNode, JcrId.NULLABLE, columnElement.getAttributeValue(ModelId.NULLABLE, URI));

        // - relational:autoIncremented (boolean) = 'false'
        setProperty(columnNode, JcrId.AUTO_INCREMENTED, columnElement.getAttributeValue(ModelId.AUTO_INCREMENTED, URI));

        // - relational:defaultValue (string)
        setProperty(columnNode, JcrId.DEFAULT_VALUE, columnElement.getAttributeValue(ModelId.DEFAULT_VALUE, URI));

        // - relational:minimumValue (string)
        setProperty(columnNode, JcrId.MIN_VALUE, columnElement.getAttributeValue(ModelId.MIN_VALUE, URI));

        // - relational:maximumValue (string)
        setProperty(columnNode, JcrId.MAX_VALUE, columnElement.getAttributeValue(ModelId.MAX_VALUE, URI));

        // - relational:format (string)
        setProperty(columnNode, JcrId.FORMAT, columnElement.getAttributeValue(ModelId.FORMAT, URI));

        // - relational:characterSetName (string)
        setProperty(columnNode, JcrId.CHARACTER_SET_NAME, columnElement.getAttributeValue(ModelId.CHARACTER_SET_NAME, URI));

        // - relational:collationName (string)
        setProperty(columnNode, JcrId.COLLATION_NAME, columnElement.getAttributeValue(ModelId.COLLATION_NAME, URI));

        // - relational:selectable (boolean) = 'true'
        setProperty(columnNode, JcrId.SELECTABLE, columnElement.getAttributeValue(ModelId.SELECTABLE, URI));

        // - relational:updateable (boolean) = 'true'
        setProperty(columnNode, JcrId.UPDATEABLE, columnElement.getAttributeValue(ModelId.UPDATEABLE, URI));

        // - relational:caseSensitive (boolean) = 'true'
        setProperty(columnNode, JcrId.CASE_SENSITIVE, columnElement.getAttributeValue(ModelId.CASE_SENSITIVE, URI));

        // - relational:searchability (string) = 'SEARCHABLE' < 'SEARCHABLE', 'ALL_EXCEPT_LIKE', 'LIKE_ONLY', 'UNSEARCHABLE'
        setProperty(columnNode, JcrId.SEARCHABILITY, columnElement.getAttributeValue(ModelId.SEARCHABILITY, URI));

        // - relational:currency (boolean) = 'false'
        setProperty(columnNode, JcrId.CURRENCY, columnElement.getAttributeValue(ModelId.CURRENCY, URI));

        // - relational:radix (long) = '10'
        setProperty(columnNode, JcrId.RADIX, columnElement.getAttributeValue(ModelId.RADIX, URI));

        // - relational:signed (boolean) = 'true'
        setProperty(columnNode, JcrId.SIGNED, columnElement.getAttributeValue(ModelId.SIGNED, URI));

        // - relational:distinctValueCount (long) = '-1'
        setProperty(columnNode, JcrId.DISTINCT_VALUE_COUNT, columnElement.getAttributeValue(ModelId.DISTINCT_VALUE_COUNT, URI));

        // - relational:nullValueCount (long) = '-1'
        setProperty(columnNode, JcrId.NULL_VALUE_COUNT, columnElement.getAttributeValue(ModelId.NULL_VALUE_COUNT, URI));

        // - relational:uniqueKeys (weakreference) multiple
        // - relational:uniqueKeyHrefs (string) multiple
        // - relational:uniqueKeyXmiUuids (string) multiple
        // - relational:uniqueKeyNames (string) multiple
        processUniqueKeysAttribute(columnElement, columnNode);

        // - relational:indexes (weakreference) multiple
        // - relational:indexHrefs (string) multiple
        // - relational:indexXmiUuids (string) multiple
        // - relational:indexNames (string) multiple
        processIndexesAttribute(columnElement, columnNode);

        // - relational:foreignKeys (weakreference) multiple
        // - relational:foreignKeyHrefs (string) multiple
        // - relational:foreignKeyXmiUuids (string) multiple
        // - relational:foreignKeyNames (string) multiple
        processForeignKeysAttribute(columnElement, columnNode);

        // - relational:accessPatterns (weakreference) multiple
        // - relational:accessPatternHrefs (string) multiple
        // - relational:accessPatternXmiUuids (string) multiple
        // - relational:accessPatternNames (string) multiple
        processAccessPatternsAttribute(columnElement, columnNode);

        // - relational:type (weakreference)
        // - relational:typeXmiUuid (string)
        // - relational:typeName (string)
        processChildren(columnElement, columnNode, ModelId.TYPE);
    }

    private void processColumnsAttribute( final XmiElement element,
                                          final Node node ) throws Exception {
        final String columns = element.getAttributeValue(ModelId.COLUMNS, URI);

        if (!StringUtil.isBlank(columns)) {
            final ReferenceResolver resolver = getResolver();
            final ValueFactory valueFactory = node.getSession().getValueFactory();

            for (final String columnRef : columns.split("\\s")) {
                final String columnUuid = resolver.resolveInternalReference(columnRef);
                final Node columnNode = resolver.getNode(columnUuid);
                UnresolvedReference unresolved = null;

                // - relational:columns (weak reference) multiple
                if (columnNode == null) {
                    unresolved = resolver.addUnresolvedReference(columnUuid);
                    unresolved.addReferencerReference(element.getUuid(), JcrId.COLUMNS);
                } else {
                    if (!columnNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        columnNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    final Value weakReference = valueFactory.createValue(columnNode, true);
                    addPropertyValue(node, JcrId.COLUMNS, weakReference);
                }

                // - relational:columnXmiUuids (string) multiple
                addPropertyValue(node, JcrId.COLUMN_XMI_UUIDS, columnUuid);

                // - relational:columnNames (string) multiple
                if (columnNode != null) {
                    addPropertyValue(node, JcrId.COLUMN_NAMES, columnNode.getName());
                } else if (unresolved != null) {
                    unresolved.addResolvedName(element.getUuid(), JcrId.COLUMN_NAMES);
                } else {
                    assert false;
                }
            }
        }
    }

    private void processColumnSet( final XmiElement columnSetElement,
                                   final Node columnSetNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(columnSetElement, columnSetNode);

        // no properties

        // + * (relational:column) = relational:column copy
        processChildren(columnSetElement, columnSetNode, ModelId.COLUMNS);
    }

    private void processForeignKey( final XmiElement foreignKeyElement,
                                    final Node foreignKeyNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(foreignKeyElement, foreignKeyNode);

        // - relational:foreignKeyMultiplicity (string)
        setProperty(foreignKeyNode,
                    JcrId.FOREIGN_KEY_MULTIPLICITY,
                    foreignKeyElement.getAttributeValue(ModelId.FOREIGN_KEY_MULTIPLICITY, URI));

        // - relational:primaryKeyMultiplicity (string)
        setProperty(foreignKeyNode,
                    JcrId.PRIMARY_KEY_MULTIPLICITY,
                    foreignKeyElement.getAttributeValue(ModelId.PRIMARY_KEY_MULTIPLICITY, URI));

        // - relational:columns (weakreference) multiple
        // - relational:columnXmiUuids (string) multiple
        // - relational:columnNames (string) multiple
        processColumnsAttribute(foreignKeyElement, foreignKeyNode);

        // - relational:uniqueKeys (weakreference) multiple
        // - relational:uniqueKeyHrefs (string) multiple
        // - relational:uniqueKeyXmiUuids (string) multiple
        // - relational:uniqueKeyNames (string) multiple
        processUniqueKeysAttribute(foreignKeyElement, foreignKeyNode);
    }

    private void processForeignKeysAttribute( final XmiElement element,
                                              final Node node ) throws Exception {
        final String foreignKeys = element.getAttributeValue(ModelId.FOREIGN_KEYS, URI);

        if (!StringUtil.isBlank(foreignKeys)) {
            final ReferenceResolver resolver = getResolver();
            final ValueFactory valueFactory = node.getSession().getValueFactory();

            for (final String foreignKeyRef : foreignKeys.split("\\s")) {
                final String foreignKeyUuid = resolver.resolveInternalReference(foreignKeyRef);
                final Node foreignKeyNode = resolver.getNode(foreignKeyUuid);
                UnresolvedReference unresolved = null;

                // - relational:foreignKeys (weakreference) multiple
                if (foreignKeyNode == null) {
                    unresolved = resolver.addUnresolvedReference(foreignKeyUuid);
                    unresolved.addReferencerReference(element.getUuid(), JcrId.FOREIGN_KEYS);
                } else {
                    if (!foreignKeyNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        foreignKeyNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    final Value weakReference = valueFactory.createValue(foreignKeyNode, true);
                    addPropertyValue(node, JcrId.FOREIGN_KEYS, weakReference);
                }

                // - relational:foreignKeyXmiUuids (string) multiple
                addPropertyValue(node, JcrId.FOREIGN_KEY_XMI_UUIDS, foreignKeyUuid);

                // - relational:foreignKeyHrefs (string) multiple
                addPropertyValue(node, JcrId.FOREIGN_KEY_HREFS, foreignKeyRef);

                // - relational:foreignKeyNames (string) multiple
                if (foreignKeyNode != null) {
                    addPropertyValue(node, JcrId.UNIQUE_KEY_NAMES, foreignKeyNode.getName());
                } else if (unresolved != null) {
                    unresolved.addResolvedName(element.getUuid(), JcrId.FOREIGN_KEY_NAMES);
                } else {
                    assert false;
                }
            }
        }
    }

    private void processIndex( final XmiElement indexElement,
                               final Node indexNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(indexElement, indexNode);

        // - relational:filterCondition (string)
        setProperty(indexNode, JcrId.FILTER_CONDITION, indexElement.getAttributeValue(ModelId.FILTER_CONDITION, URI));

        // - relational:nullable (boolean) = 'true'
        setProperty(indexNode, JcrId.NULLABLE, indexElement.getAttributeValue(ModelId.NULLABLE, URI));

        // - relational:autoUpdate (boolean)
        setProperty(indexNode, JcrId.AUTO_UPDATE, indexElement.getAttributeValue(ModelId.AUTO_UPDATE, URI));

        // - relational:unique (boolean)
        setProperty(indexNode, JcrId.UNIQUE, indexElement.getAttributeValue(ModelId.UNIQUE, URI));

        // - relational:columns (weakreference) multiple
        // - relational:columnXmiUuids (string) multiple
        // - relational:columnNames (string) multiple
        processColumnsAttribute(indexElement, indexNode);
    }

    private void processIndexesAttribute( final XmiElement columnElement,
                                          final Node columnNode ) throws Exception {
        final String indexes = columnElement.getAttributeValue(ModelId.INDEXES, URI);

        if (!StringUtil.isBlank(indexes)) {
            final ReferenceResolver resolver = getResolver();
            final ValueFactory valueFactory = columnNode.getSession().getValueFactory();

            for (final String indexRef : indexes.split("\\s")) {
                final String indexUuid = resolver.resolveInternalReference(indexRef);
                final Node indexNode = resolver.getNode(indexUuid);
                UnresolvedReference unresolved = null;

                // - relational:indexes (weakreference) multiple
                if (indexNode == null) {
                    unresolved = resolver.addUnresolvedReference(indexUuid);
                    unresolved.addReferencerReference(columnElement.getUuid(), JcrId.INDEXES);
                } else {
                    if (!indexNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        indexNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    final Value weakReference = valueFactory.createValue(indexNode, true);
                    addPropertyValue(columnNode, JcrId.INDEXES, weakReference);
                }

                // - relational:indexXmiUuids (string) multiple
                addPropertyValue(columnNode, JcrId.INDEX_XMI_UUIDS, indexUuid);

                // - relational:indexHrefs (string) multiple
                addPropertyValue(columnNode, JcrId.INDEX_HREFS, indexRef);

                // - relational:indexNames (string) multiple
                if (indexNode != null) {
                    addPropertyValue(columnNode, JcrId.INDEX_NAMES, indexNode.getName());
                } else if (unresolved != null) {
                    unresolved.addResolvedName(columnElement.getUuid(), JcrId.INDEX_NAMES);
                } else {
                    assert false;
                }
            }
        }
    }

    private void processProcedure( final XmiElement procedureElement,
                                   final Node procedureNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(procedureElement, procedureNode);

        // - relational:function (boolean)
        setProperty(procedureNode, JcrId.FUNCTION, procedureElement.getAttributeValue(ModelId.FUNCTION, URI));

        // - relational:updateCount (string) < 'AUTO', 'ZERO', 'ONE', 'MULTIPLE'
        setProperty(procedureNode, JcrId.UPDATE_COUNT, procedureElement.getAttributeValue(ModelId.UPDATE_COUNT, URI));

        // + * (relational:procedureParameter) = relational:procedureParameter copy sns
        // + * (relational:procedureResult) = relational:procedureResult copy
        processChildren(procedureElement, procedureNode, ModelId.PROCEDURE_PARAMETER, ModelId.PROCEDURE_RESULT);
    }

    private void processProcedureParameter( final XmiElement procedureParameterElement,
                                            final Node procedureParameterNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(procedureParameterElement, procedureParameterNode);

        // - relational:direction (string) < 'IN', 'OUT', 'INOUT', 'RETURN', 'UNKNOWN'
        setProperty(procedureParameterNode, JcrId.DIRECTION, procedureParameterElement.getAttributeValue(ModelId.DIRECTION, URI));

        // - relational:defaultValue (string)
        setProperty(procedureParameterNode,
                    JcrId.DEFAULT_VALUE,
                    procedureParameterElement.getAttributeValue(ModelId.DEFAULT_VALUE, URI));

        // - relational:nativeType (string)
        setProperty(procedureParameterNode,
                    JcrId.NATIVE_TYPE,
                    procedureParameterElement.getAttributeValue(ModelId.NATIVE_TYPE, URI));

        // - relational:length (long)
        setProperty(procedureParameterNode, JcrId.LENGTH, procedureParameterElement.getAttributeValue(ModelId.LENGTH, URI));

        // - relational:precision (long)
        setProperty(procedureParameterNode, JcrId.PRECISION, procedureParameterElement.getAttributeValue(ModelId.PRECISION, URI));

        // - relational:scale (long)
        setProperty(procedureParameterNode, JcrId.SCALE, procedureParameterElement.getAttributeValue(ModelId.SCALE, URI));

        // - relational:nullable (string) = 'NULLABLE' < 'NO_NULLS', 'NULLABLE', 'NULLABLE_UNKNOWN'
        setProperty(procedureParameterNode, JcrId.NULLABLE, procedureParameterElement.getAttributeValue(ModelId.NULLABLE, URI));

        // - relational:radix (long) = '10'
        setProperty(procedureParameterNode, JcrId.RADIX, procedureParameterElement.getAttributeValue(ModelId.RADIX, URI));

        // - relational:type (weakreference)
        // - relational:typeXmiUuid (string)
        // - relational:typeName (string)
        processChildren(procedureParameterElement, procedureParameterNode, ModelId.TYPE);
    }

    private void processRelationalEntity( final XmiElement element,
                                          final Node node ) throws Exception {
        // set inherited properties
        processXmiReferenceable(element, node);

        // - relational:nameInSource (string)
        setProperty(node, JcrId.NAME_IN_SOURCE, element.getAttributeValue(ModelId.NAME_IN_SOURCE, URI));
    }

    private void processSchema( final XmiElement schemaElement,
                                final Node schemaNode ) throws Exception {
        // set inherited properties
        processRelationalEntity(schemaElement, schemaNode);

        // no properties to process so just process children
        processChildren(schemaElement, schemaNode);
    }

    private void processTable( final XmiElement tableElement,
                               final Node tableNode ) throws Exception {
        // set inherited properties
        processColumnSet(tableElement, tableNode);

        // - relational:system (boolean) = 'false'
        setProperty(tableNode, JcrId.SYSTEM, tableElement.getAttributeValue(ModelId.SYSTEM, URI));

        // - relational:cardinality (long)
        setProperty(tableNode, JcrId.CARDINALITY, tableElement.getAttributeValue(ModelId.CARDINALITY, URI));

        // - relational:supportsUpdate (boolean) = 'true'
        setProperty(tableNode, JcrId.SUPPORTS_UPDATE, tableElement.getAttributeValue(ModelId.SUPPORTS_UPDATE, URI));

        // - relational:materialized (boolean) = 'false'
        setProperty(tableNode, JcrId.MATERIALIZED, tableElement.getAttributeValue(ModelId.MATERIALIZED, URI));

        // + * (relational:primaryKey) = relational:primaryKey copy
        // + * (relational:foreignKey) = relational:foreignKey copy
        // + * (relational:accessPattern) = relational:accessPattern copy sns
        processChildren(tableElement,
                        tableNode,
                        ModelId.PRIMARY_KEY,
                        ModelId.FOREIGN_KEYS,
                        ModelId.ACCESS_PATTERN,
                        ModelId.ACCESS_PATTERNS);
    }

    private void processType( final XmiElement typeElement,
                              final Node parentNode ) throws Exception {
        final String href = typeElement.getAttributeValue(ModelId.HREF, URI);

        if (!StringUtil.isBlank(href)) {
            // - relational:typeHref (string)
            setProperty(parentNode, JcrId.TYPE_HREF, href);

            final String typeName = ReferenceResolver.STANDARD_DATA_TYPE_URLS_TO_NAMES.get(href);

            if (!StringUtil.isBlank(typeName)) {
                // - relational:typeName (string)
                setProperty(parentNode, JcrId.TYPE_NAME, typeName);

                // TODO typeElement does not have a XMI UUID so there is not a way to look up node (if it even exists)
                // - relational:type (weakreference)
                final String uuid = ReferenceResolver.STANDARD_DATA_TYPE_UUIDS_BY_NAMES.get(typeName);
//                final Node typeNode = getResolver().getNode(uuid);
//
//                if (typeNode == null) {
//                    UnresolvedReference unresolved = getResolver().addUnresolvedReference(uuid);
//                    unresolved.addReferencerReference(typeElement.getUuid(), JcrId.TYPE);
//                } else {
//                    parentNode.setProperty(JcrId.TYPE, parentNode.getSession().getValueFactory().createValue(typeNode, true));
//                }

                // - relational:typeXmiUuid (string)
                setProperty(parentNode, JcrId.TYPE_XMI_UUID, uuid);
            }
        }
    }

    private void processUniqueKey( final XmiElement uniqueKeyElement,
                                   final Node uniqueKeyNode ) throws Exception {
        assert (uniqueKeyElement != null);
        assert (uniqueKeyNode != null);

        getResolver();
        uniqueKeyNode.getSession().getValueFactory();

        // set inherited properties
        processRelationalEntity(uniqueKeyElement, uniqueKeyNode);

        // - relational:columns (weakreference) multiple
        // - relational:columnXmiUuids (string) multiple
        // - relational:columnNames (string) multiple
        processColumnsAttribute(uniqueKeyElement, uniqueKeyNode);

        // - relational:foreignKeys (weakreference) multiple
        // - relational:foreignKeyHrefs (string) multiple
        // - relational:foreignKeyXmiUuids (string) multiple
        // - relational:foreignKeyNames (string) multiple
        processForeignKeysAttribute(uniqueKeyElement, uniqueKeyNode);
    }

    private void processUniqueKeysAttribute( final XmiElement element,
                                             final Node node ) throws Exception {
        final String uniqueKeys = element.getAttributeValue(ModelId.UNIQUE_KEYS, URI);

        if (!StringUtil.isBlank(uniqueKeys)) {
            final ReferenceResolver resolver = getResolver();
            final ValueFactory valueFactory = node.getSession().getValueFactory();

            for (final String uniqueKeyRef : uniqueKeys.split("\\s")) {
                final String uniqueKeyUuid = resolver.resolveInternalReference(uniqueKeyRef);
                final Node uniqueKeyNode = resolver.getNode(uniqueKeyUuid);
                UnresolvedReference unresolved = null;

                // - relational:uniqueKeys (weakreference) multiple
                if (uniqueKeyNode == null) {
                    unresolved = resolver.addUnresolvedReference(uniqueKeyUuid);
                    unresolved.addReferencerReference(element.getUuid(), JcrId.UNIQUE_KEYS);
                } else {
                    if (!uniqueKeyNode.isNodeType(JcrConstants.MIX_REFERENCEABLE)) {
                        uniqueKeyNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
                    }

                    final Value weakReference = valueFactory.createValue(uniqueKeyNode, true);
                    addPropertyValue(node, JcrId.UNIQUE_KEYS, weakReference);
                }

                // - relational:uniqueKeyXmiUuids (string) multiple
                addPropertyValue(node, JcrId.UNIQUE_KEY_XMI_UUIDS, uniqueKeyUuid);

                // - relational:uniqueKeyHrefs (string) multiple
                addPropertyValue(node, JcrId.UNIQUE_KEY_HREFS, uniqueKeyRef);

                // - relational:uniqueKeyNames (string) multiple
                if (uniqueKeyNode != null) {
                    addPropertyValue(node, JcrId.UNIQUE_KEY_NAMES, uniqueKeyNode.getName());
                } else if (unresolved != null) {
                    unresolved.addResolvedName(element.getUuid(), JcrId.UNIQUE_KEY_NAMES);
                } else {
                    assert false;
                }
            }
        }
    }

    private void processXmiReferenceable( final XmiElement element,
                                          final Node node ) throws Exception {
        // - xmi:uuid (string) mandatory
        setProperty(node, XmiLexicon.JcrId.UUID, element.getUuid());
    }
}
