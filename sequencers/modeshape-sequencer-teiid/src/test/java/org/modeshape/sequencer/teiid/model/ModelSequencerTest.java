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

import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.RowIterator;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.junit.SkipLongRunning;
import org.modeshape.jcr.JcrMixLexicon;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.JdbcLexicon;
import org.modeshape.sequencer.teiid.lexicon.ModelExtensionDefinitionLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon.JcrId;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;

public class ModelSequencerTest extends AbstractSequencerTest {

    @Test
    public void shouldSequencePartsSupplierSourceAModel() throws Exception {
        createNodeWithContentFromFile("PartsSupplier_SourceA.xmi", "model/parts/PartsSupplier_SourceA.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsSupplier_SourceA.xmi");
        assertNotNull(outputNode);

        // model annotation
        assertThat(outputNode.isNodeType(CoreLexicon.JcrId.MODEL), is(true));
        assertThat(outputNode.isNodeType(XmiLexicon.JcrId.REFERENCEABLE), is(true));
        assertThat(outputNode.isNodeType(JcrMixLexicon.REFERENCEABLE.getString()), is(true));
        assertThat(outputNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("343b7200-1284-1eec-8518-c32201e76066"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                   is(RelationalLexicon.Namespace.URI));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRODUCER_NAME).getString(), is("MetaMatrix"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRODUCER_VERSION).getString(), is("5.0"));

        // model imports
        NodeIterator itr = outputNode.getNodes("XMLSchema");
        assertThat(itr.getSize(), is(1L));
        Node importNode = itr.nextNode();
        assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
        assertThat(importNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("a6591281-bf1d-1f2c-9911-b53abd16b14e"));
        assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(), is("http://www.w3.org/2001/XMLSchema"));
        assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        assertThat(importNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                   is("http://www.eclipse.org/xsd/2002/XSD"));
    }

    @Test
    public void shouldSequenceMyBooksViewModel() throws Exception {
        createNodeWithContentFromFile("MyBooksView.xmi", "model/books/MyBooksView.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MyBooksView.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceBooksProceduresModel() throws Exception {
        createNodeWithContentFromFile("BooksProcedures.xmi", "model/books/BooksProcedures.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/BooksProcedures.xmi");
        assertNotNull(outputNode);

        // make sure procedure has the extension properties and mixin
        Node procedureNode = outputNode.getNode("getBooks");
        assertNotNull(procedureNode);
        assertThat(procedureNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.PROCEDURE));
        assertThat(procedureNode.isNodeType("rest:procedure"), is(true));
        assertThat(procedureNode.getProperty("rest:restMethod").getString(), is("GET"));
        assertThat(procedureNode.getProperty("rest:uri").getString(), is("books"));
    }

    @Test
    public void shouldSequenceMyBooksModel() throws Exception {
        createNodeWithContentFromFile("MyBooks.xmi", "model/books/MyBooks.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MyBooks.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequencePartsSupplierVirtualModel() throws Exception {
        createNodeWithContentFromFile("PartsVirtual.xmi", "model/parts/PartsVirtual.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsVirtual.xmi");
        assertNotNull(outputNode);

        // model annotation
        assertThat(outputNode.isNodeType(CoreLexicon.JcrId.MODEL), is(true));
        assertThat(outputNode.isNodeType(XmiLexicon.JcrId.REFERENCEABLE), is(true));
        assertThat(outputNode.isNodeType(JcrMixLexicon.REFERENCEABLE.getString()), is(true));
        assertThat(outputNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("fb52cb80-128a-1eec-8518-c32201e76066"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                   is(RelationalLexicon.Namespace.URI));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.VIRTUAL));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRODUCER_NAME).getString(), is("Teiid Designer"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRODUCER_VERSION).getString(), is("6.0"));

        { // model imports
            Node importNode = outputNode.getNode("PartSupplier_SourceB");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("980de782-b1e5-1f55-853c-ed5dfdd1bb78"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(), is("PartSupplier_SourceB.xmi"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                       is(RelationalLexicon.Namespace.URI));

            importNode = outputNode.getNode("PartsSupplier_SourceA");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("980de784-b1e5-1f55-853c-ed5dfdd1bb78"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(), is("PartsSupplier_SourceA.xmi"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                       is(RelationalLexicon.Namespace.URI));

            importNode = outputNode.getNode("XMLSchema");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("a6591280-bf1d-1f2c-9911-b53abd16b14e"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(),
                       is("http://www.w3.org/2001/XMLSchema"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                       is("http://www.eclipse.org/xsd/2002/XSD"));
        }

        { // tables
            Node tableNode = outputNode.getNode("SupplierInfo");
            assertNotNull(tableNode);
            assertThat(tableNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.BASE_TABLE));
            assertThat(tableNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("2473dbc0-128c-1eec-8518-c32201e76066"));
            assertThat(tableNode.getProperty(RelationalLexicon.JcrId.SUPPORTS_UPDATE).getBoolean(), is(false));

            // defaults
            assertThat(tableNode.getProperty(RelationalLexicon.JcrId.MATERIALIZED).getBoolean(), is(false));
            assertThat(tableNode.getProperty(RelationalLexicon.JcrId.SYSTEM).getBoolean(), is(false));

            // transformation
            assertThat(tableNode.isNodeType(TransformLexicon.JcrId.TRANSFORMED), is(true));
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_HREFS).getValues().length, is(2));
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_HREFS).getValues()[0].getString(),
                       is("PartSupplier_SourceB.xmi#mmuuid/54ed0900-1275-1eec-8518-c32201e76066"));
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_HREFS).getValues()[1].getString(),
                       is("PartsSupplier_SourceA.xmi#mmuuid/bc400080-1284-1eec-8518-c32201e76066"));
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_XMI_UUIDS).getValues().length, is(2));
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_XMI_UUIDS).getValues()[0].getString(),
                       is("54ed0900-1275-1eec-8518-c32201e76066"));
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_XMI_UUIDS).getValues()[1].getString(),
                       is("bc400080-1284-1eec-8518-c32201e76066"));
            assertThat(tableNode.isNodeType(TransformLexicon.JcrId.WITH_SQL), is(true));
            final String selectSql = "SELECT PartSupplier_Oracle.SUPPLIER_PARTS.SUPPLIER_ID, PartSupplier_Oracle.SUPPLIER_PARTS.PART_ID, PartSupplier_Oracle.SUPPLIER_PARTS.QUANTITY, PartSupplier_Oracle.SUPPLIER_PARTS.SHIPPER_ID, PartsSupplier_SQLServer.SUPPLIER.SUPPLIER_NAME, PartsSupplier_SQLServer.SUPPLIER.SUPPLIER_STATUS, PartsSupplier_SQLServer.SUPPLIER.SUPPLIER_CITY, PartsSupplier_SQLServer.SUPPLIER.SUPPLIER_STATE FROM PartSupplier_Oracle.SUPPLIER_PARTS, PartsSupplier_SQLServer.SUPPLIER WHERE PartSupplier_Oracle.SUPPLIER_PARTS.SUPPLIER_ID = PartsSupplier_SQLServer.SUPPLIER.SUPPLIER_ID";
            assertThat(tableNode.getProperty(TransformLexicon.JcrId.SELECT_SQL).getString(), is(selectSql));

            { // columns
                assertThat(tableNode.getNodes().getSize(), is(8L));

                Node columnNode = tableNode.getNode("SUPPLIER_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("143ff680-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("VARCHAR2"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NO_NULLS"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.TYPE_XMI_UUID).getString(),
                           is("bf6c34c0-c442-1e24-9b01-c8207cd53eb7"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.TYPE_NAME).getString(), is("string"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.TYPE_HREF).getString(),
                           is("http://www.w3.org/2001/XMLSchema#string"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                // column transformed
                assertThat(columnNode.isNodeType(TransformLexicon.JcrId.TRANSFORMED), is(true));
                assertThat(columnNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_HREFS).getValues().length, is(1));
                assertThat(columnNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_HREFS).getValues()[0].getString(),
                           is("PartSupplier_SourceB.xmi#mmuuid/55e12d01-1275-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_XMI_UUIDS).getValues().length, is(1));
                assertThat(columnNode.getProperty(TransformLexicon.JcrId.TRANSFORMED_FROM_XMI_UUIDS).getValues()[0].getString(),
                           is("55e12d01-1275-1eec-8518-c32201e76066"));

                columnNode = tableNode.getNode("PART_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("1d9b97c0-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(4L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("CHAR"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NO_NULLS"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("QUANTITY");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("250ef100-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("NUMBER"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.PRECISION).getLong(), is(3L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.TYPE_XMI_UUID).getString(),
                           is("5bbcf140-b9ae-1e21-b812-969c8fc8b016"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.TYPE_NAME).getString(), is("short"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.TYPE_HREF).getString(),
                           is("http://www.w3.org/2001/XMLSchema#short"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SHIPPER_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("2b8e2640-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("NUMBER"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.PRECISION).getLong(), is(2L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_NAME");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("34da8540-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(30L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_STATUS");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("3c4dde80-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.FIXED_LENGTH).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("numeric"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.PRECISION).getLong(), is(2L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_CITY");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("43c137c0-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(30L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));

                columnNode = tableNode.getNode("SUPPLIER_STATE");
                assertNotNull(columnNode);
                assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("4a4faf40-1291-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(2L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(false));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));
            }
        }

        { // procedures
            Node procedureNode = outputNode.getNode("partsByColor");
            assertNotNull(procedureNode);
            assertThat(procedureNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.PROCEDURE));
            assertThat(procedureNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("27a60e44-129f-1eec-8518-c32201e76066"));

            { // parameters
                Node paramNode = procedureNode.getNode("colorIn");
                assertNotNull(paramNode);
                assertThat(paramNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.PROCEDURE_PARAMETER));
                assertThat(paramNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("56182540-12a6-1eec-8518-c32201e76066"));

                // parameter defaults
                assertThat(paramNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
            }

            { // results
                Node resultNode = procedureNode.getNode("NewProcedureResult");
                assertNotNull(resultNode);
                assertThat(resultNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.PROCEDURE_RESULT));
                assertThat(resultNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("77c5dc41-12a7-1eec-8518-c32201e76066"));

                // result columns
                assertThat(resultNode.getNodes().getSize(), is(4L));

                Node columnNode = resultNode.getNode("PART_ID");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("86998480-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(50L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NO_NULLS"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(true));

                columnNode = resultNode.getNode("PART_NAME");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("8fe5e380-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(255L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(true));

                columnNode = resultNode.getNode("PART_COLOR");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("984d60c0-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(30L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(true));

                columnNode = resultNode.getNode("PART_WEIGHT");
                assertNotNull(columnNode);
                assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("a0a59bc0-12be-1eec-8518-c32201e76066"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.LENGTH).getLong(), is(255L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("varchar"));

                // column defaults
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NULLABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("SEARCHABLE"));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));
                assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(true));
            }
        }
    }

    @Test
    public void shouldSequenceOldBooksPhysicalRelationalModelForOracle() throws Exception {
        createNodeWithContentFromFile("BooksO.xmi", "model/old/BooksO.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/BooksO.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceOldBooksPhysicalRelationalModelForSqlServer() throws Exception {
        createNodeWithContentFromFile("BooksS.xmi", "model/old/BooksS.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/BooksS.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceNewBooksPhysicalRelationalModelForSourceA() throws Exception {
        createNodeWithContentFromFile("Books_SourceA.xmi", "model/books/Books_SourceA.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Books_SourceA.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceNewBooksPhysicalRelationalModelForSourceB() throws Exception {
        createNodeWithContentFromFile("Books_SourceB.xmi", "model/books/Books_SourceB.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Books_SourceB.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequencePartsSupplierPhysicalRelationalModelForSourceB() throws Exception {
        createNodeWithContentFromFile("PartSupplier_SourceB.xmi", "model/parts/PartSupplier_SourceB.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartSupplier_SourceB.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForProducts() throws Exception {
        createNodeWithContentFromFile("Products.xmi", "model/YeeHaa/Products.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Products.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForMarketData() throws Exception {
        createNodeWithContentFromFile("MarketData.xmi", "model/YeeHaa/MarketData.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MarketData.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForCustomerAccounts() throws Exception {
        createNodeWithContentFromFile("Customer_Accounts.xmi", "model/YeeHaa/Customer_Accounts.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Customer_Accounts.xmi");
        assertNotNull(outputNode);

        Node tableNode = outputNode.getNode(("CUSTOMER"));
        assertNotNull(tableNode);
        assertThat(tableNode.isNodeType(RelationalLexicon.JcrId.TABLE), is(true));

        { // indexes attribute
            Node columnNode = tableNode.getNode("CUSTID");
            assertNotNull(columnNode);
            assertThat(columnNode.isNodeType(RelationalLexicon.JcrId.COLUMN), is(true));
            assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("664708a0-e0b2-4b9e-8b0f-2b49528e0e31"));
            assertThat(columnNode.getProperty(JcrId.NAME_IN_SOURCE).getString(), is("CUSTID"));
            assertThat(columnNode.getProperty(JcrId.INDEX_XMI_UUIDS).getValues().length, is(1));
            assertThat(columnNode.getProperty(JcrId.INDEX_XMI_UUIDS).getValues()[0].getString(),
                       is("eb7b26ea-b003-4a2a-8b1d-3518952997f2"));
            assertThat(columnNode.getProperty(JcrId.INDEX_NAMES).getValues().length, is(1));
            assertThat(columnNode.getProperty(JcrId.INDEX_NAMES).getValues()[0].getString(), is("IX_CUSTOMER"));
        }

        { // index
            Node indexNode = outputNode.getNode("IX_CUSTOMER");
            assertNotNull(indexNode);
            assertThat(indexNode.isNodeType(RelationalLexicon.JcrId.INDEX), is(true));
            assertThat(indexNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("eb7b26ea-b003-4a2a-8b1d-3518952997f2"));
            assertThat(indexNode.getProperty(JcrId.NAME_IN_SOURCE).getString(), is("CUSTOMER"));
            assertThat(indexNode.getProperty(JcrId.UNIQUE).getBoolean(), is(true));
        }
    }

    @Test
    public void shouldSequenceYeeHaaPhysicalRelationalModelForMyPortfolio() throws Exception {
        createNodeWithContentFromFile("MyPortfolio.xmi", "model/YeeHaa/MyPortfolio.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/MyPortfolio.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceRelationalModel() throws Exception {
        createNodeWithContentFromFile("RelationalModel.xmi", "model/relational/RelationalModel.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/RelationalModel.xmi");
        assertNotNull(outputNode);

        { // test model object annotation
            Node catalogEmptyNode = outputNode.getNode("Catalog_Empty");
            assertNotNull(catalogEmptyNode);
            assertThat(catalogEmptyNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.CATALOG));
            assertTrue(catalogEmptyNode.isNodeType(CoreLexicon.JcrId.ANNOTATED));
            assertThat(catalogEmptyNode.getProperty(CoreLexicon.JcrId.DESCRIPTION).getString(),
                       is("Empty Catalog in relational model"));
        }

        Node catalogNode = outputNode.getNode("Catalog");
        assertNotNull(catalogNode);
        assertThat(catalogNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.CATALOG));

        Node schemaNode = catalogNode.getNode(("SchemaInCatalog"));
        assertNotNull(schemaNode);
        assertThat(schemaNode.isNodeType(RelationalLexicon.JcrId.SCHEMA), is(true));

        { // test access pattern
            Node viewNode = schemaNode.getNode(("View"));
            assertNotNull(viewNode);
            assertThat(viewNode.isNodeType(RelationalLexicon.JcrId.VIEW), is(true));

            Node accessPatternNode = viewNode.getNode("AccessPattern");
            assertNotNull(accessPatternNode);
            assertThat(accessPatternNode.isNodeType(RelationalLexicon.JcrId.ACCESS_PATTERN), is(true));
            assertThat(accessPatternNode.getProperty(XmiLexicon.JcrId.UUID).getString(),
                       is("bd3c041e-6982-4d08-9cd2-31eb94cc6c69"));
            assertThat(accessPatternNode.getProperty(JcrId.NAME_IN_SOURCE).getString(), is("MyAccessPatter"));
            assertThat(accessPatternNode.getProperty(JcrId.COLUMN_XMI_UUIDS).getValues().length, is(1));
            assertThat(accessPatternNode.getProperty(JcrId.COLUMN_XMI_UUIDS).getValues()[0].getString(),
                       is("580d34a0-c1d7-4a80-9fd5-2bbc19fa47c5"));
            assertThat(accessPatternNode.getProperty(JcrId.COLUMN_NAMES).getValues().length, is(1));
            assertThat(accessPatternNode.getProperty(JcrId.COLUMN_NAMES).getValues()[0].getString(), is("column"));
        }
    }

    @Test
    public void shouldSequenceRelationalModelUsingXmlFromSource() throws Exception {
        createNodeWithContentFromFile("PartsView.xmi", "model/XmlParts/PartsView.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsView.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequencePartsDataModel() throws Exception {
        createNodeWithContentFromFile("PartsData.xmi", "model/XmlParts/PartsData.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/PartsData.xmi");
        assertNotNull(outputNode);
    }

    @Test
    @SkipLongRunning
    public void shouldNotSequenceXmlDocumentModelForEmployees() throws Exception {
        createNodeWithContentFromFile("EmpDoc.xmi", "model/QuickEmployees/EmpDoc.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/EmpDoc.xmi");
        assertNull(outputNode);
    }

    @Test
    public void shouldSequenceEmployeesModel() throws Exception {
        createNodeWithContentFromFile("Employees.xmi", "model/QuickEmployees/Employees.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Employees.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceEmpVModel() throws Exception {
        createNodeWithContentFromFile("EmpV.xmi", "model/QuickEmployees/EmpV.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/EmpV.xmi");
        assertNotNull(outputNode);
    }

    @Test
    public void shouldSequenceBooksOracleModel() throws Exception {
        // this OLD model has schema object, and JDBC import settings
        createNodeWithContentFromFile("Books_Oracle.xmi", "model/books/Books_Oracle.xmi");
        Node outputNode = getOutputNode(this.rootNode, "models/Books_Oracle.xmi");
        assertNotNull(outputNode);

        // model annotation
        assertThat(outputNode.isNodeType(CoreLexicon.JcrId.MODEL), is(true));
        assertThat(outputNode.isNodeType(XmiLexicon.JcrId.REFERENCEABLE), is(true));
        assertThat(outputNode.isNodeType(JcrMixLexicon.REFERENCEABLE.getString()), is(true));
        assertThat(outputNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("6f83e692-6183-464c-8a5f-2df8113c98ec"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                   is(RelationalLexicon.Namespace.URI));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is(CoreLexicon.ModelType.PHYSICAL));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRODUCER_NAME).getString(), is("Teiid Designer"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.PRODUCER_VERSION).getString(), is("7.4.0.qualifier"));
        assertThat(outputNode.getProperty(CoreLexicon.JcrId.MAX_SET_SIZE).getLong(), is(1000L));

        { // model imports
            Node importNode = outputNode.getNode("XMLSchema");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("5ba789f7-13bb-4a0a-acd1-ee614a7c06fe"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(),
                       is("http://www.w3.org/2001/XMLSchema"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is("TYPE"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                       is("http://www.eclipse.org/xsd/2002/XSD"));

            importNode = outputNode.getNode("SimpleDatatypes-instance");
            assertNotNull(importNode);
            assertThat(importNode.getPrimaryNodeType().getName(), is(CoreLexicon.JcrId.IMPORT));
            assertThat(importNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("4cbd7bf3-033a-4898-9811-233b043c5c0a"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_LOCATION).getString(),
                       is("http://www.metamatrix.com/metamodels/SimpleDatatypes-instance"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.MODEL_TYPE).getString(), is("TYPE"));
            assertThat(importNode.getProperty(CoreLexicon.JcrId.PRIMARY_METAMODEL_URI).getString(),
                       is("http://www.eclipse.org/xsd/2002/XSD"));
        }

        { // schema
            Node schemaNode = outputNode.getNode("BOOKS");
            assertNotNull(schemaNode);
            assertThat(schemaNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("1e40dcf2-8113-4c0b-81de-5a9dbf8bede0"));
            assertThat(schemaNode.getProperty(RelationalLexicon.JcrId.NAME_IN_SOURCE).getString(), is("BOOKS"));

            { // table
                Node tableNode = schemaNode.getNode("AUTHORS");
                assertNotNull(tableNode);
                assertThat(tableNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("0351c0b2-f83c-4e1a-b9b2-765f8ee22c26"));
                assertThat(tableNode.getProperty(RelationalLexicon.JcrId.NAME_IN_SOURCE).getString(), is("AUTHORS"));

                { // columns
                    Node columnNode = tableNode.getNode("AUTHOR_ID");
                    assertNotNull(columnNode);
                    assertThat(columnNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.COLUMN));
                    assertThat(columnNode.getProperty(XmiLexicon.JcrId.UUID).getString(),
                               is("3a98c9a7-9298-4495-9ba9-13d54da54592"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NAME_IN_SOURCE).getString(), is("AUTHOR_ID"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CASE_SENSITIVE).getBoolean(), is(false));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.FIXED_LENGTH).getBoolean(), is(true));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NATIVE_TYPE).getString(), is("NUMBER"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULLABLE).getString(), is("NO_NULLS"));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.PRECISION).getLong(), is(10L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SEARCHABILITY).getString(), is("ALL_EXCEPT_LIKE"));

                    // column defaults
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.AUTO_INCREMENTED).getBoolean(), is(false));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.CURRENCY).getBoolean(), is(false));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.DISTINCT_VALUE_COUNT).getLong(), is(-1L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.NULL_VALUE_COUNT).getLong(), is(-1L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.RADIX).getLong(), is(10L));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SELECTABLE).getBoolean(), is(true));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.SIGNED).getBoolean(), is(true));
                    assertThat(columnNode.getProperty(RelationalLexicon.JcrId.UPDATEABLE).getBoolean(), is(true));
                }

                { // primary key
                    Node primaryKeyNode = tableNode.getNode("PK_AUTHORS");
                    assertNotNull(primaryKeyNode);
                    assertThat(primaryKeyNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.PRIMARY_KEY));
                    assertThat(primaryKeyNode.getProperty(XmiLexicon.JcrId.UUID).getString(),
                               is("2ab9553e-1b7a-401e-8076-0c6becdc03bd"));
                    assertThat(primaryKeyNode.getProperty(RelationalLexicon.JcrId.NAME_IN_SOURCE).getString(), is("PK_AUTHORS"));
                }
            }

            { // BOOKS table
                Node tableNode = schemaNode.getNode("BOOKS");
                assertNotNull(tableNode);
                assertThat(tableNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("aab80502-e67d-4a58-9f0b-af9d136c43a0"));
                assertThat(tableNode.getProperty(RelationalLexicon.JcrId.NAME_IN_SOURCE).getString(), is("BOOKS"));

                { // foreign key
                    Node foreignKey = tableNode.getNode("FK_PUBLISHER");
                    assertNotNull(foreignKey);
                    assertThat(foreignKey.getProperty(XmiLexicon.JcrId.UUID).getString(),
                               is("32963de6-3d4a-4309-a19d-d709413c825f"));
                    assertThat(foreignKey.getProperty(RelationalLexicon.JcrId.NAME_IN_SOURCE).getString(), is("FK_PUBLISHER"));
                    assertThat(foreignKey.getProperty(RelationalLexicon.JcrId.FOREIGN_KEY_MULTIPLICITY).getString(),
                               is("UNSPECIFIED"));
                    assertThat(foreignKey.getProperty(RelationalLexicon.JcrId.PRIMARY_KEY_MULTIPLICITY).getString(),
                               is("UNSPECIFIED"));
                }
            }
        }

        { // JDBC import settings
          // source
            Node jdbcSourceNode = outputNode.getNode("Books Oracle");
            assertNotNull(jdbcSourceNode);
            assertThat(jdbcSourceNode.getPrimaryNodeType().getName(), is(JdbcLexicon.JcrId.SOURCE));
            assertThat(jdbcSourceNode.getProperty(XmiLexicon.JcrId.UUID).getString(), is("a0444c0c-35b3-4ddf-ace4-2b0ce2e5b931"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrId.DRIVER_NAME).getString(), is("Oracle Thin Driver"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrId.DRIVER_CLASS).getString(), is("oracle.jdbc.OracleDriver"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrId.USER_NAME).getString(), is("books"));
            assertThat(jdbcSourceNode.getProperty(JdbcLexicon.JcrId.URL).getString(),
                       is("jdbc:oracle:thin:@englxdbs11.mm.atl2.redhat.com:1521:ORCL"));

            // import settings
            Node settings = outputNode.getNode(JdbcLexicon.JcrId.IMPORTED);
            assertNotNull(settings);
            assertThat(settings.getPrimaryNodeType().getName(), is(JdbcLexicon.JcrId.IMPORTED));
            assertThat(settings.getProperty(XmiLexicon.JcrId.UUID).getString(), is("5c6e0cc1-400d-4e3b-aa8a-d4fdef6a3e36"));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.INCLUDE_INDEXES).getBoolean(), is(false));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.INCLUDE_APPROXIMATE_INDEXES).getBoolean(), is(false));

            // defaults
            assertThat(settings.getProperty(JdbcLexicon.JcrId.CREATE_CATALOGS_IN_MODEL).getBoolean(), is(true));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.CREATE_SCHEMAS_IN_MODEL).getBoolean(), is(true));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.GENERATE_SOURCE_NAMES_IN_MODEL).getString(), is("UNQUALIFIED"));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.INCLUDE_FOREIGN_KEYS).getBoolean(), is(true));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.INCLUDE_PROCEDURES).getBoolean(), is(false));
            assertThat(settings.getProperty(JdbcLexicon.JcrId.INCLUDE_UNIQUE_INDEXES).getBoolean(), is(false));

            // muti-value properties
            Property property = settings.getProperty(JdbcLexicon.JcrId.INCLUDED_SCHEMA_PATHS);
            assertNotNull(property);
            assertThat(property.getValues().length, is(2));
            assertThat(property.getValues()[0].getString(), is("/BOOKS"));
            assertThat(property.getValues()[1].getString(), is("/EBOOKS"));

            property = settings.getProperty(JdbcLexicon.JcrId.INCLUDED_TABLE_TYPES);
            assertNotNull(property);
            assertThat(property.getValues().length, is(1));
            assertThat(property.getValues()[0].getString(), is("TABLE"));
        }
    }

    @Test
    public void shouldSequenceModelWithMultipleMeds() throws Exception {
        createNodeWithContentFromFile("ModelWithTwoMeds.xmi", "model/modelExtensionDefinition/ModelWithTwoMeds.xmi");
        final Node outputNode = getOutputNode(this.rootNode, "models/ModelWithTwoMeds.xmi");
        assertNotNull(outputNode);

        { // MED group node
            final Node medGroupNode = outputNode.getNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
            assertNotNull(medGroupNode);

            // relational MED node
            assertNotNull(medGroupNode.getNode("relational"));

            // salesforce MED node
            assertNotNull(medGroupNode.getNode("salesforce"));
        }

        { // model object MED properties
            Node tableNode = outputNode.getNode("productdata");
            assertNotNull(tableNode);
            assertThat(tableNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.BASE_TABLE));

            // check mixin
            assertThat(tableNode.isNodeType("salesforce:baseTable"), is(true));

            // need to reference property in this form since "relational" prefix has already been registered by sequencer init
            // sequencer will create a new prefix for the "relational" MED. Probable new prefix will be "relational2"
            // the MED URI is suffixed with a slash and the version number
            assertThat(tableNode.getProperty("{http://www.teiid.org/ext/relational/2012/2}native-query").getString(),
                       is("SELECT * FROM EastCoastProduct"));

            // check explicitly MED property set in model
            assertThat(tableNode.getProperty("salesforce:Custom").getBoolean(), is(true));
            assertThat(tableNode.getProperty("{http://www.teiid.org/translator/salesforce/2012/1}Custom").getBoolean(), is(true));

            // check MED default values
            assertThat(tableNode.getProperty("salesforce:Supports Create").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Delete").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports ID Lookup").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Merge").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Query").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Replicate").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Retrieve").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Search").getBoolean(), is(false));
        }

        { // model object MED properties
            Node tableNode = outputNode.getNode("productsymbols");
            assertNotNull(tableNode);
            assertThat(tableNode.getPrimaryNodeType().getName(), is(RelationalLexicon.JcrId.BASE_TABLE));

            // check mixin
            assertThat(tableNode.isNodeType("salesforce:baseTable"), is(true));

            // explicitly check MED property set in model
            assertThat(tableNode.getProperty("salesforce:Supports Query").getBoolean(), is(true));
            assertThat(tableNode.getProperty("{http://www.teiid.org/translator/salesforce/2012/1}Supports Query").getBoolean(),
                       is(true));

            // check MED default values
            assertThat(tableNode.getProperty("salesforce:Custom").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Create").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Delete").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports ID Lookup").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Merge").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Replicate").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Retrieve").getBoolean(), is(false));
            assertThat(tableNode.getProperty("salesforce:Supports Search").getBoolean(), is(false));
        }
    }

    @Test
    public void shouldSequenceModelWithOneMed() throws Exception {
        createNodeWithContentFromFile("ModelWithOneMed.xmi", "model/modelExtensionDefinition/ModelWithOneMed.xmi");
        final Node outputNode = getOutputNode(this.rootNode, "models/ModelWithOneMed.xmi");
        assertNotNull(outputNode);

        // MED group node
        final Node medGroupNode = outputNode.getNode(CoreLexicon.JcrId.MODEL_EXTENSION_DEFINITIONS_GROUP_NODE);
        assertNotNull(medGroupNode);

        // MED node exists
        final Node medNode = medGroupNode.getNode("relational");
        assertNotNull(medNode);

        // MED properties
        assertThat(medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.EXTENDED_METAMODEL).getString(),
                   is("http://www.metamatrix.com/metamodels/Relational"));
        assertThat(medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_PREFIX).getString(), is("relational"));
        assertThat(medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.NAMESPACE_URI).getString(),
                   is("http://www.teiid.org/ext/relational/2012"));
        assertThat(medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.VERSION).getLong(), is(2L));
        assertThat(medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.DESCRIPTION).getString(),
                   is("Relational metamodel extension properties"));

        // MED model types is multi-valued
        Value[] modelTypes = medNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.MODEL_TYPES).getValues();
        assertThat(modelTypes.length, is(2));
        String[] values = new String[] {modelTypes[0].getString(), modelTypes[1].getString()};
        assertThat(values, hasItemInArray("PHYSICAL"));
        assertThat(values, hasItemInArray("VIRTUAL"));

        // Extended metaclasses
        final NodeIterator nodeItr = medNode.getNodes();
        assertThat(nodeItr.getSize(), is(2L));
        boolean foundProcedure = false;
        boolean foundTable = false;

        while (nodeItr.hasNext()) {
            Node metaclassNode = nodeItr.nextNode();
            assertThat(metaclassNode.getPrimaryNodeType().getName(), is(ModelExtensionDefinitionLexicon.JcrId.EXTENDED_METACLASS));

            if (!foundProcedure && "org.teiid.designer.metamodels.relational.impl.ProcedureImpl".equals(metaclassNode.getName())) {
                foundProcedure = true;

                // 8 property definitions
                NodeIterator propDefItr = metaclassNode.getNodes();
                assertThat(propDefItr.getSize(), is(8L));

                { // check one property definition : deterministic
                    final Node propNode = metaclassNode.getNode("{http://www.teiid.org/ext/relational/2012/2}deterministic");
                    assertNotNull(propNode);
                    assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.ADVANCED).getBoolean(),
                               is(false));
                    assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.DEFAULT_VALUE).getBoolean(),
                               is(false));
                    assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.INDEX).getBoolean(), is(true));
                    assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.MASKED).getBoolean(),
                               is(false));
                    assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.REQUIRED).getBoolean(),
                               is(false));
                    assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.RUNTIME_TYPE).getString(),
                               is("boolean"));

                    NodeIterator itr = propNode.getNodes();
                    assertThat(itr.getSize(), is(2L));

                    { // display names
                        Node displayNameNode = propNode.getNode(ModelExtensionDefinitionLexicon.JcrId.Property.DISPLAY_NAME);
                        assertNotNull(displayNameNode);
                        assertThat(displayNameNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.LOCALE).getString(),
                                   is("en"));
                        assertThat(displayNameNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.TRANSLATION).getString(),
                                   is("Deterministic"));
                    }

                    { // descriptions
                        Node descriptionNode = propNode.getNode(ModelExtensionDefinitionLexicon.JcrId.DESCRIPTION);
                        assertNotNull(descriptionNode);
                        assertThat(descriptionNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.LOCALE).getString(),
                                   is("en"));
                        assertThat(descriptionNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.TRANSLATION).getString(),
                                   is("Specifies that the source function will always returns the same result for a specific input value"));
                    }
                }
            } else if (!foundTable
                       && "org.teiid.designer.metamodels.relational.impl.BaseTableImpl".equals(metaclassNode.getName())) {
                foundTable = true;

                { // property definitions
                    final NodeIterator propDefItr = metaclassNode.getNodes();
                    assertThat(propDefItr.getSize(), is(1L));

                    { // native-query
                        final Node propNode = metaclassNode.getNode("{http://www.teiid.org/ext/relational/2012/2}native-query");
                        assertNotNull(propNode);
                        assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.ADVANCED).getBoolean(),
                                   is(false));
                        assertThat(propNode.hasProperty(ModelExtensionDefinitionLexicon.JcrId.Property.DEFAULT_VALUE), is(false));
                        assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.INDEX).getBoolean(),
                                   is(true));
                        assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.MASKED).getBoolean(),
                                   is(false));
                        assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.REQUIRED).getBoolean(),
                                   is(false));
                        assertThat(propNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.Property.RUNTIME_TYPE).getString(),
                                   is("string"));

                        NodeIterator itr = propNode.getNodes();
                        assertThat(itr.getSize(), is(1L));

                        { // display names
                            Node displayNameNode = propNode.getNode(ModelExtensionDefinitionLexicon.JcrId.Property.DISPLAY_NAME);
                            assertNotNull(displayNameNode);
                            assertThat(displayNameNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.LOCALE).getString(),
                                       is("en"));
                            assertThat(displayNameNode.getProperty(ModelExtensionDefinitionLexicon.JcrId.TRANSLATION).getString(),
                                       is("Native Query"));
                        }
                    }
                }
            } else {
                fail();
            }
        }
    }

    @Test
    public void shouldQueryMedRelatedNodes() throws Exception {
        createNodeWithContentFromFile("ModelWithTwoMeds.xmi", "model/modelExtensionDefinition/ModelWithTwoMeds.xmi");
        final Node outputNode = getOutputNode(this.rootNode, "models/ModelWithTwoMeds.xmi");
        assertNotNull(outputNode);

        final QueryManager qm = outputNode.getSession().getWorkspace().getQueryManager();

        { // MED salesforce:baseTable mixin
            final Query query = qm.createQuery("SELECT * from [salesforce:baseTable]", JcrRepository.QueryLanguage.JCR_SQL2);
            final QueryResult result = query.execute();
            final RowIterator itr = result.getRows();
            assertThat(itr.getSize(), is(2L));
        }

        { // MED salesforce:baseTable mixin only one of the 2 tables have "Supports Query" set to "true"
            final Query query = qm.createQuery("SELECT * from [salesforce:baseTable] where [salesforce:Supports Query] = 'true'",
                                               JcrRepository.QueryLanguage.JCR_SQL2);
            final QueryResult result = query.execute();
            final RowIterator itr = result.getRows();
            assertThat(itr.getSize(), is(1L));
        }

        { // MED salesforce:column mixin
            final Query query = qm.createQuery("SELECT * from [salesforce:column]", JcrRepository.QueryLanguage.JCR_SQL2);
            final QueryResult result = query.execute();
            final RowIterator itr = result.getRows();
            assertThat(itr.getSize(), is(14L));
        }

        { // MED relational:baseTable mixin (need to get prefix as it has been mapped)
            final String nsPrefix = outputNode.getSession().getNamespacePrefix("http://www.teiid.org/ext/relational/2012/2");
            final Query query = qm.createQuery("SELECT * from [" + nsPrefix + ":baseTable]", JcrRepository.QueryLanguage.JCR_SQL2);
            final QueryResult result = query.execute();
            final RowIterator itr = result.getRows();
            assertThat(itr.getSize(), is(2L));
        }
    }
    
    @Test
    @FixFor( "MODE-2476" )
    public void shouldSequenceSuccessiveModels() throws Exception {
        createNodeWithContentFromFile("first.xmi", "model/BQT1.xmi");
        assertNotNull(getOutputNode(this.rootNode, "models/first.xmi"));

        createNodeWithContentFromFile("second.xmi", "model/Source.xmi");
        assertNotNull(getOutputNode(this.rootNode, "models/second.xmi"));
        
        createNodeWithContentFromFile("third.xmi", "model/BQT1.xmi");
        assertNotNull(getOutputNode(this.rootNode, "models/third.xmi"));
    }
}
