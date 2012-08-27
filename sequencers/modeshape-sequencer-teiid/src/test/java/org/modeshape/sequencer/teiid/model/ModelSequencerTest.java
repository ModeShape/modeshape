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

import static org.junit.Assert.assertNull;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import org.junit.Test;
import org.modeshape.jcr.JcrMixLexicon;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;

public class ModelSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequencePartsSupplierSourceAModel() throws Exception {
        createNodeWithContentFromFile("PartsSupplier_SourceA.xmi", "model/parts/PartsSupplier_SourceA.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsSupplier_SourceA.xmi", 5);
        assertNotNull(outputNode);

        // model annotation
        assertThat(outputNode.isNodeType(CoreLexicon.MODEL), is(true));
        assertThat(outputNode.isNodeType(XmiLexicon.REFERENCEABLE), is(true));
        assertThat(outputNode.isNodeType(JcrMixLexicon.REFERENCEABLE.getString()), is(true));
        assertThat(outputNode.getProperty(XmiLexicon.UUID).getString(), is("343b7200-1284-1eec-8518-c32201e76066"));
        assertThat(outputNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(), is(RelationalLexicon.Namespace.URI));
        assertThat(outputNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        assertThat(outputNode.getProperty(CoreLexicon.PRODUCER_NAME).getString(), is("MetaMatrix"));
        assertThat(outputNode.getProperty(CoreLexicon.PRODUCER_VERSION).getString(), is("5.0"));

        // model imports
        NodeIterator itr = outputNode.getNodes("XMLSchema");
        assertThat(itr.getSize(), is(1L));
        Node importNode = itr.nextNode();
        assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.IMPORT));
        assertThat(importNode.getProperty(XmiLexicon.UUID).getString(), is("a6591281-bf1d-1f2c-9911-b53abd16b14e"));
        assertThat(importNode.getProperty(CoreLexicon.MODEL_LOCATION).getString(), is("http://www.w3.org/2001/XMLSchema"));
        assertThat(importNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        assertThat(importNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(),
                   is("http://www.eclipse.org/xsd/2002/XSD"));
    }

    @Test
    public void shouldSequenceMyBooksViewModel() throws Exception {
        createNodeWithContentFromFile("MyBooksView.xmi", "model/books/MyBooksView.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MyBooksView.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequencePartsSupplierVirtualModel() throws Exception {
        createNodeWithContentFromFile("PartsVirtual.xmi", "model/parts/PartsVirtual.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsVirtual.xmi", 5);
        assertNotNull(outputNode);

        // model annotation
        assertThat(outputNode.isNodeType(CoreLexicon.MODEL), is(true));
        assertThat(outputNode.isNodeType(XmiLexicon.REFERENCEABLE), is(true));
        assertThat(outputNode.isNodeType(JcrMixLexicon.REFERENCEABLE.getString()), is(true));
        assertThat(outputNode.getProperty(XmiLexicon.UUID).getString(), is("fb52cb80-128a-1eec-8518-c32201e76066"));
        assertThat(outputNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(), is(RelationalLexicon.Namespace.URI));
        assertThat(outputNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
        assertThat(outputNode.getProperty(CoreLexicon.PRODUCER_NAME).getString(), is("Teiid Designer"));
        assertThat(outputNode.getProperty(CoreLexicon.PRODUCER_VERSION).getString(), is("6.0"));

        { // model imports
            Node importNode = outputNode.getNode("PartSupplier_SourceB");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.UUID).getString(), is("980de782-b1e5-1f55-853c-ed5dfdd1bb78"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_LOCATION).getString(), is("PartSupplier_SourceB.xmi"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(importNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(), is(RelationalLexicon.Namespace.URI));

            importNode = outputNode.getNode("PartsSupplier_SourceA");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.UUID).getString(), is("980de784-b1e5-1f55-853c-ed5dfdd1bb78"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_LOCATION).getString(), is("PartsSupplier_SourceA.xmi"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(importNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(), is(RelationalLexicon.Namespace.URI));

            importNode = outputNode.getNode("XMLSchema");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.UUID).getString(), is("a6591280-bf1d-1f2c-9911-b53abd16b14e"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_LOCATION).getString(), is("http://www.w3.org/2001/XMLSchema"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(importNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(),
                       is("http://www.eclipse.org/xsd/2002/XSD"));
        }

        { // tables
            Node tableNode = outputNode.getNode("SupplierInfo");
            assertNotNull(tableNode);
            assertThat(tableNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrIds.BASE_TABLE));
            assertThat(tableNode.getProperty(XmiLexicon.UUID).getString(), is("2473dbc0-128c-1eec-8518-c32201e76066"));
            assertThat(tableNode.getProperty(RelationalLexicon.JcrIds.SUPPORTS_UPDATE).getBoolean(), is(false));

            // defaults
            assertThat(tableNode.getProperty(RelationalLexicon.JcrIds.MATERIALIZED).getBoolean(), is(false));
            assertThat(tableNode.getProperty(RelationalLexicon.JcrIds.SYSTEM).getBoolean(), is(false));

            { // columns
                assertThat(tableNode.getNodes().getSize(), is(8L));

                Node columnNode = tableNode.getNode("SUPPLIER_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("143ff680-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("VARCHAR2"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NO_NULLS"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("PART_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("1d9b97c0-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(4L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("CHAR"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NO_NULLS"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("QUANTITY");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("250ef100-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("NUMBER"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.PRECISION).getLong(), is(3L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SHIPPER_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("2b8e2640-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("NUMBER"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.PRECISION).getLong(), is(2L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_NAME");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("34da8540-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(30L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_STATUS");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("3c4dde80-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("numeric"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.PRECISION).getLong(), is(2L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_CITY");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("43c137c0-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(30L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_STATE");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("4a4faf40-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(2L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));
            }
        }

        { // procedures
            Node procedureNode = outputNode.getNode("partsByColor");
            assertNotNull(procedureNode);
            assertThat(procedureNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrIds.PROCEDURE));
            assertThat(procedureNode.getProperty(XmiLexicon.UUID).getString(), is("27a60e44-129f-1eec-8518-c32201e76066"));

            { // parameters
                Node paramNode = procedureNode.getNode("colorIn");
                assertNotNull(paramNode);
                assertThat(paramNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrIds.PROCEDURE_PARAMETER));
                assertThat(paramNode.getProperty(XmiLexicon.UUID).getString(), is("56182540-12a6-1eec-8518-c32201e76066"));

                // parameter defaults
                assertThat(paramNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
            }

            { // results
                Node resultNode = procedureNode.getNode("NewProcedureResult");
                assertNotNull(resultNode);
                assertThat(resultNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrIds.PROCEDURE_RESULT));
                assertThat(resultNode.getProperty(XmiLexicon.UUID).getString(), is("77c5dc41-12a7-1eec-8518-c32201e76066"));

                // result columns
                assertThat(resultNode.getNodes().getSize(), is(4L));

                Node columnNode = resultNode.getNode("PART_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("86998480-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(50L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NO_NULLS"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(true));

                columnNode = resultNode.getNode("PART_NAME");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("8fe5e380-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(255L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(true));

                columnNode = resultNode.getNode("PART_COLOR");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("984d60c0-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(30L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(true));

                columnNode = resultNode.getNode("PART_WEIGHT");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("a0a59bc0-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.LENGTH).getLong(), is(255L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("varchar"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(true));
            }
        }
    }

    //
    // @Test
    // public void shouldSequenceBooksAModels() throws Exception {
    // XmiReader.read(streamFor("/model/books/BookDatatypes.xsd"), null);
    // XmiReader.read(streamFor("/model/books/Books_SourceA.xmi"), null);
    // XmiReader.read(streamFor("/model/books/Books_SourceB.xmi"), null);
    // XmiReader.read(streamFor("/model/books/Books.xsd"), null);
    // XmiReader.read(streamFor("/model/books/BooksInput.xsd"), null);
    // XmiReader.read(streamFor("/model/books/BooksWebService.xmi"), null);
    // XmiReader.read(streamFor("/model/books/BooksXML.xmi"), null);
    // }

    @Test
    public void shouldSequenceOldBooksPhysicalRelationalModelForOracle() throws Exception {
        createNodeWithContentFromFile("BooksO.xmi", "model/old/BooksO.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/BooksO.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceOldBooksPhysicalRelationalModelForSqlServer() throws Exception {
        createNodeWithContentFromFile("BooksS.xmi", "model/old/BooksS.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/BooksS.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceNewBooksPhysicalRelationalModelForSourceA() throws Exception {
        createNodeWithContentFromFile("Books_SourceA.xmi", "model/books/Books_SourceA.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Books_SourceA.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceNewBooksPhysicalRelationalModelForSourceB() throws Exception {
        createNodeWithContentFromFile("Books_SourceB.xmi", "model/books/Books_SourceB.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Books_SourceB.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequencePartsSupplierPhysicalRelationalModelForSourceB() throws Exception {
        createNodeWithContentFromFile("PartSupplier_SourceB.xmi", "model/parts/PartSupplier_SourceB.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartSupplier_SourceB.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForProducts() throws Exception {
        createNodeWithContentFromFile("Products.xmi", "model/YeeHaa/Products.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Products.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForMarketData() throws Exception {
        createNodeWithContentFromFile("MarketData.xmi", "model/YeeHaa/MarketData.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MarketData.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForCustomerAccounts() throws Exception {
        createNodeWithContentFromFile("Customer_Accounts.xmi", "model/YeeHaa/Customer_Accounts.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Customer_Accounts.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForMyPortfolio() throws Exception {
        createNodeWithContentFromFile("MyPortfolio.xmi", "model/YeeHaa/MyPortfolio.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MyPortfolio.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceRepresentativeRelationalModel() throws Exception {
        createNodeWithContentFromFile("RelationalModel.xmi", "model/relational/RelationalModel.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/RelationalModel.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceRelationalModelUsingXmlFromSource() throws Exception {
        createNodeWithContentFromFile("PartsView.xmi", "model/XmlParts/PartsView.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsView.xmi", 5);
        assertNotNull(outputNode);
    }

    @Test
    public void shouldNotSequenceXmlDocumentModelForEmployees() throws Exception {
        createNodeWithContentFromFile("EmpDoc.xmi", "model/QuickEmployees/EmpDoc.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/EmpDoc.xmi", 5);
        assertNull(outputNode);
    }

    @Test
    public void shouldSequenceBooksOracleModel() throws Exception {
        // this OLD model has schema object, and JDBC import settings
        createNodeWithContentFromFile("Books_Oracle.xmi", "model/books/Books_Oracle.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Books_Oracle.xmi", 5);
        assertNotNull(outputNode);

        // model annotation
        assertThat(outputNode.isNodeType(CoreLexicon.MODEL), is(true));
        assertThat(outputNode.isNodeType(XmiLexicon.REFERENCEABLE), is(true));
        assertThat(outputNode.isNodeType(JcrMixLexicon.REFERENCEABLE.getString()), is(true));
        assertThat(outputNode.getProperty(XmiLexicon.UUID).getString(), is("6f83e692-6183-464c-8a5f-2df8113c98ec"));
        assertThat(outputNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(), is(RelationalLexicon.Namespace.URI));
        assertThat(outputNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        assertThat(outputNode.getProperty(CoreLexicon.PRODUCER_NAME).getString(), is("Teiid Designer"));
        assertThat(outputNode.getProperty(CoreLexicon.PRODUCER_VERSION).getString(), is("7.4.0.qualifier"));
        assertThat(outputNode.getProperty(CoreLexicon.MAX_SET_SIZE).getLong(), is(1000L));

        { // model imports
            Node importNode = outputNode.getNode("XMLSchema");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.UUID).getString(), is("5ba789f7-13bb-4a0a-acd1-ee614a7c06fe"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_LOCATION).getString(), is("http://www.w3.org/2001/XMLSchema"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is("TYPE"));
            assertThat(importNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(),
                       is("http://www.eclipse.org/xsd/2002/XSD"));

            importNode = outputNode.getNode("SimpleDatatypes-instance");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.UUID).getString(), is("4cbd7bf3-033a-4898-9811-233b043c5c0a"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_LOCATION).getString(),
                       is("http://www.metamatrix.com/metamodels/SimpleDatatypes-instance"));
            assertThat(importNode.getProperty(CoreLexicon.MODEL_TYPE).getString(), is("TYPE"));
            assertThat(importNode.getProperty(CoreLexicon.PRIMARY_METAMODEL_URI).getString(),
                       is("http://www.eclipse.org/xsd/2002/XSD"));
        }

        { // schema
            Node schemaNode = outputNode.getNode("BOOKS");
            assertNotNull(schemaNode);
            assertThat(schemaNode.getProperty(XmiLexicon.UUID).getString(), is("1e40dcf2-8113-4c0b-81de-5a9dbf8bede0"));
            assertThat(schemaNode.getProperty(RelationalLexicon.JcrIds.NAME_IN_SOURCE).getString(), is("BOOKS"));

            { // table
                Node tableNode = schemaNode.getNode("AUTHORS");
                assertNotNull(tableNode);
                assertThat(tableNode.getProperty(XmiLexicon.UUID).getString(), is("0351c0b2-f83c-4e1a-b9b2-765f8ee22c26"));
                assertThat(tableNode.getProperty(RelationalLexicon.JcrIds.NAME_IN_SOURCE).getString(), is("AUTHORS"));

                { // columns
                    Node columnNode = tableNode.getNode("AUTHOR_ID");
                    assertNotNull(columnNode);
                    assertThat(columnNode.getProperty(XmiLexicon.UUID).getString(), is("3a98c9a7-9298-4495-9ba9-13d54da54592"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NAME_IN_SOURCE).getString(), is("AUTHOR_ID"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CASE_SENSITIVE).getBoolean(), is(false));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.FIXED_LENGTH).getBoolean(), is(true));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NATIVE_TYPE).getString(), is("NUMBER"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULLABLE).getString(), is("NO_NULLS"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.PRECISION).getLong(), is(10L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SEARCHABILITY).getString(), is("ALL_EXCEPT_LIKE"));

                    // column defaults
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.AUTO_INCREMENTED).getBoolean(), is(false));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.CURRENCY).getBoolean(), is(false));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.NULL_VALUE_COUNT).getLong(), is(-1L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.RADIX).getLong(), is(10L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SELECTABLE).getBoolean(), is(true));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.SIGNED).getBoolean(), is(true));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrIds.UPDATEABLE).getBoolean(), is(true));
                }
            }
        }

        { // JDBC import settings
          // source
            Node jdbcSourceNode = outputNode.getNode("Books Oracle");
            assertNotNull(jdbcSourceNode);
            assertThat(jdbcSourceNode.getPrimaryNodeType().getName(), is(JdbcLexicon.JcrIds.SOURCE));
            assertThat(jdbcSourceNode.getProperty(XmiLexicon.UUID).getString(), is("a0444c0c-35b3-4ddf-ace4-2b0ce2e5b931"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrIds.DRIVER_NAME).getString(), is("Oracle Thin Driver"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrIds.DRIVER_CLASS).getString(), is("oracle.jdbc.OracleDriver"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrIds.USER_NAME).getString(), is("books"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrIds.URL).getString(),
                       is("jdbc:oracle:thin:@englxdbs11.mm.atl2.redhat.com:1521:ORCL"));

            // import settings
            Node settings = jdbcSourceNode.getNode(JdbcLexicon.JcrIds.IMPORTED);
            assertNotNull(settings);
            assertThat(settings.getPrimaryNodeType().getName(), is(JdbcLexicon.JcrIds.IMPORTED));
            assertThat(settings.getProperty(XmiLexicon.UUID).getString(), is("5c6e0cc1-400d-4e3b-aa8a-d4fdef6a3e36"));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.INCLUDE_INDEXES).getBoolean(), is(false));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.INCLUDE_APPROXIMATE_INDEXES).getBoolean(), is(false));

            // defaults
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.CREATE_CATALOGS_IN_MODEL).getBoolean(), is(true));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.CREATE_SCHEMAS_IN_MODEL).getBoolean(), is(true));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.GENERATE_SOURCE_NAMES_IN_MODEL).getString(), is("UNQUALIFIED"));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.INCLUDE_FOREIGN_KEYS).getBoolean(), is(true));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.INCLUDE_PROCEDURES).getBoolean(), is(false));
            assertThat(settings.getProperty(JdbcLexicon.JcrIds.INCLUDE_UNIQUE_INDEXES).getBoolean(), is(false));

            // muti-value properties
            Property property = settings.getProperty(JdbcLexicon.JcrIds.INCLUDED_SCHEMA_PATHS);
            assertNotNull(property);
            assertThat(property.getValues().length, is(2));
            assertThat(property.getValues()[0].getString(), is("/BOOKS"));
            assertThat(property.getValues()[1].getString(), is("/EBOOKS"));

            property = settings.getProperty(JdbcLexicon.JcrIds.INCLUDED_TABLE_TYPES);
            assertNotNull(property);
            assertThat(property.getValues().length, is(1));
            assertThat(property.getValues()[0].getString(), is("TABLE"));
        }
    }
}
