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
import static org.junit.Assert.fail;
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
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@SuppressWarnings( "nls" )
public class ModeShapeConfigurationTest extends AbstractSubsystemBaseTest {

    public ModeShapeConfigurationTest() {
        super(ModeShapeExtension.SUBSYSTEM_NAME, new ModeShapeExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("modeshape-sample-config.xml");
    }

    @Override
    protected String getSubsystemXml( String configId ) throws IOException {
        return configId != null ? readResource("modeshape-" + configId + "-config.xml") : getSubsystemXml();
    }

    @Test
    public void testMinimalConfigurationWithOneMinimalRepository() throws Exception {
        standardSubsystemTest("minimal");
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithLocalFileIndexStorage() throws Exception {
        parse(readResource("modeshape-local-file-index-storage.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithFileBinaryStorage() throws Exception {
        parse(readResource("modeshape-file-binary-storage.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithCacheBinaryStorage() throws Exception {
        parse(readResource("modeshape-cache-binary-storage.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithCompositeBinaryStores() throws Exception {
        parse(readResource("modeshape-composite-binary-storage-config.xml"));
    }

    @Test( expected = XMLStreamException.class )
    public void shouldRejectInvalidCompositeBinaryStoreConfiguration() throws Exception {
        parse(readResource("modeshape-invalid-composite-binary-storage.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithWorkspaceInitialContent() throws Exception {
        parse(readResource("modeshape-initial-content-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithNodeTypes() throws Exception {
        parse(readResource("modeshape-node-types-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithCustomAuthenticators() throws Exception {
        parse(readResource("modeshape-custom-authenticators-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithWorkspacesCacheContainer() throws Exception {
        parse(readResource("modeshape-workspaces-cache-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithExternalSources() throws Exception {
        parse(readResource("modeshape-federation-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithDisabledQueries() throws Exception {
        parse(readResource("modeshape-disabled-queries.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithGarbageCollectionSpecified() throws Exception {
        parse(readResource("modeshape-garbage-collection.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithCustomIndexRebuildOptions() throws Exception {
        parse(readResource("modeshape-index-rebuilding-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithWebapps() throws Exception {
        parse(readResource("modeshape-webapp-config.xml"));
    }

    @Test
    public void testOutputPersistenceOfConfigurationWithJournaling() throws Exception {
        parse(readResource("modeshape-journaling.xml"));
    }

    /* // todo replace with dmr format not json
    @Test
    public void testOutputPersistence() throws Exception {
    String subsystemXml = readResource("modeshape-sample-config.xml");

    String json = readResource("modeshape-sample-config.json");
    ModelNode testModel = filterValues(ModelNode.fromJSONString(json));
    String triggered = outputModel(testModel);

    KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

    // Get the model and the persisted xml from the controller
    ModelNode model = services.readWholeModel();
    String marshalled = services.getPersistedSubsystemXml();

    compare(testModel, model);
    Assert.assertEquals(triggered, marshalled);
    Assert.assertEquals(normalizeXML(triggered), normalizeXML(marshalled));
    }
    */
    /*
    @Test
    public void testOutputPersistenceOfRelativelyThoroughConfiguration() throws Exception {
        String subsystemXml = readResource("modeshape-full-config.xml");

        String json = readResource("modeshape-full-config.json");
        ModelNode testModel = filterValues(ModelNode.fromJSONString(json));
        String triggered = outputModel(testModel);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the persisted xml from the controller
        ModelNode model = services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();

        compare(ModelNode.fromJSONString(json), model);
        compare(testModel, model);
        Assert.assertEquals(normalizeXML(triggered), normalizeXML(marshalled));
        // The input XML contains some default values, and the marshalled value doesn't contain the defaults;
        // therefore we cannot compare them directly (though they are equivalent) ...
        // Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));
    }
    */

    /*
        @Test
        public void testOutputPersistenceOfConfigurationWithLocalFileIndexStorage() throws Exception {
        roundTrip("modeshape-local-file-index-storage.xml", "modeshape-local-file-index-storage.json");
        }

        @Test
        public void testOutputPersistenceOfConfigurationWithFileBinaryStorage() throws Exception {
        roundTrip("modeshape-file-binary-storage.xml", "modeshape-file-binary-storage.json");
        }

        @Test
        public void testOutputPersistenceOfConfigurationWithCacheBinaryStorage() throws Exception {
        roundTrip("modeshape-cache-binary-storage.xml", "modeshape-cache-binary-storage.json");
        }

        @Test
        public void testOutputPersistenceOfConfigurationWithMinimalRepository() throws Exception {
        roundTrip("modeshape-minimal-config.xml", "modeshape-minimal-config.json");
        }
    */
    @Ignore
    @Test
    public void testOutputPersistenceOfConfigurationWithAuthenticators() throws Exception {
        roundTrip("modeshape-custom-authenticators-config.xml", "modeshape-custom-authenticators-config.json");
    }

    protected void roundTrip( String filenameOfInputXmlConfig,
                              String filenameOfExpectedJson ) throws Exception {
        String subsystemXml = readResource(filenameOfInputXmlConfig);

        String json = readResource(filenameOfExpectedJson);
        System.out.println("JSON: " + json);
        ModelNode testModel = filterValues(ModelNode.fromJSONString(json));
        String triggered = outputModel(testModel);
        System.out.println("Triggered: " + triggered);

        KernelServices services = initKernel(subsystemXml);

        // Get the model and the persisted xml from the controller
        ModelNode model = services.readWholeModel();
        System.out.println("Original Model: " + testModel);
        System.out.println("Re-read  Model: " + model);
        String marshalled = services.getPersistedSubsystemXml();

        System.out.println("Marshalled: " + marshalled);

        compare(ModelNode.fromJSONString(json), model);
        compare(testModel, model);
        Assert.assertEquals(normalizeXML(triggered), normalizeXML(marshalled));
        // The input XML contains some default values, and the marshalled value doesn't contain the defaults;
        // therefore we cannot compare them directly (though they are equivalent) ...
        // Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));
    }

    private KernelServices initKernel( String subsystemXml ) throws Exception {
        return super.createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
    }

    @SuppressWarnings( "deprecation" )
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

    private void validate( String marshalled ) throws SAXException, IOException {
        URL xsdURL = Thread.currentThread().getContextClassLoader().getResource("schema/modeshape_1_0.xsd");
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

    @Ignore
    @Test
    public void testAddRemoveRepository() throws Exception {
        KernelServices services = super.installInController("modeshape-sample-config.xml");

        PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME));

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

    /**
     * When JSON strings are parsed into ModelNode structures, any integer values are parsed into org.jboss.dmr.BigIntegerValue
     * instances rather than org.jboss.dmr.IntegerValue instances. This method converts all BigIntegerValue instances into a
     * IntegerValue instance.
     * 
     * @param node the model
     * @return the updated model
     */
    protected ModelNode filterValues( ModelNode node ) {
        ModelNode result = new ModelNode();
        switch (node.getType()) {
            case OBJECT:
                for (String key : node.keys()) {
                    ModelNode value = node.get(key);
                    result.get(key).set(filterValues(value));
                }
                break;
            case LIST:
                for (ModelNode value : node.asList()) {
                    result.add(filterValues(value));
                }
                break;
            case PROPERTY:
                Property prop = node.asProperty();
                ModelNode propValue = prop.getValue();
                ModelNode filteredValue = filterValues(propValue);
                if (propValue != filteredValue) {
                    Property newProp = new Property(prop.getName(), filteredValue);
                    result.set(newProp);
                } else {
                    result = node;
                }
                break;
            case BIG_INTEGER:
                result.set(node.asBigInteger().intValue());
                break;
            default:
                result = node;
        }
        return result;
    }
}
