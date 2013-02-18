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
package org.modeshape.connector.cmis.test;

import java.util.*;
import org.apache.chemistry.opencmis.client.api.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.data.Ace;
import org.apache.chemistry.opencmis.commons.data.Acl;
import org.apache.chemistry.opencmis.commons.data.ContentStream;
import org.apache.chemistry.opencmis.commons.data.RepositoryInfo;
import org.apache.chemistry.opencmis.commons.enums.AclPropagation;
import org.apache.chemistry.opencmis.commons.enums.IncludeRelationships;
import org.apache.chemistry.opencmis.commons.enums.RelationshipDirection;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.apache.chemistry.opencmis.commons.spi.CmisBinding;
import org.junit.*;


/**
 *
 * @author kulikov
 */
public class SessionImpl implements Session {

    private CmisRepository repository;

    public SessionImpl(CmisRepository repository) {
        this.repository = repository;
    }

//    public SessionImpl() {
//        repositoryInfo = new RepositoryInfoImpl();
//        this.createFolder(null, this.createObjectId("/home"));
//        this.createFolder(null, this.createObjectId("/home/kulikov"));
//    }
    
    @Override
    public void clear() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisBinding getBinding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OperationContext getDefaultContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setDefaultContext(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OperationContext createOperationContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public OperationContext createOperationContext(Set<String> set, boolean bln, boolean bln1, boolean bln2, IncludeRelationships ir, Set<String> set1, boolean bln3, String string, boolean bln4, int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createObjectId(String path) {
        return new ObjectIdImpl(path);
    }

    @Override
    public Locale getLocale() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public RepositoryInfo getRepositoryInfo() {
        return repository.getRepositoryInfo();
    }

    @Override
    public ObjectFactory getObjectFactory() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectType getTypeDefinition(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<ObjectType> getTypeChildren(String string, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Tree<ObjectType>> getTypeDescendants(String string, int i, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Folder getRootFolder() {
        return repository.getRootFolder();
    }

    @Override
    public Folder getRootFolder(OperationContext oc) {
        return repository.getRootFolder();
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<Document> getCheckedOutDocs(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public CmisObject getObject(ObjectId id) {
        return (CmisObject) repository.getObjectById(id.getId());
    }

    @Override
    public CmisObject getObject(ObjectId id, OperationContext oc) {
        return (CmisObject) repository.getObjectById(id.getId());
    }

    @Override
    public CmisObject getObject(String id) {
        return (CmisObject) repository.getObjectById(id);
    }

    @Override
    public CmisObject getObject(String id, OperationContext oc) {
        return (CmisObject) repository.getObjectById(id);
    }

    @Override
    public CmisObject getObjectByPath(String path) {
        return repository.find(path);
    }

    @Override
    public CmisObject getObjectByPath(String path, OperationContext oc) {
        return repository.find(path);
    }

    @Override
    public void removeObjectFromCache(ObjectId oi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeObjectFromCache(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<QueryResult> query(String string, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<QueryResult> query(String string, boolean bln, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<CmisObject> queryObjects(String string, String string1, boolean bln, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public QueryStatement createQueryStatement(String string) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChangeEvents getContentChanges(String string, boolean bln, long l) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ChangeEvents getContentChanges(String string, boolean bln, long l, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createDocument(Map<String, ?> map, ObjectId oi, ContentStream stream, VersioningState vs, List<Policy> list, List<Ace> list1, List<Ace> list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createDocument(Map<String, ?> params, ObjectId id, ContentStream stream, VersioningState vs) {
        Map<String, Object> p = new HashMap();
        p.putAll(params);
        return repository.createDocument(p, stream);
    }

    @Override
    public ObjectId createDocumentFromSource(ObjectId oi, Map<String, ?> map, ObjectId oi1, VersioningState vs, List<Policy> list, List<Ace> list1, List<Ace> list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createDocumentFromSource(ObjectId oi, Map<String, ?> map, ObjectId oi1, VersioningState vs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createFolder(Map<String, ?> map, ObjectId oi, List<Policy> list, List<Ace> list1, List<Ace> list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createFolder(Map<String, ?> params, ObjectId id) {
        String path = (String) params.get(PropertyIds.PATH);

        if (path == null) {
            throw new IllegalArgumentException("Path not specified");
        }

        return repository.addFolder(id, path);
    }

    @Override
    public ObjectId createPolicy(Map<String, ?> map, ObjectId oi, List<Policy> list, List<Ace> list1, List<Ace> list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createPolicy(Map<String, ?> map, ObjectId oi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createRelationship(Map<String, ?> map, List<Policy> list, List<Ace> list1, List<Ace> list2) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ObjectId createRelationship(Map<String, ?> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<Relationship> getRelationships(ObjectId oi, boolean bln, RelationshipDirection rd, ObjectType ot, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl getAcl(ObjectId oi, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Acl applyAcl(ObjectId oi, List<Ace> list, List<Ace> list1, AclPropagation ap) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void applyPolicy(ObjectId oi, ObjectId... ois) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removePolicy(ObjectId oi, ObjectId... ois) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    

}
