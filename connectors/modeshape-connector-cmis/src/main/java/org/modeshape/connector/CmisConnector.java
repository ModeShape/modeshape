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

import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Document.Field;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.federation.spi.Connector;
import org.modeshape.jcr.federation.spi.DocumentChanges;
import org.modeshape.jcr.federation.spi.DocumentChanges.PropertyChanges;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFactories;
import org.w3c.dom.Element;

import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.*;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.*;

/**
 * @author kulikov
 * @author Ivan Vasyliev
 */
public class CmisConnector extends Connector {

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
    private Properties properties;
    private Nodes nodes;

    private NamespaceRegistry registry;
    private Prefix prefixes = new Prefix();
    
    public CmisConnector() {
        super();
    }

    @Override
    public void initialize(NamespaceRegistry registry,
                           NodeTypeManager nodeTypeManager) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        this.registry = registry;
        this.factories = getContext().getValueFactories();
        
        properties = new Properties(getContext().getValueFactories());
        nodes = new Nodes();
        
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

        SessionFactoryImpl factory = SessionFactoryImpl.newInstance();
        session = factory.createSession(parameter, null,
                new StandardAuthenticationProvider() {
                    @Override
                    public Element getSOAPHeaders(Object portObject) {
                        //Place headers here
                        return super.getSOAPHeaders(portObject);
                    }
                }, null);

        registry.registerNamespace(CmisLexicon.Namespace.PREFIX, CmisLexicon.Namespace.URI);
        importTypes(session.getTypeDescendants(null, Integer.MAX_VALUE, true), nodeTypeManager, registry);
        registerRepositoryInfoType(nodeTypeManager);

        InputStream cndStream = getClass().getClassLoader().getResourceAsStream("cmis.cnd");
        nodeTypeManager.registerNodeTypes(cndStream, true);        
    }

    @Override
    public Document getDocumentById(String id) {
        //object id is a composite key which holds information about 
        //unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(id);
        
        //this action depends from object type                
        switch (objectId.getType()) {
            case REPOSITORY:
                //convert information about repository from 
                //cmis domain into jcr domain
                return cmisRepository();
            case CONTENT:
                //in the jcr domain content is represented by child node of 
                //the nt:file node while in cmis domain it is a property of 
                //the cmis:document object. This action searches original 
                //cmis:document and converts its content property into jcr node
                return cmisContent(objectId.getIdentifier());
            case OBJECT:
                //converts cmis folders and documents into jcr folders and files
                return cmisObject(objectId.getIdentifier());
            default:
                return null;
        }
    }

    @Override
    public String getDocumentId(String path) {
        //establish relation between path and object identifier
        return session.getObjectByPath(path).getId();
    }

    @Override
    public boolean removeDocument(String id) {
        //object id is a composite key which holds information about 
        //unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(id);
        
        //this action depends from object type        
        switch (objectId.getType()) {
            case REPOSITORY :
                //information about repository is ready only
                return false;
            case CONTENT :
                //in the jcr domain content is represented by child node of 
                //the nt:file node while in cmis domain it is a property of 
                //the cmis:document object. so to perform this operation we need 
                //to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();
                
                org.apache.chemistry.opencmis.client.api.Document doc =
                (org.apache.chemistry.opencmis.client.api.Document) 
                        session.getObject(cmisId);
                
                //object exists?
                if (doc == null) {
                    //object does not exist. propably was deleted by from cmis domain
                    //we don't know how to handle such case yet, thus TODO
                    return false;
                }
                
                //delete content stream
                doc.deleteContentStream();
                return true;
            case OBJECT:
                //these type points to either cmis:document or cmis:folder so
                //we can just delete it using original identifier defined in cmis domain.
                CmisObject object = session.getObject(objectId.getIdentifier());
                
                //check that object exist
                if (object == null) {
                    return false;
                }
                
                //just delete object
                object.delete(true);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean hasDocument(String id) {
        //object id is a composite key which holds information about 
        //unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(id);

        //this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY:
                //exist always 
                return true;
            case CONTENT:
                //in the jcr domain content is represented by child node of 
                //the nt:file node while in cmis domain it is a property of 
                //the cmis:document object. so to perform this operation we need 
                //to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();
                
                //now checking that this document exists
                return session.getObject(cmisId) != null;
            default:
                //here we checking cmis:folder and cmis:document
                return session.getObject(id) != null;
        }
    }

    @Override
    public void storeDocument(Document document) {
        //object id is a composite key which holds information about 
        //unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(document.getString("key"));
        
        //this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY :
                //repository information is ready only
                return;
            case CONTENT :
                //in the jcr domain content is represented by child node of 
                //the nt:file node while in cmis domain it is a property of 
                //the cmis:document object. so to perform this operation we need 
                //to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();
                
                //now let's get the reference to this object
                CmisObject cmisObject = session.getObject(cmisId);
                
                //object exists?
                if (cmisObject == null) {
                    //object does not exist. propably was deleted by from cmis domain
                    //we don't know how to handle such case yet, thus TODO
                    return;
                }
                
                //original object is here so converting binary value and
                //updating original cmis:document
                ContentStream stream = jcrBinaryContent(document);
                if (stream != null && cmisObject != null) {
                    ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).setContentStream(stream, true);
                }
                break;
            case OBJECT :
                //extract node properties from the document view
                Document jcrProperties = document.getDocument("properties");
                
                //check that we have jcr properties to store in the cmis repo
                if (jcrProperties == null) {
                    //nothing to store
                    return;
                }
                
                //if node has properties we need to pickup cmis object from
                //cmis repository, convert properties from jcr domain to cmis
                //and update properties
                cmisObject = session.getObject(objectId.getIdentifier());
                
                //unknown object?
                if (cmisObject == null) {
                    //exit silently
                    return;
                }
                
                //Prepare store for the cmis properties
                Map updateProperties = new HashMap();
                
                //ask cmis repository to get property definitions
                //we will use these definitions for correct conversation
                Map<String, PropertyDefinition<?>> propDefs = cmisObject.getBaseType().getPropertyDefinitions();
                
                //jcr properties are grouped by namespace uri
                //we will travers over all namespaces (ie group of properties)
                Iterator<Field> it = jcrProperties.fields().iterator();
                while (it.hasNext()) {
                    //field has namespace uri as field's name and properties
                    //as value
                    Field field = it.next();
                    
                    //getting namespace uri and properties
                    String namespaceUri = field.getName();
                    Document props = field.getValueAsDocument();
                    
                    //namespace uri uniquily defines prefix for the property name
                    String prefix = prefixes.value(namespaceUri);
                    
                    //now scroll over properties
                    Iterator<Field> pit = props.fields().iterator();
                    while (pit.hasNext()) {
                        Field property = pit.next();
                        
                        //getting jcr fully qualified name of property
                        //then determine the the name of this property 
                        //in the cmis domain
                        String jcrPropertyName = prefix + property.getName();
                        String cmisPropertyName = properties.findCmisName(jcrPropertyName);
                        
                        //now we need to convert value, we will use 
                        //property definition from the original cmis repo for this step
                        PropertyDefinition pdef = propDefs.get(cmisPropertyName);
                        
                        //unknown property? 
                        if (pdef == null) {
                            //ignore
                            return;
                        }
                        
                        //make conversation for the value
                        Object cmisValue = properties.cmisValue(pdef, property);
                        
                        //store properties for update
                        updateProperties.put(cmisPropertyName, cmisValue);
                    }
                }
                
                //finaly execute update action
                if (!updateProperties.isEmpty()) {
                    cmisObject.updateProperties(updateProperties);
                }
                break;
        }
    }

    @Override
    public void updateDocument(DocumentChanges delta) {
        //object id is a composite key which holds information about 
        //unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(delta.getDocumentId());

        //this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY :
                //repository node is read only
                break;
            case CONTENT :
                //in the jcr domain content is represented by child node of 
                //the nt:file node while in cmis domain it is a property of 
                //the cmis:document object. so to perform this operation we need 
                //to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();
                
                //now let's get the reference to this object
                CmisObject cmisObject = session.getObject(cmisId);
                
                //object exists?
                if (cmisObject == null) {
                    //object does not exist. propably was deleted by from cmis domain
                    //we don't know how to handle such case yet, thus TODO
                    return;
                }
                
                PropertyChanges changes = delta.getPropertyChanges();                
                //for this case we have only one property jcr:data
                //what we need to understant it is what kind of action
                
                if (!changes.getRemoved().isEmpty()) {                    
                    //we need to remove
                    ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).deleteContentStream();
                } else {
                    ContentStream stream = jcrBinaryContent(delta.getDocument());
                    if (stream != null && cmisObject != null) {
                        ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).setContentStream(stream, true);
                    }
                }
                break;
            case OBJECT :
                //modifing cmis:folders and cmis:documents
                cmisObject = session.getObject(objectId.getIdentifier());
                changes = delta.getPropertyChanges();
                
                Document props = delta.getDocument().getDocument("properties");                
                
                //checking that object exists
                if (cmisObject == null) {
                    //unknown object
                    return;
                }
                
                //Prepare store for the cmis properties
                Map updateProperties = new HashMap();
                
                //ask cmis repository to get property definitions
                //we will use these definitions for correct conversation
                Map<String, PropertyDefinition<?>> propDefs = cmisObject.getBaseType().getPropertyDefinitions();
                
                //group added and modified properties
                ArrayList<Name> modifications = new ArrayList();
                modifications.addAll(changes.getAdded());
                modifications.addAll(changes.getChanged());
                
                //convert names and values 
                for (Name name : modifications) {
                    String prefix = prefixes.value(name.getNamespaceUri());            
                    
                    //prefixed name of the property in jcr domain is
                    String jcrPropertyName = prefix != null ? 
                        prefix + ":" + name.getLocalName() : name.getLocalName();
                    //the name of this property in cmis domain is
                    String cmisPropertyName = properties.findCmisName(jcrPropertyName);
                    
                    //in cmis domain this property is defined as
                    PropertyDefinition pdef = propDefs.get(cmisPropertyName);
                    
                    //unknown property?
                    if (pdef == null) {
                        //ignore
                        continue;
                    }
                    
                    //convert value and store 
                    Document jcrValues = props.getDocument(name.getNamespaceUri());
                    updateProperties.put(cmisPropertyName, properties.cmisValue(pdef, name.getLocalName(), jcrValues));
                    
                }
                
                //step #2: nullify removed properties
                for (Name name : changes.getRemoved()) {
                    String prefix = prefixes.value(name.getNamespaceUri());            
                    
                    //prefixed name of the property in jcr domain is
                    String jcrPropertyName = prefix != null ? 
                        prefix + ":" + name.getLocalName() : name.getLocalName();
                    //the name of this property in cmis domain is
                    String cmisPropertyName = properties.findCmisName(jcrPropertyName);
                    
                    //in cmis domain this property is defined as
                    PropertyDefinition pdef = propDefs.get(cmisPropertyName);
                    
                    //unknown property?
                    if (pdef == null) {
                        //ignore
                        continue;
                    }
                    
                    updateProperties.put(cmisPropertyName, null);
                }
                
                //run update action
                cmisObject.updateProperties(updateProperties);
                break;
        }        
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public String newDocumentId(String parentId, Name name, Name primaryType) {
        HashMap<String, Object> params = new HashMap();

        //let'start from checking primary type
        if (primaryType.getLocalName().equals("resource")) {
            //nt:resource node belongs to cmis:document's content thus 
            //we must return just parent id without creating any CMIS object
            return ObjectId.toString(ObjectId.Type.CONTENT, parentId);
        }
        
        //all other node types belong to cmis object
        String jcrNodeType = primaryType.getLocalName();        
        String cmisObjectTypeName = nodes.findCmisName(jcrNodeType);

        Folder parent = (Folder) session.getObject(parentId);

        //Ivan, we can pick up object type and prperty definition map from CMIS repo        
        ObjectType objectType = session.getTypeDefinition(cmisObjectTypeName);
        Map<String, PropertyDefinition<?>> propDefs = objectType.getPropertyDefinitions();
        
        //assign mandatory properties
        Collection<PropertyDefinition<?>> list = propDefs.values();
        for (PropertyDefinition<?> pdef : list) {
            if (pdef.isRequired()) {
                params.put(pdef.getId(), "");
            }
        }
        
        String path = parent.getPath() + "/" + name.getLocalName();

        //assign(override) 100% mandatory properties
        params.put(PropertyIds.OBJECT_TYPE_ID, objectType.getId());
        params.put(PropertyIds.NAME, name.getLocalName());
        
        //create object and id for it.
        switch (objectType.getBaseTypeId()) {
            case CMIS_FOLDER :
                params.put(PropertyIds.PATH, path);
                String id = ObjectId.toString(ObjectId.Type.OBJECT, parent.createFolder(params).getId());
                return  id;
            case CMIS_DOCUMENT :
                id = ObjectId.toString(ObjectId.Type.OBJECT, parent.createDocument(params, null, VersioningState.NONE).getId());
                return id;
            default :
                return null;
        }
    }

    /**
     * Converts CMIS object to JCR node.
     *
     * @param id the identifier of the CMIS object
     * @return JCR node document.
     */
    private Document cmisObject(String id) {
        CmisObject cmisObject = session.getObject(id);

        //object does not exist? return null
        if (cmisObject == null) {
            return null;
        }

        //converting CMIS object to JCR node
        switch (cmisObject.getBaseTypeId()) {
            case CMIS_FOLDER:
                return cmisFolder(cmisObject);
            case CMIS_DOCUMENT:
                return cmisDocument(cmisObject);
        }

        //unexpected object type
        return null;
    }

    /**
     * Translates CMIS folder object to JCR node
     *
     * @param cmisObject CMIS folder object
     * @return JCR node document.
     */
    private Document cmisFolder(CmisObject cmisObject) {
        Folder folder = (Folder) cmisObject;
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.OBJECT, folder.getId()));

        ObjectType objectType = cmisObject.getType();
        if (objectType.isBaseType()) {
            writer.setPrimaryType(NodeType.NT_FOLDER);
        } else {
            writer.setPrimaryType(objectType.getId());
        }

        writer.setParent(folder.getParentId());
        //TODO: Is this required?
        writer.addMixinType(NodeType.MIX_CREATED);
        writer.addMixinType(NodeType.MIX_LAST_MODIFIED);
        writer.addMixinType(NodeType.MIX_REFERENCEABLE);
        writer.addMixinType("mix:folder");

        cmisProperties(folder, writer);
        cmisChildren(folder, writer);

        //append repository information to the root node
        if (folder.isRootFolder()) {
            writer.addChild(ObjectId.toString(ObjectId.Type.REPOSITORY, ""), REPOSITORY_NODE_NAME);
        }

        return writer.document();
    }

    /**
     * Translates cmis document object to JCR node.
     *
     * @param cmisObject cmis document node
     * @return JCR node document.
     */
    public Document cmisDocument(CmisObject cmisObject) {
        org.apache.chemistry.opencmis.client.api.Document doc =
                (org.apache.chemistry.opencmis.client.api.Document) cmisObject;
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.OBJECT, doc.getId()));

        ObjectType objectType = cmisObject.getType();
        if (objectType.isBaseType()) {
            writer.setPrimaryType(NodeType.NT_FILE);
        } else {
            writer.setPrimaryType(objectType.getId());
        }

        /*

        + myFile  (jcr:primaryType=nt:file,
           |        jcr:created=<date>,
           |        jcr:createdBy=<username>)
           + jcr:content  (jcr:primaryType=nt:resource,
                           jcr:lastModified=<date>,
                           jcr:lastModifiedBy=<username>,
                           jcr:mimeType=<mimeType>,
                           jcr:encoding=<null>,
                           jcr:data=<binary-content>)
        */
        //TODO: Is this required? 
        //Ivan, yes. Just try to comment out those lines
       
        List<Folder> parents = doc.getParents();
        ArrayList parentIds = new ArrayList();
        
        for (Folder f : parents) {
            parentIds.add(ObjectId.toString(ObjectId.Type.OBJECT, f.getId()));
        }

        writer.setParents(parentIds);
        
        writer.addMixinType(NodeType.MIX_CREATED);
        writer.addMixinType(NodeType.MIX_LAST_MODIFIED);
        writer.addMixinType(NodeType.MIX_REFERENCEABLE);

        cmisProperties(doc, writer);
        writer.addChild(ObjectId.toString(ObjectId.Type.CONTENT, doc.getId()), JcrConstants.JCR_CONTENT);

        return writer.document();
    }

    /**
     * Converts binary content into JCR node.
     *
     * @param id the id of the CMIS document.
     * @return JCR node representation.
     */
    private Document cmisContent(String id) {
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.CONTENT, id));

        org.apache.chemistry.opencmis.client.api.Document doc =
                (org.apache.chemistry.opencmis.client.api.Document) session.getObject(id);
        writer.setPrimaryType(NodeType.NT_RESOURCE);
        writer.setParent(id);
        
        if (doc.getContentStream() != null) {
            InputStream is = doc.getContentStream().getStream();
            BinaryValue content = factories.getBinaryFactory().create(is);
            writer.addProperty(JcrConstants.JCR_DATA, content);
            writer.addProperty(JcrConstants.JCR_MIME_TYPE, doc.getContentStream().getMimeType());
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
            String pname = properties.findJcrName(property.getId());
            writer.addProperty(pname, properties.jcrValues(property));
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
    private Document cmisRepository() {
        RepositoryInfo info = session.getRepositoryInfo();
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.REPOSITORY, ""));

        writer.setPrimaryType(CmisLexicon.REPOSITORY);
        writer.setId(REPOSITORY_ID);

        //product name/vendor/version
        writer.addProperty(CmisLexicon.VENDOR_NAME, info.getVendorName());
        writer.addProperty(CmisLexicon.PRODUCT_NAME, info.getProductName());
        writer.addProperty(CmisLexicon.PRODUCT_VERSION, info.getProductVersion());

        return writer.document();
    }

    /**
     * Creates content stream using JCR node.
     *
     * @param document JCR node representation
     * @return CMIS content stream object
     */
    private ContentStream jcrBinaryContent(Document document) {
        System.out.println("Binary content: " + document);
        //pickup node properties
        Document props = document.getDocument("properties").getDocument(JcrLexicon.Namespace.URI);

        //extract binary value and content
        Binary value = props.getBinary("data");

        if (value == null) {
            return null;
        }

        byte[] content = value.getBytes();
        String fileName = props.getString("fileName");
        String mimeType = props.getString("mimeType");

        //wrap with input stream
        ByteArrayInputStream bin = new ByteArrayInputStream(content);
        bin.reset();

        //create content stream
        ContentStreamImpl contentStream = new ContentStreamImpl(fileName, BigInteger.valueOf(content.length), mimeType, bin);

        return contentStream;
    }

    /**
     * Import CMIS types to JCR repository.
     *
     * @param types       CMIS types
     * @param typeManager JCR type manager
     * @param registry
     */
    private void importTypes(List<Tree<ObjectType>> types, NodeTypeManager typeManager, NamespaceRegistry registry) throws RepositoryException {
        for (int i = 0; i < types.size(); i++) {
            Tree<ObjectType> tree = types.get(i);
            importType(tree.getItem(), typeManager, registry);
            importTypes(tree.getChildren(), typeManager, registry);
        }
    }

    /**
     * Import given CMIS type to the JCR repository.
     *
     * @param cmisType    cmis object type
     * @param typeManager JCR type manager/
     * @param registry
     */
    public void importType(ObjectType cmisType, NodeTypeManager typeManager, NamespaceRegistry registry) throws RepositoryException {
        //skip base types because we are going to
        //map base types directly
        if (cmisType.isBaseType()) {
            return;
        }

        //TODO: get namespace information and register
        //registry.registerNamespace(cmisType.getLocalNamespace(), cmisType.getLocalNamespace());

        //create node type template
        NodeTypeTemplate type = typeManager.createNodeTypeTemplate();

        //convert CMIS type's attributes to node type template we have just created
        type.setName(cmisType.getId());
        type.setAbstract(false);
        type.setMixin(true);
        type.setOrderableChildNodes(true);
        type.setQueryable(true);
        type.setDeclaredSuperTypeNames(superTypes(cmisType));

        Map<String, PropertyDefinition<?>> props = cmisType.getPropertyDefinitions();
        Set<String> names = props.keySet();
        //properties
        for (String name : names) {
            PropertyDefinition pd = props.get(name);
            PropertyDefinitionTemplate pt = typeManager.createPropertyDefinitionTemplate();
            pt.setRequiredType(properties.getJcrType(pd.getPropertyType()));
            pt.setAutoCreated(false);
            pt.setAvailableQueryOperators(new String[]{});
            pt.setName(name);
            pt.setMandatory(pd.isRequired());
            type.getPropertyDefinitionTemplates().add(pt);
        }

        //register type
        NodeTypeDefinition[] nodeDefs = new NodeTypeDefinition[]{type};
        typeManager.registerNodeTypes(nodeDefs, true);
    }

    /**
     * Determines supertypes for the given CMIS type in terms of JCR.
     *
     * @param cmisType given CMIS type
     * @return supertypes in JCR lexicon.
     */
    private String[] superTypes(ObjectType cmisType) {
        if (cmisType.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
            return new String[] {JcrConstants.NT_FOLDER};
        }

        if (cmisType.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
            return new String[] {JcrConstants.NT_FILE};
        }

        return new String[]{cmisType.getParentType().getId()};
    }

    /**
     * Defines node type for the repository info.
     *
     * @param typeManager JCR node type manager.
     * @throws RepositoryException
     */
    private void registerRepositoryInfoType(NodeTypeManager typeManager) throws RepositoryException {
        //create node type template
        NodeTypeTemplate type = typeManager.createNodeTypeTemplate();

        //convert CMIS type's attributes to node type template we have just created
        type.setName("cmis:repository");
        type.setAbstract(false);
        type.setMixin(true);
        type.setOrderableChildNodes(true);
        type.setQueryable(true);
        type.setDeclaredSuperTypeNames(new String[]{JcrConstants.NT_FOLDER});

        PropertyDefinitionTemplate vendorName = typeManager.createPropertyDefinitionTemplate();
        vendorName.setAutoCreated(false);
        vendorName.setName("cmis:vendorName");
        vendorName.setMandatory(false);

        type.getPropertyDefinitionTemplates().add(vendorName);

        PropertyDefinitionTemplate productName = typeManager.createPropertyDefinitionTemplate();
        productName.setAutoCreated(false);
        productName.setName("cmis:productName");
        productName.setMandatory(false);

        type.getPropertyDefinitionTemplates().add(productName);

        PropertyDefinitionTemplate productVersion = typeManager.createPropertyDefinitionTemplate();
        productVersion.setAutoCreated(false);
        productVersion.setName("cmis:productVersion");
        productVersion.setMandatory(false);

        type.getPropertyDefinitionTemplates().add(productVersion);

        //register type
        NodeTypeDefinition[] nodeDefs = new NodeTypeDefinition[]{type};
        typeManager.registerNodeTypes(nodeDefs, true);
    }
}
