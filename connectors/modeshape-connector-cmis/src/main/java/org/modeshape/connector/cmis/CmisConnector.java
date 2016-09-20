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
package org.modeshape.connector.cmis;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.NamespaceRegistry;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeDefinition;
import javax.jcr.nodetype.NodeTypeTemplate;
import javax.jcr.nodetype.PropertyDefinitionTemplate;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.FileableCmisObject;
import org.apache.chemistry.opencmis.client.api.Folder;
import org.apache.chemistry.opencmis.client.api.ItemIterable;
import org.apache.chemistry.opencmis.client.api.ObjectType;
import org.apache.chemistry.opencmis.client.api.OperationContext;
import org.apache.chemistry.opencmis.client.api.Property;
import org.apache.chemistry.opencmis.client.api.Session;
import org.apache.chemistry.opencmis.client.api.Tree;
import org.apache.chemistry.opencmis.client.bindings.spi.StandardAuthenticationProvider;
import org.apache.chemistry.opencmis.client.runtime.OperationContextImpl;
import org.apache.chemistry.opencmis.client.runtime.SessionFactoryImpl;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.SessionParameter;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.definitions.PropertyDefinition;
import org.apache.chemistry.opencmis.commons.enums.BaseTypeId;
import org.apache.chemistry.opencmis.commons.enums.BindingType;
import org.apache.chemistry.opencmis.commons.enums.Updatability;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.exceptions.CmisObjectNotFoundException;
import org.apache.chemistry.opencmis.commons.impl.dataobjects.ContentStreamImpl;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.spi.federation.Connector;
import org.modeshape.jcr.spi.federation.DocumentChanges;
import org.modeshape.jcr.spi.federation.DocumentChanges.ChildrenChanges;
import org.modeshape.jcr.spi.federation.DocumentChanges.PropertyChanges;
import org.modeshape.jcr.spi.federation.DocumentWriter;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.binary.ExternalBinaryValue;
import org.modeshape.schematic.document.Binary;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Document.Field;

/**
 * This connector exposes the content of a CMIS repository.
 * <p>
 * The Content Management Interoperability Services (CMIS) standard deﬁnes a domain model and Web Services that can be used by
 * applications to work with one or more Content Management repositories/systems.
 * </p>
 * <p>
 * The CMIS connector is designed to be layered on top of existing Content Management systems. It is intended to use Apache
 * Chemistry API to access services provided by Content Management system and incorporate those services into Modeshape content
 * repository.
 * </p>
 * <p>
 * There are several attributes that should be configured on each external source:
 * <ul>
 * <li><strong><code></code></strong></li>
 * <li><strong><code>aclService</code></strong> URL of the Access list service binding entry point. The ACL Services are used to
 * discover and manage Access Control Lists.</li>
 * <li><strong><code>discoveryService</code></strong> URL of the Discovery service binding entry point. Discovery service executes
 * a CMIS query statement against the contents of the repository.</li>
 * <li><strong><code>multifilingService</code></strong> URL of the Multi-filing service binding entry point. The Multi-ﬁling
 * Services are used to ﬁle/un-ﬁle objects into/from folders.</li>
 * <li><strong><code>navigationService</code></strong> URL of the Navigation service binding entry point. The Navigation service
 * gets the list of child objects contained in the speciﬁed folder.</li>
 * <li><strong><code>objectService</code></strong> URL of the Object service binding entry point. Creates a document object of the
 * speciﬁed type (given by the cmis:objectTypeId property) in the (optionally) speciﬁed location</li>
 * <li><strong><code>policyService</code></strong> URL of the Policy service binding entry point. Applies a speciﬁed policy to an
 * object.</li>
 * <li><strong><code>relationshipService</code></strong> URL of the Relationship service binding entry point. Gets all or a subset
 * of relationships associated with an independent object.</li>
 * <li><strong><code>repositoryService</code></strong> URL of the Repository service binding entry point. Returns a list of CMIS
 * repositories available from this CMIS service endpoint.</li>
 * <li><strong><code>versioningService</code></strong> URL of the Policy service binding entry point. Create a private working
 * copy (PWC) of the document.</li>
 * </ul>
 * </p>
 * <p>
 * The connector results in the following form
 * </p>
 * <table cellspacing="0" cellpadding="1" border="1">
 * <tr>
 * <th>Path</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td><code>/repository_info</code></td>
 * <td>Repository description</td>
 * </tr>
 * <tr>
 * <td><code>/filesAndFolder</code></td>
 * <td>The structure of the folders and files in the projected repository</td>
 * </tr>
 * <table>
 * 
 * @author Oleg Kulikov
 * @author Ivan Vasyliev
 */
public class CmisConnector extends Connector {

    // path and id for the repository node
    private static final String REPOSITORY_INFO_ID = "repositoryInfo";
    private static final String REPOSITORY_INFO_NODE_NAME = "repositoryInfo";
    private Session session;
    private ValueFactories factories;
    // binding parameters
    private String aclService;
    private String discoveryService;
    private String multifilingService;
    private String navigationService;
    private String objectService;
    private String policyService;
    private String relationshipService;
    private String repositoryService;
    private String versioningService;
    // repository id
    private String repositoryId;
    private Properties properties;
    private Nodes nodes;

    private Prefix prefixes = new Prefix();
    private final OperationContext ctx = new OperationContextImpl();
    
    public CmisConnector() {
        super();
        ctx.setIncludeAcls(true);
    }

    @SuppressWarnings( "deprecation" )
    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.initialize(registry, nodeTypeManager);

        this.factories = getContext().getValueFactories();

        properties = new Properties(getContext().getValueFactories());
        nodes = new Nodes();

        // default factory implementation
        Map<String, String> parameter = new HashMap<String, String>();

        // user credentials
        // parameter.put(SessionParameter.USER, user);
        // parameter.put(SessionParameter.PASSWORD, passw);

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
        session = factory.createSession(parameter, null, new StandardAuthenticationProvider(), null, null);

        registry.registerNamespace(CmisLexicon.Namespace.PREFIX, CmisLexicon.Namespace.URI);
        importTypes(session.getTypeDescendants(null, Integer.MAX_VALUE, true), nodeTypeManager, registry);
        registerRepositoryInfoType(nodeTypeManager);
    }

    @Override
    public Document getDocumentById( String id ) {
        // object id is a composite key which holds information about
        // unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(id);

        // this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY_INFO:
                // convert information about repository from
                // cmis domain into jcr domain
                return cmisRepository();
            case CONTENT:
                // in the jcr domain content is represented by child node of
                // the nt:file node while in cmis domain it is a property of
                // the cmis:document object. This action searches original
                // cmis:document and converts its content property into jcr node
                return cmisContent(objectId.getIdentifier());
            case OBJECT:
                // converts cmis folders and documents into jcr folders and files
                return cmisObject(objectId.getIdentifier());
            case ACL :
                return cmisAccessList(objectId.getIdentifier());
            case PERMISSIONS :
                return cmisPermission(objectId.getIdentifier());
            default:
                return null;
        }
    }

    @Override
    public String getDocumentId( String path ) {
        // establish relation between path and object identifier
        return session.getObjectByPath(path).getId();
    }

    @Override
    public Collection<String> getDocumentPathsById( String id ) {
        CmisObject obj = session.getObject(id);
        // check that object exist
        if (obj instanceof Folder) {
            return Collections.singletonList(((Folder)obj).getPath());
        }
        if (obj instanceof org.apache.chemistry.opencmis.client.api.Document) {
            org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document)obj;
            List<Folder> parents = doc.getParents();
            List<String> paths = new ArrayList<String>(parents.size());
            for (Folder parent : doc.getParents()) {
                paths.add(parent.getPath() + "/" + doc.getName());
            }
            return paths;
        }
        return Collections.emptyList();
    }

    @Override
    public boolean removeDocument( String id ) {
        // object id is a composite key which holds information about
        // unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(id);

        // this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY_INFO:
                // information about repository is ready only
                return false;
            case CONTENT:
                // in the jcr domain content is represented by child node of
                // the nt:file node while in cmis domain it is a property of
                // the cmis:document object. so to perform this operation we need
                // to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();

                org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document)session.getObject(cmisId);

                // object exists?
                if (doc == null) {
                    // object does not exist. propably was deleted by from cmis domain
                    // we don't know how to handle such case yet, thus TODO
                    return false;
                }

                // delete content stream
                doc.deleteContentStream();
                return true;
            case OBJECT:
                // these type points to either cmis:document or cmis:folder so
                // we can just delete it using original identifier defined in cmis domain.
                CmisObject object = null;
                try {
                    session.getObject(objectId.getIdentifier());
                } catch (CmisObjectNotFoundException e) {
                    return false;
                }

                // check that object exist
                if (object == null) {
                    return false;
                }

                // just delete object
                object.delete(true);
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean hasDocument( String id ) {
        // object id is a composite key which holds information about
        // unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(id);

        // this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY_INFO:
                // exist always
                return true;
            case CONTENT:
                // in the jcr domain content is represented by child node of
                // the nt:file node while in cmis domain it is a property of
                // the cmis:document object. so to perform this operation we need
                // to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();

                // now checking that this document exists
                return session.getObject(cmisId) != null;
            case ACL :
            case PERMISSIONS :
                return true;
            default:
                // here we checking cmis:folder and cmis:document
                return session.getObject(id) != null;
        }
    }

    @Override
    public void storeDocument( Document document ) {
        // object id is a composite key which holds information about
        // unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(document.getString("key"));
        ObjectId parentId = ObjectId.valueOf(document.getString("parent"));
        
        //if parent is ACL then handle this document as permission entry
        if (parentId.getType() == ObjectId.Type.ACL) {
            //bypass this
            return;
        }

        // this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY_INFO:
                // repository information is ready only
                return;
            case CONTENT:
                // in the jcr domain content is represented by child node of
                // the nt:file node while in cmis domain it is a property of
                // the cmis:document object. so to perform this operation we need
                // to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();

                // now let's get the reference to this object
                CmisObject cmisObject = session.getObject(cmisId);

                // object exists?
                if (cmisObject == null) {
                    // object does not exist. propably was deleted by from cmis domain
                    // we don't know how to handle such case yet, thus TODO
                    return;
                }

                // original object is here so converting binary value and
                // updating original cmis:document
                ContentStream stream = jcrBinaryContent(document);
                if (stream != null) {
                    ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).setContentStream(stream, true);
                }
                break;
            case OBJECT:
                // extract node properties from the document view
                Document jcrProperties = document.getDocument("properties");

                // check that we have jcr properties to store in the cmis repo
                if (jcrProperties == null) {
                    // nothing to store
                    return;
                }

                // if node has properties we need to pickup cmis object from
                // cmis repository, convert properties from jcr domain to cmis
                // and update properties
                cmisObject = session.getObject(objectId.getIdentifier());

                // unknown object?
                if (cmisObject == null) {
                    // exit silently
                    return;
                }

                // Prepare store for the cmis properties
                Map<String, Object> updateProperties = new HashMap<String, Object>();

                // ask cmis repository to get property definitions
                // we will use these definitions for correct conversation
                Map<String, PropertyDefinition<?>> propDefs = cmisObject.getBaseType().getPropertyDefinitions();

                // jcr properties are grouped by namespace uri
                // we will travers over all namespaces (ie group of properties)
                for (Field field : jcrProperties.fields()) {
                    // field has namespace uri as field's name and properties
                    // as value
                    // getting namespace uri and properties
                    String namespaceUri = field.getName();
                    Document props = field.getValueAsDocument();

                    // namespace uri uniquily defines prefix for the property name
                    String prefix = prefixes.value(namespaceUri);

                    // now scroll over properties
                    for (Field property : props.fields()) {
                        // getting jcr fully qualified name of property
                        // then determine the the name of this property
                        // in the cmis domain
                        String jcrPropertyName = prefix + property.getName();
                        String cmisPropertyName = properties.findCmisName(jcrPropertyName);

                        // now we need to convert value, we will use
                        // property definition from the original cmis repo for this step
                        PropertyDefinition<?> pdef = propDefs.get(cmisPropertyName);

                        // unknown property?
                        if (pdef == null) {
                            // ignore
                            return;
                        }

                        // make conversation for the value
                        Object cmisValue = properties.cmisValue(pdef, property);

                        // store properties for update
                        updateProperties.put(cmisPropertyName, cmisValue);
                    }
                }

                // finaly execute update action
                if (!updateProperties.isEmpty()) {
                    cmisObject.updateProperties(updateProperties);
                }
                break;
        }
    }

    @Override
    public void updateDocument( DocumentChanges delta ) {
        ObjectId parentId = ObjectId.valueOf(delta.getDocument().getString("parent"));
        if (parentId.getType().equals(ObjectId.Type.ACL)) {
            //skip modification
            return;
        }
        // object id is a composite key which holds information about
        // unique object identifier and about its type
        ObjectId objectId = ObjectId.valueOf(delta.getDocumentId());

        // this action depends from object type
        switch (objectId.getType()) {
            case REPOSITORY_INFO:
                // repository node is read only
                break;
            case CONTENT:
                // in the jcr domain content is represented by child node of
                // the nt:file node while in cmis domain it is a property of
                // the cmis:document object. so to perform this operation we need
                // to restore identifier of the original cmis:document. it is easy
                String cmisId = objectId.getIdentifier();

                // now let's get the reference to this object
                CmisObject cmisObject = session.getObject(cmisId);

                // object exists?
                if (cmisObject == null) {
                    // object does not exist. propably was deleted by from cmis domain
                    // we don't know how to handle such case yet, thus TODO
                    return;
                }

                PropertyChanges changes = delta.getPropertyChanges();
                // for this case we have only one property jcr:data
                // what we need to understant it is what kind of action

                if (!changes.getRemoved().isEmpty()) {
                    // we need to remove
                    ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).deleteContentStream();
                } else {
                    ContentStream stream = jcrBinaryContent(delta.getDocument());
                    if (stream != null) {
                        ((org.apache.chemistry.opencmis.client.api.Document)cmisObject).setContentStream(stream, true);
                    }
                }
                break;
            case OBJECT:
                // modifing cmis:folders and cmis:documents
                cmisObject = session.getObject(objectId.getIdentifier());
                changes = delta.getPropertyChanges();

                Document props = delta.getDocument().getDocument("properties");

                // checking that object exists
                if (cmisObject == null) {
                    // unknown object
                    return;
                }

                // Prepare store for the cmis properties
                Map<String, Object> updateProperties = new HashMap<String, Object>();

                // ask cmis repository to get property definitions
                // we will use these definitions for correct conversation
                Map<String, PropertyDefinition<?>> propDefs = cmisObject.getBaseType().getPropertyDefinitions();

                // group added and modified properties
                ArrayList<Name> modifications = new ArrayList<Name>();
                modifications.addAll(changes.getAdded());
                modifications.addAll(changes.getChanged());

                // convert names and values
                for (Name name : modifications) {
                    String prefix = prefixes.value(name.getNamespaceUri());

                    // prefixed name of the property in jcr domain is
                    String jcrPropertyName = prefix != null ? prefix + ":" + name.getLocalName() : name.getLocalName();
                    // the name of this property in cmis domain is
                    String cmisPropertyName = properties.findCmisName(jcrPropertyName);

                    // in cmis domain this property is defined as
                    PropertyDefinition<?> pdef = propDefs.get(cmisPropertyName);
                    
                    // unknown property?
                    if (pdef == null) {
                        // ignore
                        continue;
                    }

                    Updatability updatability = pdef.getUpdatability();
                    if (updatability == Updatability.READWRITE) {
                        // convert value and store
                        Document jcrValues = props.getDocument(name.getNamespaceUri());
                        updateProperties.put(cmisPropertyName, properties.cmisValue(pdef, name.getLocalName(), jcrValues));
                    }
                }

                // step #2: nullify removed properties
                for (Name name : changes.getRemoved()) {
                    String prefix = prefixes.value(name.getNamespaceUri());

                    // prefixed name of the property in jcr domain is
                    String jcrPropertyName = prefix != null ? prefix + ":" + name.getLocalName() : name.getLocalName();
                    // the name of this property in cmis domain is
                    String cmisPropertyName = properties.findCmisName(jcrPropertyName);

                    // in cmis domain this property is defined as
                    PropertyDefinition<?> pdef = propDefs.get(cmisPropertyName);

                    // unknown property?
                    if (pdef == null) {
                        // ignore
                        continue;
                    }

                    updateProperties.put(cmisPropertyName, null);
                }

                // run update action
                if (!updateProperties.isEmpty()) {
                    cmisObject.updateProperties(updateProperties);
                }

                ChildrenChanges childrenChanges = delta.getChildrenChanges();
                Map<String, Name> renamed = new HashMap<>();
                renamed.putAll(childrenChanges.getRenamed());
                renamed.putAll(childrenChanges.getAppended());

                String before, after;
                for (String key : renamed.keySet()) {
                    CmisObject object = session.getObject(key);
                    if (object == null) continue;

                    // check if name was changed
                    before = object.getName();
                    after = renamed.get(key).getLocalName();
                    if (after.equals(before)) continue;

                    // determine if in child's parent already exists a child with same name
                    if (isExistCmisObject(((FileableCmisObject) object).getParents().get(0).getPath() + "/" + after)) {
                        // already exists, so generates a temporary name
                        after += "-temp";
                    }

                    rename(object, after);
                }
            
                // run move action
                if (delta.getParentChanges().hasNewPrimaryParent()) {
                    FileableCmisObject object = (FileableCmisObject)cmisObject;
                    CmisObject source = object.getParents().get(0);
                    CmisObject destination = session.getObject(delta.getParentChanges().getNewPrimaryParent());

                    object.move(source, destination);

                    // rename temporary name to a original
                    String name = object.getName();
                    if (name.endsWith("-temp")) {
                        rename(object, name.replace("-temp", ""));
                    }
                }
                
                break;
                
            case ACL :
                break;
        }
    }

    /**
     * Utility method for checking if CMIS object exists at defined path
     * @param path path for object
     * @return <code>true</code> if exists, <code>false</code> otherwise
     */
    private boolean isExistCmisObject(String path) {
        try {
            session.getObjectByPath(path);
            return true;
        }
        catch (CmisObjectNotFoundException e) {
            return false;
        }
    }

    /**
     * Utility method for renaming CMIS object
     * @param object CMIS object to rename
     * @param name new name
     */
    private void rename(CmisObject object, String name){
        Map<String, Object> newName = new HashMap<String, Object>();
        newName.put("cmis:name", name);

        object.updateProperties(newName);
    }

    @Override
    public boolean isReadonly() {
        return false;
    }

    @Override
    public String newDocumentId( String parentId,
                                 Name name,
                                 Name primaryType ) {
        HashMap<String, Object> params = new HashMap<String, Object>();

        // let'start from checking primary type
        if (primaryType.getLocalName().equals("resource")) {
            // nt:resource node belongs to cmis:document's content thus
            // we must return just parent id without creating any CMIS object
            return ObjectId.toString(ObjectId.Type.CONTENT, parentId);
        }

        // all other node types belong to cmis object
        String jcrNodeType = primaryType.toString();
        String cmisObjectTypeName = nodes.findCmisName(jcrNodeType);

        Folder parent = (Folder)session.getObject(parentId);

        // Ivan, we can pick up object type and prperty definition map from CMIS repo
        ObjectType objectType = session.getTypeDefinition(cmisObjectTypeName);
        Map<String, PropertyDefinition<?>> propDefs = objectType.getPropertyDefinitions();

        // assign mandatory properties
        Collection<PropertyDefinition<?>> list = propDefs.values();
        for (PropertyDefinition<?> pdef : list) {
            if (pdef.isRequired()) {
                params.put(pdef.getId(), "");
            }
        }

        String path = parent.getPath() + "/" + name.getLocalName();

        // assign(override) 100% mandatory properties
        params.put(PropertyIds.OBJECT_TYPE_ID, objectType.getId());
        params.put(PropertyIds.NAME, name.getLocalName());

        // create object and id for it.
        switch (objectType.getBaseTypeId()) {
            case CMIS_FOLDER:
                params.put(PropertyIds.PATH, path);
                return ObjectId.toString(ObjectId.Type.OBJECT, parent.createFolder(params).getId());
            case CMIS_DOCUMENT:
                return ObjectId.toString(ObjectId.Type.OBJECT, parent.createDocument(params, null, VersioningState.NONE).getId());
            default:
                return null;
        }
    }

    /**
     * Converts CMIS object to JCR node.
     * 
     * @param id the identifier of the CMIS object
     * @return JCR node document.
     */
    private Document cmisObject( String id ) {
        CmisObject cmisObject;
        try {
             cmisObject   = session.getObject(id);
        } catch (CmisObjectNotFoundException e) {
            return null;
        }

        // object does not exist? return null
        if (cmisObject == null) {
            return null;
        }

        // converting CMIS object to JCR node
        switch (cmisObject.getBaseTypeId()) {
            case CMIS_FOLDER:
                return cmisFolder(cmisObject);
            case CMIS_DOCUMENT:
                return cmisDocument(cmisObject);
            case CMIS_POLICY:
            case CMIS_RELATIONSHIP:
            case CMIS_SECONDARY:
            case CMIS_ITEM:
        }

        // unexpected object type
        return null;
    }

    /**
     * Translates CMIS folder object to JCR node
     * 
     * @param cmisObject CMIS folder object
     * @return JCR node document.
     */
    private Document cmisFolder( CmisObject cmisObject ) {
        Folder folder = (Folder)cmisObject;
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.OBJECT, folder.getId()));

        ObjectType objectType = cmisObject.getType();
        if (objectType.isBaseType()) {
            writer.setPrimaryType(NodeType.NT_FOLDER);
        } else {
            writer.setPrimaryType(objectType.getId());
        }

        writer.setParent(folder.getParentId());
        writer.addMixinType(NodeType.MIX_REFERENCEABLE);
        writer.addMixinType(NodeType.MIX_LAST_MODIFIED);

        cmisProperties(folder, writer);
        cmisChildren(folder, writer);

        writer.addMixinType("mode:accessControllable");
        writer.addChild(ObjectId.toString(ObjectId.Type.ACL, folder.getId()), "mode:acl");
        
        // append repository information to the root node
        if (folder.isRootFolder()) {
            writer.addChild(ObjectId.toString(ObjectId.Type.REPOSITORY_INFO, ""), REPOSITORY_INFO_NODE_NAME);
        }
        return writer.document();
    }

    /**
     * Translates cmis document object to JCR node.
     * 
     * @param cmisObject cmis document node
     * @return JCR node document.
     */
    public Document cmisDocument( CmisObject cmisObject ) {
        org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document)cmisObject;
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.OBJECT, doc.getId()));

        ObjectType objectType = cmisObject.getType();
        if (objectType.isBaseType()) {
            writer.setPrimaryType(NodeType.NT_FILE);
        } else {
            writer.setPrimaryType(objectType.getId());
        }

        List<Folder> parents = doc.getParents();
        ArrayList<String> parentIds = new ArrayList<String>();
        for (Folder f : parents) {
            parentIds.add(ObjectId.toString(ObjectId.Type.OBJECT, f.getId()));
        }

        writer.setParents(parentIds);
        writer.addMixinType(NodeType.MIX_REFERENCEABLE);
        writer.addMixinType(NodeType.MIX_LAST_MODIFIED);

        // document specific property conversation
        cmisProperties(doc, writer);
        writer.addChild(ObjectId.toString(ObjectId.Type.CONTENT, doc.getId()), JcrConstants.JCR_CONTENT);

        writer.addMixinType("mode:accessControllable");
        writer.addChild(ObjectId.toString(ObjectId.Type.ACL, cmisObject.getId()), "mode:acl");
        
        return writer.document();
    }

    /**
     * Converts binary content into JCR node.
     * 
     * @param id the id of the CMIS document.
     * @return JCR node representation.
     */
    private Document cmisContent( String id ) {
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.CONTENT, id));

        org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document)session.getObject(id);
        writer.setPrimaryType(NodeType.NT_RESOURCE);
        writer.setParent(id);

        ContentStream contentStream = doc.getContentStream();
        if (contentStream != null) {
            BinaryValue content = new CmisConnectorBinary(contentStream, getSourceName(), id, getMimeTypeDetector());
            writer.addProperty(JcrConstants.JCR_DATA, content);
            writer.addProperty(JcrConstants.JCR_MIME_TYPE, contentStream.getMimeType());
        }

        Property<Object> lastModified = doc.getProperty(PropertyIds.LAST_MODIFICATION_DATE);
        Property<Object> lastModifiedBy = doc.getProperty(PropertyIds.LAST_MODIFIED_BY);

        writer.addProperty(JcrLexicon.LAST_MODIFIED, properties.jcrValues(lastModified));
        writer.addProperty(JcrLexicon.LAST_MODIFIED_BY, properties.jcrValues(lastModifiedBy));

        return writer.document();
    }

    @Override
    public ExternalBinaryValue getBinaryValue(String id) {
        org.apache.chemistry.opencmis.client.api.Document doc = (org.apache.chemistry.opencmis.client.api.Document)session.getObject(id);
        if (doc == null) {
            return null;
        }
        ContentStream contentStream = doc.getContentStream();
        if (contentStream == null) {
            return null;
        }
        return new CmisConnectorBinary(contentStream, getSourceName(), id, getMimeTypeDetector());
    }

    /**
     * Converts CMIS object's properties to JCR node properties.
     * 
     * @param object CMIS object
     * @param writer JCR node representation.
     */
    private void cmisProperties( CmisObject object,
                                 DocumentWriter writer ) {
        // convert properties
        List<Property<?>> list = object.getProperties();
        for (Property<?> property : list) {
            String pname = properties.findJcrName(property.getId());
            if (pname != null) {
                writer.addProperty(pname, properties.jcrValues(property));
            }
        }
    }

    /**
     * Converts CMIS folder children to JCR node children
     * 
     * @param folder CMIS folder
     * @param writer JCR node representation
     */
    private void cmisChildren( Folder folder,
                               DocumentWriter writer ) {
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
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.REPOSITORY_INFO, ""));

        writer.setPrimaryType(CmisLexicon.REPOSITORY);
        writer.setId(REPOSITORY_INFO_ID);

        // product name/vendor/version
        writer.addProperty(CmisLexicon.VENDOR_NAME, info.getVendorName());
        writer.addProperty(CmisLexicon.PRODUCT_NAME, info.getProductName());
        writer.addProperty(CmisLexicon.PRODUCT_VERSION, info.getProductVersion());

        return writer.document();
    }

    private Document cmisAccessList(String id) {
        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.ACL, id));
        writer.setPrimaryType(ModeShapeLexicon.ACCESS_LIST_NODE_TYPE_STRING);
        writer.setParent(id);

        CmisObject obj = session.getObject(id);
        obj = session.getObject(obj, ctx);

        Acl acl = obj.getAcl();
        List<Ace> entries = acl.getAces();
        for (Ace entry : entries) {
            String entryId = AclObjectId.encode(id, entry.getPrincipalId());
            writer.addChild(ObjectId.toString(ObjectId.Type.PERMISSIONS, entryId), 
                    ModeShapeLexicon.PERMISSION.toString());
        }
        
        return writer.document();
    }
    
    private Document cmisPermission( String id ) {
        String cmisObjectId = AclObjectId.cmisObjectId(id);
        String entryId = AclObjectId.entryId(id);

        DocumentWriter writer = newDocument(ObjectId.toString(ObjectId.Type.PERMISSIONS, id));
        writer.setPrimaryType(ModeShapeLexicon.PERMISSION.toString());
        writer.setParent(id);
        
        Acl acl = session.getObject(cmisObjectId, ctx).getAcl();
        List<Ace> entries = acl.getAces();
        for (Ace entry : entries) {
            if (entry.getPrincipalId().equals(entryId)) {
                String name = entry.getPrincipal().getId();
                name = Converter.jcrPrincipal(name);
                writer.addProperty(ModeShapeLexicon.PERMISSION_PRINCIPAL_NAME.getLocalName(), name);
                List<String> perms = entry.getPermissions();
                writer.addProperty(ModeShapeLexicon.PERMISSION_PRIVILEGES_NAME.getLocalName(), Converter.jcrPermissions(perms));
            }
        }
        return writer.document();
    }
    
    /**
     * Creates content stream using JCR node.
     * 
     * @param document JCR node representation
     * @return CMIS content stream object
     */
    private ContentStream jcrBinaryContent( Document document ) {
        // pickup node properties
        Document props = document.getDocument("properties").getDocument(JcrLexicon.Namespace.URI);

        // extract binary value and content
        Binary value = props.getBinary("data");

        if (value == null) {
            return null;
        }

        byte[] content = value.getBytes();
        String fileName = props.getString("fileName");
        String mimeType = props.getString("mimeType");

        // wrap with input stream
        ByteArrayInputStream bin = new ByteArrayInputStream(content);
        bin.reset();

        // create content stream
        return new ContentStreamImpl(fileName, BigInteger.valueOf(content.length), mimeType, bin);
    }

    /**
     * Import CMIS types to JCR repository.
     * 
     * @param types CMIS types
     * @param typeManager JCR type manager
     * @param registry
     * @throws RepositoryException if there is a problem importing the types
     */
    private void importTypes( List<Tree<ObjectType>> types,
                              NodeTypeManager typeManager,
                              NamespaceRegistry registry ) throws RepositoryException {
        for (Tree<ObjectType> tree : types) {
            importType(tree.getItem(), typeManager, registry);
            importTypes(tree.getChildren(), typeManager, registry);
        }
    }

    /**
     * Import given CMIS type to the JCR repository.
     * 
     * @param cmisType cmis object type
     * @param typeManager JCR type manager/
     * @param registry
     * @throws RepositoryException if there is a problem importing the types
     */
    @SuppressWarnings( "unchecked" )
    public void importType( ObjectType cmisType,
                            NodeTypeManager typeManager,
                            NamespaceRegistry registry ) throws RepositoryException {
        // TODO: get namespace information and register
        // registry.registerNamespace(cmisType.getLocalNamespace(), cmisType.getLocalNamespace());

        // create node type template
        NodeTypeTemplate type = typeManager.createNodeTypeTemplate();

        // convert CMIS type's attributes to node type template we have just created
        type.setName(cmisType.getId());
        type.setAbstract(false);
        type.setMixin(false);
        type.setOrderableChildNodes(true);
        type.setQueryable(true);
        if (!cmisType.isBaseType()) {
            type.setDeclaredSuperTypeNames(superTypes(cmisType));
        }

        Map<String, PropertyDefinition<?>> props = cmisType.getPropertyDefinitions();
        Set<String> names = props.keySet();
        // properties
        for (String name : names) {
            PropertyDefinition<?> pd = props.get(name);
            PropertyDefinitionTemplate pt = typeManager.createPropertyDefinitionTemplate();
            pt.setRequiredType(properties.getJcrType(pd.getPropertyType()));
            pt.setAutoCreated(false);
            pt.setAvailableQueryOperators(new String[] {});
            pt.setName(name);
            pt.setMandatory(pd.isRequired());
            type.getPropertyDefinitionTemplates().add(pt);
        }

        // register type
        NodeTypeDefinition[] nodeDefs = new NodeTypeDefinition[] {type};
        typeManager.registerNodeTypes(nodeDefs, true);
    }

    /**
     * Determines supertypes for the given CMIS type in terms of JCR.
     * 
     * @param cmisType given CMIS type
     * @return supertypes in JCR lexicon.
     */
    private String[] superTypes( ObjectType cmisType ) {
        if (cmisType.getBaseTypeId() == BaseTypeId.CMIS_FOLDER) {
            return new String[] {JcrConstants.NT_FOLDER};
        }

        if (cmisType.getBaseTypeId() == BaseTypeId.CMIS_DOCUMENT) {
            return new String[] {JcrConstants.NT_FILE};
        }

        return new String[] {cmisType.getParentType().getId()};
    }

    /**
     * Defines node type for the repository info.
     * 
     * @param typeManager JCR node type manager.
     * @throws RepositoryException
     */
    @SuppressWarnings( "unchecked" )
    private void registerRepositoryInfoType( NodeTypeManager typeManager ) throws RepositoryException {
        // create node type template
        NodeTypeTemplate type = typeManager.createNodeTypeTemplate();

        // convert CMIS type's attributes to node type template we have just created
        type.setName("cmis:repository");
        type.setAbstract(false);
        type.setMixin(false);
        type.setOrderableChildNodes(true);
        type.setQueryable(true);
        type.setDeclaredSuperTypeNames(new String[] {JcrConstants.NT_FOLDER});

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

        // register type
        NodeTypeDefinition[] nodeDefs = new NodeTypeDefinition[] {type};
        typeManager.registerNodeTypes(nodeDefs, true);
    }    
}
