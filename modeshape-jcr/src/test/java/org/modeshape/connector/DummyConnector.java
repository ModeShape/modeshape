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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import org.infinispan.schematic.document.Document;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.federation.spi.DocumentWriter;
import org.modeshape.jcr.federation.spi.ReadOnlyConnector;

/**
 * Connector to dummy external repository.
 * 
 * Used for testing procedure of exposing external content via workspace.
 * @author kulikov
 */
public class DummyConnector extends ReadOnlyConnector {

    private Repo repo = new Repo();
    
    public DummyConnector() {
        super();
    }
    
    @Override
    public Document getDocumentById(String id) {
        Doc obj = repo.getDocumentById(id);
        DocumentWriter writer = newDocument(obj.id);
        
        writer.setPrimaryType(obj.type);
        writer.setParent(obj.parentId);
        writer.addProperty("name", obj.name);
        writer.addProperty("path", obj.path);
        
        for (Doc child : obj.children) {
            writer.addChild(child.id, child.name);
        }
        return writer.document();
    }

    @Override
    public String getDocumentId(String path) {
        return repo.getDocumentByPath(path).id;
    }

    @Override
    public Collection<String> getDocumentPathsById(String id) {
        return Collections.unmodifiableSet(repo.getDocumentById(id).path);
    }

    @Override
    public boolean hasDocument(String id) {
        return repo.getDocumentById(id) != null;
    }
    
    private class Doc {
        private String id;
        private String name;    
        private String path;
        private String type;
        private String parentId;
        private ArrayList<Doc> children = new ArrayList();
        
        public Doc(String id, String name, String path, String type) {
            this.id = id;
            this.name = name;
            this.path = path;
            this.type = type;
        }
    }
    
    private class Repo {
        private Doc root = new Doc("0", "root", "/", "nt:folder");
        private HashMap<String, Doc> objectsById = new HashMap();
        private HashMap<String, Doc> objectsByPath = new HashMap();
        
        public Repo() {
            Doc folder1 = new Doc("1", "folder1", "/folder1", "nt:folder");
            Doc folder2 = new Doc("2", "folder2", "/folder2", "nt:folder");

            Doc obj1 = new Doc("3", "obj1", "/folder1/obj1", "nt:file");
            Doc obj2 = new Doc("4", "obj2", "/folder2/obj2", "nt:file");
            
            objectsById.put("0", root);
            objectsById.put("1", folder1);
            objectsById.put("2", folder2);
            objectsById.put("3", obj1);
            objectsById.put("4", obj1);

            objectsByPath.put("/", root);
            objectsByPath.put("/folder1", folder1);
            objectsByPath.put("/folder2", folder2);
            objectsByPath.put("/folder1/obj1", obj1);
            objectsByPath.put("/folder2/obj2", obj2);
            
            root.children.add(folder1);
            root.children.add(folder2);
            
            folder1.children.add(obj1);
            folder2.children.add(obj2);
            
            folder1.parentId = root.id;
            folder2.parentId = root.id;
            
            obj1.parentId = folder1.id;
            obj2.parentId = folder2.id;
        }
        
        public Doc getDocumentById(String id) {
            return objectsById.get(id);
        }
        
        public Doc getDocumentByPath(String path) {
            return objectsByPath.get(path);
        }
    }   
}
