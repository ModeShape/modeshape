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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
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
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.infinispan.schematic.document.Binary;
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
        //find object
        String key = document.getString("key");
        CmisObject cmisObject = session.getObject(key);

        //no such object? just exit
        if (cmisObject == null) {
            return;
        }

        //pickup node properties
        Document props = document.getDocument("properties").getDocument(JcrLexicon.Namespace.URI);
        
        switch (cmisObject.getBaseTypeId()) {
            case CMIS_FOLDER :
                break;
            case CMIS_DOCUMENT:
                //store binary content
                ContentStream stream = getContentStream(document);
                if (stream != null) {
                    ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).setContentStream(stream, true);
                }
                break;
        }
        //set binary value and properties
    }

    @Override
    public void updateDocument(DocumentChanges delta) {
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public String newDocumentId(String parentId, Name name, Name primaryType) {
        HashMap<String, Object> params = new HashMap();

        //Creating CMIS folder
        if (primaryType.equals(CmisLexicon.FOLDER)) {
            Folder parent = (Folder) session.getObject(parentId);
            String path = parent.getPath() + "/" + name.getLocalName();

            //minimal set of parameters
            params.put(PropertyIds.OBJECT_TYPE_ID,  "cmis:folder");
            params.put(PropertyIds.NAME, name.getLocalName());
            params.put(PropertyIds.PATH, path);

            Folder folder = parent.createFolder(params);
            return folder.getId();
        }

        //Creating CMIS document
        if (primaryType.equals(CmisLexicon.DOCUMENT)) {
            Folder parent = (Folder) session.getObject(parentId);
            String path = parent.getPath() + "/" + name.getLocalName();

            //minimal set of parameters
            params.put(PropertyIds.OBJECT_TYPE_ID,  "cmis:document");
            params.put(PropertyIds.NAME, name.getLocalName());
            params.put(PropertyIds.PATH, path);

            String id = parent.createDocument(params, null, VersioningState.NONE).getId();
            System.out.println("Created document with id = " + id);
            return id;
        }

        return null;
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

        //copy binary value
        if (doc.getContentStream() != null) {
            BinaryValue content = factories.getBinaryFactory().create(doc.getContentStream().getStream());
            writer.addProperty(CmisLexicon.DATA, content);
        }

        return writer.document();
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

    /**
     * Creates content stream using JCR node.
     *
     * @param document JCR node representation
     * @return CMIS content stream object
     */
    private ContentStream getContentStream(Document document) {
        //pickup node properties
        Document props = document.getDocument("properties").getDocument(JcrLexicon.Namespace.URI);

        //extract binary value and content
        Binary value = props.getBinary("data");

        if (value == null) {
            return null;
        }

        byte[] content = value.getBytes();

        //wrap with input stream
        ByteArrayInputStream bin = new ByteArrayInputStream(content);
        bin.reset();

        //create content stream
        ContentStreamImpl contentStream = new ContentStreamImpl("", BigInteger.valueOf(content.length), "", bin);

        return contentStream;
    }
}
