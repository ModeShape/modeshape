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

import static org.junit.Assert.fail;
import static org.modeshape.jboss.subsystem.ModeShapeExtension.SUBSYSTEM_NAME;
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
import org.junit.Test;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Unit test for the ModeShape AS7 subsystem.
 */
@SuppressWarnings( "nls" )
public class ModeShapeConfigurationTest extends AbstractSubsystemBaseTest {

    /**
     * An initialization mode which tells the base test class that the container should be started/simulated in "normal" mode.
     * This is required in order to make sure that all the ModeShape service have been started.
     */
    private static final AdditionalInitialization NORMAL_INITIALIZATION_MODE = new AdditionalInitialization();

    public ModeShapeConfigurationTest() {
        super(SUBSYSTEM_NAME, new ModeShapeExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("modeshape-sample-config.xml");
    }

    @Override
    protected AdditionalInitialization createAdditionalInitialization() {
        return NORMAL_INITIALIZATION_MODE;
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
    public void testFullConfiguration() throws Exception {
        standardSubsystemTest("full");
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
        return super.createKernelServicesBuilder(NORMAL_INITIALIZATION_MODE).setSubsystemXml(subsystemXml).build();
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
}
