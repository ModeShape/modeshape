package org.modeshape.jboss.subsystem;

import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
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

    @Test(expected = XMLStreamException.class)
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
    public void testAnonymousRolesConfiguration() throws Exception {
        standardSubsystemTest("anonymous");
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

    private KernelServices initKernel( String subsystemXml ) throws Exception {
        return super.createKernelServicesBuilder(AdditionalInitialization.MANAGEMENT).setSubsystemXml(subsystemXml).build();
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
