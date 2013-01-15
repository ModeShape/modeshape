package org.modeshape.cmis;

import com.googlecode.sardine.DavResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.modeshape.common.util.StringUtil;
import com.googlecode.sardine.Sardine;
import com.googlecode.sardine.SardineFactory;
import com.googlecode.sardine.util.SardineException;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.junit.Before;
import org.w3c.dom.Element;

/**
 * Unit test for CMIS bridge
 * 
 * @author kulikov
 */
public class ModeShapeCmisClientTest  {

    private Session session;

    @Before
    public void setUp() {
        // default factory implementation
        SessionFactoryImpl factory = SessionFactoryImpl.newInstance();
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        //parameter.put(SessionParameter.USER, user);
        //parameter.put(SessionParameter.PASSWORD, passw);

        // connection settings
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, "http://localhost:8090/modeshape-cmis/services/ACLService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, "http://localhost:8090/modeshape-cmis/services/DiscoveryService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, "http://localhost:8090/modeshape-cmis/services/MultiFilingService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, "http://localhost:8090/modeshape-cmis/services/NavigationService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, "http://localhost:8090/modeshape-cmis/services/ObjectService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, "http://localhost:8090/modeshape-cmis/services/PolicyService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, "http://localhost:8090/modeshape-cmis/services/RelationshipService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, "http://localhost:8090/modeshape-cmis/services/RepositoryService?wsdl");
        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, "http://localhost:8090/modeshape-cmis/services/VersioningService?wsdl");

        parameter.put(SessionParameter.REPOSITORY_ID, "cmis_repo:default");

        // create session
        session =  factory.createSession(parameter, null, new StandardAuthenticationProvider() {

            @Override
            public Element getSOAPHeaders(Object portObject) {
                //Place headers here
                return super.getSOAPHeaders(portObject);
            }
         ;
        }, null);
    }

    @Test
    public void shouldAccessRootFolder() throws Exception {
        Folder root = session.getRootFolder();
        System.out.println("Root: " + root);
    }


}
