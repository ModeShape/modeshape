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
package org.modeshape.connector;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.util.*;
import javax.jcr.RepositoryException;
import javax.jcr.NamespaceRegistry;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.runtime.ObjectIdImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.infinispan.schematic.document.Document;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.value.*;

/**
 *
 * @author kulikov
 */
public class CmisConnector extends Connector {
    private static final String CMIS_CND_PATH = "org/modeshape/connector/cmis.cnd";

    //path and id for the repository node
    private static final String REPOSITORY_ID = "repository";
    private static final String REPOSITORY_NODE_NAME = "repository";

    private Session session;
    private ValueFactories factories;

    //binding parameters
    private String aclService;
    private String discoveryService;
    private String multifilingService;
    private String navigationService;
    private String objectService;
    private String policyService;
    private String relationshipService;
    private String repositoryService;
    private String versioningService;

    //repository id
    private String repositoryId;

    //running mode
    private String mode;

    //path to node definition file
    private String nodeDefinitionFile = CMIS_CND_PATH;

    //maps for new nodes
    private HashMap<String, String> newNodes = new HashMap();

    public CmisConnector() {
        super();
    }

    @Override
    public void initialize(NamespaceRegistry registry,
            NodeTypeManager nodeTypeManager) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);
        this.factories = getContext().getValueFactories();
        
        // default factory implementation
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        //parameter.put(SessionParameter.USER, user);
        //parameter.put(SessionParameter.PASSWORD, passw);

        // connection settings
        parameter.put(SessionParameter.BINDING_TYPE, BindingType.WEBSERVICES.value());
        parameter.put(SessionParameter.WEBSERVICES_ACL_SERVICE, aclService);
        parameter.put(SessionParameter.WEBSERVICES_DISCOVERY_SERVICE, discoveryService);
        parameter.put(SessionParameter.WEBSERVICES_MULTIFILING_SERVICE, multifilingService);
        parameter.put(SessionParameter.WEBSERVICES_NAVIGATION_SERVICE, navigationService);
        parameter.put(SessionParameter.WEBSERVICES_OBJECT_SERVICE, objectService);
        parameter.put(SessionParameter.WEBSERVICES_POLICY_SERVICE, policyService);
        parameter.put(SessionParameter.WEBSERVICES_RELATIONSHIP_SERVICE, relationshipService);
        parameter.put(SessionParameter.WEBSERVICES_REPOSITORY_SERVICE, repositoryService);
        parameter.put(SessionParameter.WEBSERVICES_VERSIONING_SERVICE, versioningService);
        parameter.put(SessionParameter.REPOSITORY_ID, repositoryId);

        CmisFactory.Mode runMode = mode == null ?
                CmisFactory.Mode.SERVICE :
                CmisFactory.Mode.TEST;

        session = new CmisFactory().session(runMode, parameter);
        
        // Register the repository-specific node types ...
        InputStream cndStream = getClass().getClassLoader().getResourceAsStream(nodeDefinitionFile);
        nodeTypeManager.registerNodeTypes(cndStream, true);
    }

    @Override
    public Document getDocumentById(String id) {
        //search object by its identifier
        if (id.equals(REPOSITORY_ID)) {
            return repository();
        }

        CmisObject cmisObject = session.getObject(id);

        //object does not exist? return null
        if (cmisObject == null) {
            return null;
        }

        //converting CMIS object to JCR node
        switch (cmisObject.getBaseTypeId()) {
            case CMIS_FOLDER : {
                Document doc = folderNode(cmisObject);
                System.out.println(doc);
                return doc;
            }
            case CMIS_DOCUMENT : return documentNode(cmisObject);
        }

        return null;
    }

    @Override
    public String getDocumentId(String path) {
        System.out.println("----->Get document ID for path " + path);
        return session.getObjectByPath(path).getId();
    }

    @Override
    public boolean removeDocument(String id) {
        CmisObject object = session.getObject(id);
        if (object == null) {
            return false;
        }
        object.delete(true);
        return true;
    }

    @Override
    public boolean hasDocument(String id) {
        return session.getObject(id) != null;
    }

    @Override
    public void storeDocument(Document document) {
        String key = document.getString("key");
        String parent = document.getString("parent");

        System.out.println("Store document: " + parent+ "/" + key);
        Document properties = document.getDocument("properties").getDocument(JcrLexicon.Namespace.URI);
        String nodeType = properties.getDocument(JcrLexicon.PRIMARY_TYPE.getLocalName()).getString("$name");

        //nodeType = nodeType.replace("{" + CmisLexicon.Namespace.URI + "}", CmisLexicon.Namespace.PREFIX + ":");

        if (nodeType.equals(CmisLexicon.FOLDER.toString())) {
            cmisFolder(document);
        } else if (nodeType.equals(CmisLexicon.DOCUMENT.toString())) {
            System.out.println("Construction document");
            cmisDocument(document);
        }
        
        //String created = properties.getDocument(JcrLexicon.CREATED.getLocalName()).getString("$date");
        //String createdBy = properties.getString(JcrLexicon.CREATED_BY.getLocalName());

        //Folder folder = (Folder) session.getObject(parent);
        //folder.createFolder(null);

        System.out.println("------------- STORE DOCUMENT-------");
        System.out.println("Node type=" + nodeType);

//        Document props = properties.getDocument(JcrLexicon.Namespace.URI);

//        System.out.println(props);
        
//        System.out.println(props.getDocument(JcrLexicon.PRIMARY_TYPE.getLocalName()));
//        System.out.println(props.getDocument(JcrLexicon.PRIMARY_TYPE.getLocalName()).getString("$name"));

/*        Iterator<Field> fields = props.fields().iterator();
        while (fields.hasNext()) {
            Field f = fields.next();
            System.out.println(f.getName() + ":" + f.getValue());
        }
  */
    }

    @Override
    public void updateDocument(DocumentChanges delta) {
    }

    @Override
    public String newDocumentId(String parentId, Name newDocumentName) {
        return parentId + "/" + newDocumentName.getLocalName();
/*        session.crea
        newNodes.put(parentId, mode)
        CmisObject cmisObject = session.getObject(new ObjectIdImpl(parentId));
        cmisObject.
        System.out.println("<----- newDocumentId has been called: " + parentId + ": " + newDocumentName);
        session.createObjectId(parentId);
        String id = UUID.randomUUID().toString();
        System.out.println("<-- ID=" + id);
        return id;
        *
        */
    }

    /**
     * Translates CMIS folder object to JCR node
     * 
     * @param cmisObject CMIS folder object
     * @return JCR node document.
     */
    private Document folderNode(CmisObject cmisObject) {
        Folder folder = (Folder) cmisObject;
        DocumentWriter writer = newDocument(folder.getId());

        writer.setPrimaryType(CmisLexicon.FOLDER);
        writer.setParent(folder.getParentId());

        cmisProperties(folder, writer);
        cmisChildren((Folder)folder, writer);

        //append repository information to the root node
        if (folder.isRootFolder()) {
            writer.addChild(REPOSITORY_ID, REPOSITORY_NODE_NAME);
        }

        return writer.document();
    }

    private void cmisFolder(Document document) {
        String key = document.getString("key");
        String parentId = document.getString("parent");
System.out.println("KEY=" + key);
System.out.println("ParentId=" + parentId);
        Folder parent = (Folder) session.getObject(parentId);

        String name = key.substring(key.indexOf("/") + 1);
        String path = parent.getPath() + "/" + name;

        Map<String, Object> params = new HashMap();
        params.put(PropertyIds.NAME, name);
        params.put(PropertyIds.PATH, path);

        System.out.println("------ DOCUMENT-------");
        System.out.println(document);
        System.out.println("name=" + name);
        System.out.println("path=" + path);

        session.createFolder(params, new ObjectIdImpl(parent.getId()));
    }

    /**
     * Translates cmis document object to JCR node.
     * 
     * @param cmisObject cmis document node
     * @return JCR node document.
     */
    public Document documentNode(CmisObject cmisObject) {
        org.apache.chemistry.opencmis.client.api.Document doc =
                (org.apache.chemistry.opencmis.client.api.Document) cmisObject;
        DocumentWriter writer = newDocument(doc.getId());
        writer.setPrimaryType(CmisLexicon.DOCUMENT);

        cmisProperties(doc, writer);

        BinaryValue content = factories.getBinaryFactory().create(doc.getContentStream().getStream());
        writer.addProperty(JcrLexicon.DATA, content);

        return writer.document();
    }

    private void cmisDocument(Document document) {
        System.out.println("Creating DOCUMENT-------!!!!!!!!!!!!");
        String key = document.getString("key");
        String parentId = document.getString("parent");

        Folder parent = (Folder) session.getObject(parentId);

        String name = key.substring(key.indexOf("/") + 1);
        String path = parent.getPath() + "/" + name;

        Map<String, Object> params = new HashMap();
        params.put(PropertyIds.NAME, name);
        params.put(PropertyIds.PATH, path);

        System.out.println(document);
        System.out.println("name=" + name);
        System.out.println("path=" + path);

        session.createDocument(params, new ObjectIdImpl(parent.getId()), null, VersioningState.NONE);
    }

    /**
     * Converts CMIS object's properties to JCR node properties.
     *
     * @param object CMIS object
     * @param writer JCR node representation.
     */
    private void cmisProperties(CmisObject object, DocumentWriter writer) {
        //convert properties
        List<Property<?>> list = object.getProperties();
        for (Property property : list) {
            //obtain values
            List values = property.getValues();

            //convert CMIS values to JCR values
            switch (property.getType()) {
                case STRING :
                    writer.addProperty(property.getId(), asStrings(values));
                    break;
                case BOOLEAN :
                    writer.addProperty(property.getId(), asBooleans(values));
                    break;
                case DECIMAL :
                    writer.addProperty(property.getId(), asDecimals(values));
                    break;
                case INTEGER :
                    writer.addProperty(property.getId(), asIntegers(values));
                    break;
                case DATETIME :
                    writer.addProperty(property.getId(), asDateTime(values));
                    break;
                case URI :
                    writer.addProperty(property.getId(), asURI(values));
                    break;
                case ID :
                    writer.addProperty(property.getId(), asIDs(values));
                    break;
                case HTML :
                    writer.addProperty(property.getId(), asHTMLs(values));
                    break;
            }
        }
    }


    /**
     * Converts CMIS folder children to JCR node children
     * 
     * @param folder CMIS folder
     * @param writer JCR node representation
     */
    private void cmisChildren(Folder folder, DocumentWriter writer) {
        ItemIterable<CmisObject> it = folder.getChildren();
        for (CmisObject obj : it) {
            writer.addChild(obj.getId(), obj.getName());
        }
    }

    /**
     * Translates CMIS repository information into Node.
     * 
     * @return node document.
     */
    private Document repository() {
        RepositoryInfo info = session.getRepositoryInfo();
        DocumentWriter writer = newDocument(REPOSITORY_ID);

        writer.setPrimaryType(CmisLexicon.REPOSITORY);
        writer.setId(REPOSITORY_ID);

        //product name/vendor/version
        writer.addProperty(CmisLexicon.VENDOR_NAME, info.getVendorName());
        writer.addProperty(CmisLexicon.PRODUCT_NAME, info.getProductName());
        writer.addProperty(CmisLexicon.PRODUCT_VERSION, info.getProductVersion());
        
        return writer.document();
    }




    /**
     * Converts CMIS value of boolean type into JCR value of boolean type.
     * 
     * @param values CMIS values of boolean type
     * @return JCR values of boolean type
     */
    private Boolean[] asBooleans(List<Object> values) {
        ValueFactory<Boolean> factory = getContext().getValueFactories().getBooleanFactory();
        Boolean[] res = new Boolean[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of string type into JCR value of string type.
     *
     * @param values CMIS values of string type
     * @return JCR values of string type
     */
    private String[] asStrings(List<Object> values) {
        ValueFactory<String> factory = getContext().getValueFactories().getStringFactory();
        String[] res = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of integer type into JCR value of boolean type.
     *
     * @param values CMIS values of integer type
     * @return JCR values of integer type
     */
    private Long[] asIntegers(List<Object> values) {
        ValueFactory<Long> factory = getContext().getValueFactories().getLongFactory();
        Long[] res = new Long[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of decimal type into JCR value of boolean type.
     *
     * @param values CMIS values of decimal type
     * @return JCR values of decimal type
     */
    private BigDecimal[] asDecimals(List<Object> values) {
        ValueFactory<BigDecimal> factory = getContext().getValueFactories().getDecimalFactory();
        BigDecimal[] res = new BigDecimal[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of date/time type into JCR value of date/time type.
     *
     * @param values CMIS values of gregorian calendar type
     * @return JCR values of date/time type
     */
    private DateTime[] asDateTime(List<Object> values) {
        ValueFactory<DateTime> factory = getContext().getValueFactories().getDateFactory();
        DateTime[] res = new DateTime[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(((GregorianCalendar)values.get(i)).getTime());
        }
        return res;
    }

    /**
     * Converts CMIS value of URI type into JCR value of URI type.
     *
     * @param values CMIS values of URI type
     * @return JCR values of URI type
     */
    private URI[] asURI(List<Object> values) {
        ValueFactory<URI> factory = getContext().getValueFactories().getUriFactory();
        URI[] res = new URI[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(((GregorianCalendar)values.get(i)).getTime());
        }
        return res;
    }

    /**
     * Converts CMIS value of ID type into JCR value of String type.
     *
     * @param values CMIS values of Id type
     * @return JCR values of String type
     */
    private String[] asIDs(List<Object> values) {
        ValueFactory<String> factory = getContext().getValueFactories().getStringFactory();
        String[] res = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

    /**
     * Converts CMIS value of HTML type into JCR value of String type.
     *
     * @param values CMIS values of HTML type
     * @return JCR values of String type
     */
    private String[] asHTMLs(List<Object> values) {
        ValueFactory<String> factory = getContext().getValueFactories().getStringFactory();
        String[] res = new String[values.size()];
        for (int i = 0; i < res.length; i++) {
            res[i] = factory.create(values.get(i));
        }
        return res;
    }

}
