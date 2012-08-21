package org.modeshape.jboss.subsystem;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.CHILD_TYPE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OUTCOME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUCCESS;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import junit.framework.Assert;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
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
        if ("minimal".equals(configId)) {
            return "<subsystem xmlns=\"urn:jboss:domain:modeshape:1.0\">\n" + " <repository name=\"repo1\" />\n</subsystem>";
        }
        return getSubsystemXml();
    }

    @Test
    public void testMinimalConfigurationWithOneMinimalRepository() throws Exception {
        standardSubsystemTest("minimal");
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithLocalFileIndexStorage() throws Exception {
        parse(readResource("modeshape-local-file-index-storage.xml"));
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithCacheIndexStorage() throws Exception {
        parse(readResource("modeshape-cache-index-storage.xml"));
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithFileBinaryStorage() throws Exception {
        parse(readResource("modeshape-file-binary-storage.xml"));
    }

    @Test
    public void testOutputPersistanceOfConfigurationWithCacheBinaryStorage() throws Exception {
        parse(readResource("modeshape-cache-binary-storage.xml"));
    }

    /* // todo replace with dmr format not json
    @Test
    public void testOutputPersistance() throws Exception {
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
    public void testOutputPersistanceOfRelativelyThoroughConfiguration() throws Exception {
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
        String subsystemXml = readResource(filenameOfInputXmlConfig);

        String json = readResource(filenameOfExpectedJson);
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
        }*/

    @Test
    public void testSchema() throws Exception {
        String subsystemXml = readResource("modeshape-sample-config.xml");
        validate(subsystemXml);

        KernelServices services = super.installInController(AdditionalInitialization.MANAGEMENT, subsystemXml);

        // Get the model and the persisted xml from the controller
        /*ModelNode model =*/
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
        String subsystemXml = readResource("modeshape-sample-config.xml");

        KernelServices services = super.installInController(subsystemXml);
        return services;
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
