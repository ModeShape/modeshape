package org.modeshape.cmis;

import java.util.HashMap;
import java.util.Map;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for CMIS bridge
 * 
 * @author kulikov
 */
public class ModeShapeCmisClientTest {

    private Session session;

    @Before
    public void setUp() {
        // default factory implementation
        SessionFactoryImpl factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        parameter.put(SessionParameter.USER, "dnauser");
        parameter.put(SessionParameter.PASSWORD, "password");

        // connection settings
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, serviceUrl("ACLService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, serviceUrl("/DiscoveryService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, serviceUrl("MultiFilingService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, serviceUrl("NavigationService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, serviceUrl("ObjectService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, serviceUrl("/PolicyService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, serviceUrl("RelationshipService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, serviceUrl("RepositoryService?wsdl"));
        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, serviceUrl("VersioningService?wsdl"));

        parameter.put(SessionParameter.REPOSITORY_ID, "cmis_repo:default");

        // create session
        session = factory.createSession(parameter, null, new StandardAuthenticationProvider(), null);
    }

    @Test
    public void shouldAccessRootFolder() throws Exception {
        Folder root = session.getRootFolder();
        System.out.println("Root: " + root);
    }

    private String serviceUrl( String serviceMethod ) {
        return "http://localhost:8090/modeshape-cmis/services/" + serviceMethod;
    }
}
