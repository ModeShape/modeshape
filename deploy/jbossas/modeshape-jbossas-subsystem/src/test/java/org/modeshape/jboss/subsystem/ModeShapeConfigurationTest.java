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

package org.modeshape.jboss.subsystem;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

@SuppressWarnings( "nls" )
public class ModeShapeConfigurationTest extends AbstractSubsystemTest {

    public ModeShapeConfigurationTest() {
        super(ModeShapeExtension.SUBSYSTEM_NAME, new ModeShapeExtension());
    }

    @Test
    public void testDescribeHandler() throws Exception {
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-sample-config.xml"));
        KernelServices servicesA = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the describe operations from the first controller
        ModelNode modelA = servicesA.readWholeModel();
        String marshalled = servicesA.getPersistedSubsystemXml();
        assertNotNull(modelA);
        assertNotNull(marshalled);

        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR)
                  .set(PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModeShapeExtension.SUBSYSTEM_NAME))
                                  .toModelNode());
        List<ModelNode> operations = super.checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();

        // Install the describe options from the first controller into a second controller
        KernelServices servicesB = super.installInController(AdditionalInitialization.MANAGEMENT, operations);
        ModelNode modelB = servicesB.readWholeModel();
        assertNotNull(modelB);

        // Make sure the models from the two controllers are identical
        // super.compare(modelA, modelB);
    }

    @Test
    public void testMinimalConfigurationWithOneMinimalRepository() throws Exception {
        String subsystemXml = "<subsystem xmlns=\"urn:jboss:domain:modeshape:1.0\">\n"
                              + "    <repository name=\"repo1\" />\n</subsystem>";
        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);
        services.readWholeModel();
    }

    @Test
    public void testOutputPersistance() throws Exception {
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-sample-config.xml"));

        String json = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-sample-config.json"));
        ModelNode testModel = filterValues(ModelNode.fromJSONString(json));
        String triggered = outputModel(testModel);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the persisted xml from the controller
        ModelNode model = services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();

        Assert.assertEquals(json, model.toJSONString(false));
        Assert.assertEquals(triggered, marshalled);
        Assert.assertEquals(normalizeXML(triggered), normalizeXML(marshalled));
    }

    @Test
    public void testOutputPersistanceOfRelativelyThoroughConfiguration() throws Exception {
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-full-config.xml"));

        String json = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-full-config.json"));
        ModelNode testModel = filterValues(ModelNode.fromJSONString(json));
        String triggered = outputModel(testModel);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the persisted xml from the controller
        ModelNode model = services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();

        Assert.assertEquals(json, model.toJSONString(false));
        compare(testModel, model);
        Assert.assertEquals(normalizeXML(triggered), normalizeXML(marshalled));
        // The input XML contains some default values, and the marshalled value doesn't contain the defaults;
        // therefore we cannot compare them directly (though they are equivalent) ...
        // Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithLocalFileIndexStorage() throws Exception {
        roundTrip("modeshape-local-file-index-storage.xml", "modeshape-local-file-index-storage.json");
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithCacheIndexStorage() throws Exception {
        roundTrip("modeshape-cache-index-storage.xml", "modeshape-cache-index-storage.json");
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithFileBinaryStorage() throws Exception {
        roundTrip("modeshape-file-binary-storage.xml", "modeshape-file-binary-storage.json");
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithCacheBinaryStorage() throws Exception {
        roundTrip("modeshape-cache-binary-storage.xml", "modeshape-cache-binary-storage.json");
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithClustering() throws Exception {
        roundTrip("modeshape-clustered-config.xml", "modeshape-clustered-config.json");
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithMinimalRepository() throws Exception {
        roundTrip("modeshape-minimal-config.xml", "modeshape-minimal-config.json");
    }

    protected void roundTrip( String filenameOfInputXmlConfig,
                              String filenameOfExpectedJson ) throws Exception {
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/" + filenameOfInputXmlConfig));

        String json = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/" + filenameOfExpectedJson));
        ModelNode testModel = filterValues(ModelNode.fromJSONString(json));
        String triggered = outputModel(testModel);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the persisted xml from the controller
        ModelNode model = services.readWholeModel();
        String marshalled = services.getPersistedSubsystemXml();

        Assert.assertEquals(json, model.toJSONString(false));
        compare(testModel, model);
        Assert.assertEquals(normalizeXML(triggered), normalizeXML(marshalled));
        // The input XML contains some default values, and the marshalled value doesn't contain the defaults;
        // therefore we cannot compare them directly (though they are equivalent) ...
        // Assert.assertEquals(normalizeXML(subsystemXml), normalizeXML(marshalled));
    }

    @Test
    public void testSchema() throws Exception {
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-sample-config.xml"));
        validate(subsystemXml);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the persisted xml from the controller
        /*ModelNode model =*/services.readWholeModel();
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
                if (!exception.getMessage().contains("cvc-enumeration-valid") && !exception.getMessage().contains("cvc-type")) {
                    fail(exception.getMessage());
                }
            }
        });

        validator.validate(source);
    }

    @Test
    public void testSubSystemDescription() throws IOException {
        ModelNode desc = ModeShapeSubsystemProviders.SUBSYSTEM.getModelDescription(Locale.getDefault());
        assertEquals(ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-subsystem-description.json")),
                     desc.toString());
    }

    //
    // @Test
    // public void testtransportDescription() throws IOException {
    // ModelNode node = new ModelNode();
    // TransportAdd.describeTransport(node, ATTRIBUTES, IntegrationPlugin.getResourceBundle(null));
    // assertEquals(ObjectConverterUtil.convertToString(new FileReader("src/test/resources/teiid-transport-config.txt")),
    // node.toString());
    // }

    @Test
    public void testParseSubsystem() throws Exception {
        // Parse the subsystem xml into operations
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-sample-config.xml"));
        List<ModelNode> operations = super.parse(subsystemXml);

        // /Check that we have the expected number of operations
        Assert.assertEquals(7, operations.size());

        // Check that each operation has the correct content
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(ModeShapeExtension.SUBSYSTEM_NAME, element.getValue());
    }

    // @Test
    // public void testQueryOperations() throws Exception {
    // KernelServices services = buildSubsystem();
    //
    // PathAddress addr = PathAddress.pathAddress(
    // PathElement.pathElement(SUBSYSTEM, ModeShapeExtension.MODESHAPE_SUBSYSTEM));
    // ModelNode addOp = new ModelNode();
    // addOp.get(OP).set("read-operation-names");
    // addOp.get(OP_ADDR).set(addr.toModelNode());
    //
    // ModelNode result = services.executeOperation(addOp);
    // Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    //
    // List<String> opNames = getList(result);
    // assertEquals(38, opNames.size());
    // String[] ops = { "add", "add-anyauthenticated-role", "add-data-role",
    // "assign-datasource", "cache-statistics", "cache-types",
    // "cancel-request", "change-vdb-connection-type", "clear-cache",
    // "describe", "execute-query", "get-translator", "get-vdb",
    // "list-requests", "list-sessions", "list-transactions",
    // "list-translators", "list-vdbs", "long-running-queries",
    // "mark-datasource-available",
    // "merge-vdbs", "read-attribute", "read-children-names",
    // "read-children-resources", "read-children-types",
    // "read-operation-description", "read-operation-names", "read-rar-description",
    // "read-resource", "read-resource-description",
    // "remove-anyauthenticated-role", "remove-data-role",
    // "requests-per-session", "requests-per-vdb",
    // "terminate-session", "terminate-transaction",
    // "workerpool-statistics", "write-attribute" };
    // assertEquals(Arrays.asList(ops), opNames);
    // }

    @Ignore
    @Test
    public void testAddRemoveRepository() throws Exception {
        KernelServices services = buildSubsystem();

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

    private KernelServices buildSubsystem() throws IOException, FileNotFoundException, Exception {
        String subsystemXml = ObjectConverterUtil.convertToString(new FileReader("src/test/resources/modeshape-sample-config.xml"));

        KernelServices services = super.installInController(subsystemXml);
        return services;
    }

    private static List<String> getList( ModelNode operationResult ) {
        if (!operationResult.hasDefined("result")) return Collections.emptyList();

        List<ModelNode> nodeList = operationResult.get("result").asList();
        if (nodeList.isEmpty()) return Collections.emptyList();

        List<String> list = new ArrayList<String>(nodeList.size());
        for (ModelNode node : nodeList) {
            list.add(node.asString());
        }
        return list;
    }

    // private ModelNode buildProperty(String name, String value) {
    // ModelNode node = new ModelNode();
    // node.get("property-name").set(name);
    // node.get("property-value").set(value);
    // return node;
    // }

    // @Test
    // public void testConnector() throws Exception {
    // KernelServices services = buildSubsystem();
    //
    // PathAddress addr = PathAddress.pathAddress(PathElement.pathElement(SUBSYSTEM, ModeShapeExtension.TEIID_SUBSYSTEM));
    //
    // ModelNode addOp = new ModelNode();
    // addOp.get(OP).set("add");
    // addOp.get(OP_ADDR).set(addr.toModelNode().add("translator", "oracle"));
    // ModelNode result = services.executeOperation(addOp);
    // Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    //
    // ModelNode read = new ModelNode();
    // read.get(OP).set("read-children-names");
    // read.get(OP_ADDR).set(addr.toModelNode());
    // read.get(CHILD_TYPE).set("translator");
    //
    // result = services.executeOperation(read);
    // Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    //
    // List<String> translators = Util.getList(result);
    // Assert.assertTrue(translators.contains("oracle"));
    //
    // ModelNode resourceRead = new ModelNode();
    // resourceRead.get(OP).set("read-resource");
    // resourceRead.get(OP_ADDR).set(addr.toModelNode());
    // resourceRead.get("translator").set("oracle");
    //
    // result = services.executeOperation(resourceRead);
    // Assert.assertEquals(SUCCESS, result.get(OUTCOME).asString());
    //
    // ModelNode oracleNode = result.get("result");
    //
    // ModelNode oracle = new ModelNode();
    // oracle.get("translator-name").set("oracle");
    // oracle.get("description").set("A translator for Oracle 9i Database or later");
    // oracle.get("children",
    // "properties").add(buildProperty("execution-factory-class","org.teiid.translator.jdbc.oracle.OracleExecutionFactory"));
    // oracle.get("children", "properties").add(buildProperty("TrimStrings","false"));
    // oracle.get("children", "properties").add(buildProperty("SupportedJoinCriteria","ANY"));
    // oracle.get("children", "properties").add(buildProperty("requiresCriteria","false"));
    // oracle.get("children", "properties").add(buildProperty("supportsOuterJoins","true"));
    // oracle.get("children", "properties").add(buildProperty("useCommentsInSourceQuery","false"));
    // oracle.get("children", "properties").add(buildProperty("useBindVariables","true"));
    // oracle.get("children", "properties").add(buildProperty("MaxPreparedInsertBatchSize","2048"));
    // oracle.get("children", "properties").add(buildProperty("supportsInnerJoins","true"));
    // oracle.get("children", "properties").add(buildProperty("MaxInCriteriaSize","1000"));
    // oracle.get("children", "properties").add(buildProperty("supportsSelectDistinct","true"));
    // oracle.get("children", "properties").add(buildProperty("supportsOrderBy","true"));
    // oracle.get("children", "properties").add(buildProperty("supportsFullOuterJoins","true"));
    // oracle.get("children", "properties").add(buildProperty("Immutable","false"));
    // oracle.get("children", "properties").add(buildProperty("MaxDependentInPredicates","50"));
    //
    // super.compare(oracleNode, oracle);
    // }

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
