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
package org.modeshape.jboss.subsystem;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.modeshape.jboss.subsystem.ModeShapeExtension.SUBSYSTEM_NAME;
import static org.modeshape.jboss.subsystem.ModelKeys.REPOSITORY;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;
import org.junit.Assert;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Unit test for the ModeShape AS subsystem.
 */
@SuppressWarnings( "nls" )
public class ModeShapeConfigurationTest extends AbstractSubsystemBaseTest {
    
    public ModeShapeConfigurationTest() {
        super(SUBSYSTEM_NAME, new ModeShapeExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("modeshape-sample-config.xml");
    }

    @Override
    protected String getSubsystemXml( String configId ) throws IOException {
        if (configId == null) {
            return getSubsystemXml();
        }
        String config = "modeshape-" + configId + "-config.xml";
        if (ModeShapeConfigurationTest.class.getResource(config) != null) {
            return readResource(config); 
        }
        return readResource(configId);
    }

    @Test
    public void testMinimalConfiguration() throws Exception {
        standardSubsystemTest("minimal");
    }

    @Test
    public void testFullConfiguration() throws Exception {
        standardSubsystemTest("full");
    }

    @Test
    public void testConfigurationWithIndexStorage() throws Exception {
        standardSubsystemTest("modeshape-index-storage.xml", false);
    }

    @Test
    public void testConfigurationWithAllIndexTypes() throws Exception {
        // fix for MODE-2348
        standardSubsystemTest("modeshape-index-types.xml");
    }

    @Test
    public void testConfigurationWithFileBinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-file-binary-storage.xml", false);
    }  
    
    @Test
    public void testConfigurationWithTransientBinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-transient-binary-storage.xml");
    }   
    
    @Test
    public void testConfigurationWithDBBinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-db-binary-storage.xml");
    }  

    @Test
    public void testConfigurationWithCassandraBinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-cassandra-binary-storage.xml");
    }  
    
    @Test
    public void testConfigurationWithMongoBinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-mongo-binary-storage.xml");
    }

    @Test
    public void testConfigurationWithS3BinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-s3-binary-storage.xml");
    }

    @Test
    public void testConfigurationWithCustomBinaryStorage() throws Exception {
        standardSubsystemTest("modeshape-custom-binary-storage.xml");
    }

    @Test(expected = XMLStreamException.class)
    public void shouldValidateFileBinaryStoreAttributesAgainstSchema() throws Exception {
        standardSubsystemTest("modeshape-invalid-file-binary-storage.xml");
    }

    @Test
    public void testConfigurationWithCompositeBinaryStores() throws Exception {
        standardSubsystemTest("modeshape-composite-binary-storage-config.xml", false);
    }

    @Test( expected = XMLStreamException.class )
    public void shouldRejectInvalidCompositeBinaryStoreConfiguration() throws Exception {
        standardSubsystemTest("modeshape-invalid-composite-binary-storage.xml");
    }

    @Test
    public void testConfigurationWithWorkspaceInitialContent() throws Exception {
        standardSubsystemTest("modeshape-initial-content-config.xml", false);
    }
    
    @Test
    public void testConfigurationWithClustering() throws Exception {
        standardSubsystemTest("modeshape-clustered-config.xml");
    }

    @Test
    public void testConfigurationWithNodeTypes() throws Exception {
        standardSubsystemTest("modeshape-node-types-config.xml");
    }

    @Test
    public void testConfigurationWithCustomAuthenticators() throws Exception {
        standardSubsystemTest("modeshape-custom-authenticators-config.xml", false);
    }

    @Test
    public void testConfigurationWithWorkspacesCacheContainer() throws Exception {
        standardSubsystemTest("modeshape-workspaces-cache-config.xml", false);
    }

    @Test
    public void testConfigurationWithExternalSources() throws Exception {
        standardSubsystemTest("modeshape-federation-config.xml", false);
    }

    @Test
    public void testConfigurationWithGarbageCollectionSpecified() throws Exception {
        standardSubsystemTest("modeshape-garbage-collection.xml", false);
    }

    @Test
    public void testConfigurationWithWebapps() throws Exception {
        standardSubsystemTest("modeshape-webapp-config.xml", false);
    }

    @Test
    public void testConfigurationWithJournaling() throws Exception {
        standardSubsystemTest("modeshape-journaling.xml");
    }
    
    @Test
    public void testConfigurationWithOptimization() throws Exception {
        standardSubsystemTest("modeshape-optimiziation-config.xml", false);
    }
    
    @Test
    public void testConfigurationWithMimeTypeDetection() throws Exception {
        standardSubsystemTest("modeshape-mime-type-detection.xml");
    } 
    
    @Test
    public void testConfigurationWithReindexing() throws Exception {
        standardSubsystemTest("modeshape-reindexing.xml");
    }
    
    @Test
    public void testConfigurationWithPersistence() throws Exception {
        standardSubsystemTest("modeshape-persistence-config.xml");
    }  
    
    @Test
    public void testConfigurationWithCustomDependencies() throws Exception {
        standardSubsystemTest("modeshape-repository-dependencies-config.xml");
    }

    @Test
    public void testSampleConfigurationModel() throws Exception {
        List<ModelNode> nodes = parse(readResource("modeshape-sample-config.xml"));
        assertEquals(7, nodes.size());

        ModelNode subsystem = nodes.get(0);
        assertNode(subsystem.get(OP_ADDR), new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME));

        ModelNode repo1 = nodes.get(1);
        assertNode(repo1.get(OP_ADDR), new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME), new KeyValue(REPOSITORY, "sample1"));
        assertEquals(repo1.get(ModelKeys.JNDI_NAME).asString(), "jcr/local/modeshape_repo1");

        ModelNode seq1 = nodes.get(2);
        assertNode(seq1.get(OP_ADDR),
                   new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME),
                   new KeyValue(REPOSITORY, "sample1"),
                   new KeyValue("sequencer", "modeshape-sequencer-ddl"));
        assertEquals(seq1.get(ModelKeys.CLASSNAME).asString(), "ddl");
        assertEquals(seq1.get(ModelKeys.PATH_EXPRESSIONS).asList().get(0).asString(), "//a/b");

        ModelNode seq2 = nodes.get(3);
        assertNode(seq2.get(OP_ADDR),
                   new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME),
                   new KeyValue(REPOSITORY, "sample1"),
                   new KeyValue("sequencer", "modeshape-sequencer-java"));
        assertEquals(seq2.get(ModelKeys.CLASSNAME).asString(), "java");
        assertEquals(seq2.get(ModelKeys.PATH_EXPRESSIONS).asList().get(0).asString(), "//a/b");
        assertNode(seq2.get(ModelKeys.PROPERTIES), new KeyValue("extra1", "value1"), new KeyValue("extra2", "2"));

        ModelNode repo2 = nodes.get(4);
        assertNode(repo2.get(OP_ADDR), new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME), new KeyValue(REPOSITORY, "sample2"));

        ModelNode seq3 = nodes.get(5);
        assertNode(seq3.get(OP_ADDR),
                   new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME),
                   new KeyValue(REPOSITORY, "sample2"),
                   new KeyValue("sequencer", "modeshape-sequencer-ddl"));
        assertEquals(seq3.get(ModelKeys.CLASSNAME).asString(), "ddl");
        assertEquals(seq3.get(ModelKeys.PATH_EXPRESSIONS).asList().get(0).asString(), "//a/b/");
        assertEquals(seq3.get(ModelKeys.PATH_EXPRESSIONS).asList().get(1).asString(), "//a/b2/");

        ModelNode seq4 = nodes.get(6);
        assertNode(seq4.get(OP_ADDR),
                   new KeyValue(SUBSYSTEM, SUBSYSTEM_NAME),
                   new KeyValue(REPOSITORY, "sample2"),
                   new KeyValue("sequencer", "modeshape-sequencer-java"));
        assertEquals(seq4.get(ModelKeys.CLASSNAME).asString(), "java");
        assertEquals(seq4.get(ModelKeys.PATH_EXPRESSIONS).asList().get(0).asString(), "//a/b");
    }

    @Test
    public void testAddRemoveRepository() throws Exception {
        String subsystemXml = readResource("modeshape-sample-config.xml");
        validate(subsystemXml);
        KernelServices services = initKernel(subsystemXml);

        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, SUBSYSTEM_NAME));

        // look at current query engines make sure there are only two from configuration.
        ModelNode read = new ModelNode();
        read.get(OP).set("read-children-names");
        read.get(OP_ADDR).set(addr.toModelNode());
        read.get(CHILD_TYPE).set("repository");

        ModelNode result = services.executeOperation(read);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        List<String> opNames = getList(result);
        assertEquals(2, opNames.size());
        String[] ops = {"sample1", "sample2"};
        assertEquals(Arrays.asList(ops), opNames);

        // add repository
        ModelNode addOp = new ModelNode();
        addOp.get(OP).set("add");
        addOp.get(OP_ADDR).set(addr.toModelNode().add("repository", "myrepository")); //$NON-NLS-1$;
        addOp.get("jndi-name").set("jcr:local:myrepository");

        result = services.executeOperation(addOp);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());

        result = services.executeOperation(read);
        Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
        opNames = getList(result);
        assertEquals(3, opNames.size());
        String[] ops2 = {"myrepository", "sample1", "sample2"};
        assertEquals(Arrays.asList(ops2), opNames);
    }

    private KernelServices initKernel( String subsystemXml ) throws Exception {
        return super.createKernelServicesBuilder(super.createAdditionalInitialization()).setSubsystemXml(subsystemXml).build();
    }

    @Test
    public void testAnonymousRolesConfiguration() throws Exception {
        standardSubsystemTest("anonymous");
    }

    @Test
    public void testSequencingConfiguration() throws Exception {
        standardSubsystemTest("sequencing");
    }
   
    @Test
    public void testTextExtractionConfiguration() throws Exception {
        standardSubsystemTest("text-extraction");
    }    

    @Test
    public void testSchema() throws Exception {
        String subsystemXml = readResource("modeshape-sample-config.xml");
        validate(subsystemXml);
        KernelServices services = initKernel(subsystemXml);

        // Get the model and the persisted xml from the controller
        services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();
        validate(marshalled);
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/modeshape_3_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[]{"modeshape-full-config.xml"};
    }

    private void validate( String marshalled ) throws SAXException, IOException {
        URL xsdURL = Thread.currentThread().getContextClassLoader().getResource("schema/modeshape_3_0.xsd");
        // System.out.println(marshalled);

        SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
        Schema schema = factory.newSchema(xsdURL);

        Validator validator = schema.newValidator();
        Source source = new StreamSource(new ByteArrayInputStream(marshalled.getBytes()));
        validator.setErrorHandler(new ErrorHandler() {

            @Override
            public void warning( SAXParseException exception ) {
                fail(exception.getMessage());
            }

            @Override
            public void fatalError( SAXParseException exception ) {
                fail(exception.getMessage());
            }

            @Override
            public void error( SAXParseException exception ) {
                if (exception.getMessage().contains("cvc-enumeration-valid")) return;
                if (exception.getMessage().contains("cvc-type")) return;
                fail(exception.getMessage());
            }
        });

        validator.validate(source);
    }

    private static List<String> getList( ModelNode operationResult ) {
        if (!operationResult.hasDefined("result")) {
            return Collections.emptyList();
        }

        List<ModelNode> nodeList = operationResult.get("result").asList();
        if (nodeList.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> list = new ArrayList<String>(nodeList.size());
        for (ModelNode node : nodeList) {
            list.add(node.asString());
        }
        return list;
    }

    private void assertNode( ModelNode node,
                             KeyValue... values ) {
        assertTrue(values.length > 0);
        if (node.getType() == ModelType.LIST) {
            List<ModelNode> modelNodes = node.asList();
            Assert.assertEquals(values.length, modelNodes.size());
            for (int i = 0; i < modelNodes.size(); i++) {
                assertNode(modelNodes.get(i), values[i]);
            }
        } else {
            values[0].assertEquals(node);
        }
    }

    private class KeyValue {
        private String key;
        private Object value;
        private ModelType type;

        protected KeyValue( String key,
                            Object value,
                            ModelType type ) {
            this.key = key;
            this.value = value;
            this.type = type;
        }

        protected KeyValue( String key,
                            Object value ) {
            this(key, value, ModelType.STRING);
        }

        protected void assertEquals( ModelNode node ) {
            Object actualInstance = null;
            assertTrue(key + " not present on ModelNode", node.has(key));
            switch (type) {
                case BIG_DECIMAL: {
                    actualInstance = node.get(key).asBigDecimal();
                    break;
                }
                case STRING: {
                    actualInstance = node.get(key).asString();
                    break;
                }
                case LONG: {
                    actualInstance = node.get(key).asLong();
                    break;
                }
                case BOOLEAN: {
                    actualInstance = node.get(key).asBoolean();
                    break;
                }
                default: {
                    actualInstance = node.get(key).asString();
                }
            }
            Assert.assertEquals("Unexpected value in model node under " + key, value, actualInstance);
        }
    }
}
