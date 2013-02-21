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
import org.apache.chemistry.opencmis.commons.data.*;
import org.apache.chemistry.opencmis.commons.enums.*;

/**
 *
 * @author kulikov
 */
public class FolderImpl extends CmisObjectImpl implements Folder {

    private ArrayList<FolderImpl> folders = new ArrayList();
    private ArrayList<DocumentImpl> documents = new ArrayList();

    protected Folder parent;

    private CmisRepository repository;

    public FolderImpl(CmisRepository repository, Folder parent, Map<String, Object> params) {
        super(params);
        this.parent = parent;
        this.repository = repository;

        String parentId = (String) params.get(PropertyIds.PARENT_ID);
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:baseTypeId", "Base Type Id", "cmis:baseTypeId", "cmis:baseTypeId", "cmis:folder"));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:objectTypeId", "Object Type Id", "cmis:objectTypeId", "cmis:objectTypeId", "cmis:folder"));
        properties.add(new PropertyImpl(PropertyType.STRING,
                "cmis:parentId", "Patent Id", "cmis:parentId", "cmis:parentId", parentId));

        repository.map.put(this.getId(), this);
    }

    private String name(String path) {
        if (path.equals("/")) {
            return "/";
        }
        String[] tokens = path.split("/");
        return tokens[tokens.length - 1];
    }


    @Override
    public TransientFolder getTransientFolder() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document createDocument(Map<String, ?> params, ContentStream stream, VersioningState vs, List<Policy> list, List<Ace> list1, List<Ace> list2, OperationContext oc) {
        DocumentImpl doc = new DocumentImpl(repository, this, params);

        if (stream != null) {
            doc.setContentStream(stream, true);
        }

        this.add(doc);
        return doc;
    }

    @Override
    public Document createDocument(Map<String, ?> params, ContentStream stream, VersioningState vs) {
        DocumentImpl doc = new DocumentImpl(repository, this, params);

        if (stream != null) {
            doc.setContentStream(stream, true);
        }

        this.add(doc);
        return doc;
    }

    @Override
    public Document createDocumentFromSource(ObjectId oi, Map<String, ?> map, VersioningState vs, List<Policy> list, List<Ace> list1, List<Ace> list2, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Document createDocumentFromSource(ObjectId oi, Map<String, ?> map, VersioningState vs) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Folder createFolder(Map<String, ?> map, List<Policy> list, List<Ace> list1, List<Ace> list2, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Folder createFolder(Map<String, ?> params) {
        HashMap<String, Object> p = new HashMap();
        p.putAll(params);
        FolderImpl f = new FolderImpl(repository, this, p);
        return f;
    }

    @Override
    public Policy createPolicy(Map<String, ?> map, List<Policy> list, List<Ace> list1, List<Ace> list2, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Policy createPolicy(Map<String, ?> map) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> deleteTree(boolean bln, UnfileObject uo, boolean bln1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Tree<FileableCmisObject>> getFolderTree(int i, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int i) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Tree<FileableCmisObject>> getDescendants(int i, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public ItemIterable<CmisObject> getChildren() {
        return new ItemIterableImpl(folders, documents);
    }

    @Override
    public ItemIterable<CmisObject> getChildren(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isRootFolder() {
        return path().equals("/");
    }

    @Override
    public Folder getFolderParent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPath() {
        return path();
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
    public FileableCmisObject move(ObjectId oi, ObjectId oi1) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FileableCmisObject move(ObjectId oi, ObjectId oi1, OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Folder> getParents() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Folder> getParents(OperationContext oc) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<String> getPaths() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void addToFolder(ObjectId oi, boolean bln) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeFromFolder(ObjectId oi) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public TransientCmisObject getTransientObject() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getParentId() {
        return parent != null ? parent.getId() : "/";
    }

    @Override
    public List<ObjectType> getAllowedChildObjectTypes() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public CmisObject find(String path) {
        for (FolderImpl f : folders) {
            if (f.path().equals(path)) {
                return f;
            }
        }

        for (DocumentImpl d : documents) {
            if (d.path().equals(path)) {
                return d;
            }
        }
        
        return null;
    }

    protected void add(FolderImpl folder) {
        folders.add(folder);
    }

    protected void add(DocumentImpl document) {
        documents.add(document);
    }
    
    @Override
    public String toString() {
        return this.path();
    }
}
