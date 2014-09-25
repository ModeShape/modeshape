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
package org.modeshape.web.client;

import java.io.Serializable;

/**
 * Represents fully qualified node path including repository name and workspace.
 * 
 * @author kulikov
 */
public class JcrURL implements Serializable {
    private static final long serialVersionUID = 1L;
    private String context;
    private String repository;
    private String workspace;
    private String path;
    
    public void parse2(String uri) {
        String path = uri;
        path = path.replace("http://", "");
        int pos = path.indexOf("/");
        path = path.substring(pos);
        parse(path);
    }
    
    public void parse(String uri) {
        if (uri.startsWith("/")) {
            uri = uri.substring(1);
        }
        
        int pos = uri.indexOf("/");
        if (pos == -1) {
            context = uri;
            repository = "";
            workspace = "";
            path="/";
            return;
        }
        
        if (pos > 0) {
            context = uri.substring(0, pos);
            uri = uri.substring(pos + 1);
        }
        
        //expecting tree here
         uri = uri.substring(5);
        
         pos = uri.indexOf("/ws-");
         repository = uri.substring(0, pos);
         
         uri = uri.substring(repository.length() + 1);
         pos = uri.indexOf("/");
         
         workspace = uri.substring(3, pos);
         path = uri.substring(workspace.length() + 3);
    }
    
    public JcrURL() {
    }
    
    /**
     * Creates locator.
     * 
     * @param repository the jndi name of the repository.
     * @param workspace the name of the workspace.
     * @param path path to the node;
     */
    public JcrURL(String repository, String workspace, String path) {
        this.repository = repository;
        this.workspace = workspace;
        this.path = path;
    } 
    
    public String getContext() {
        return context;
    }
    
    /**
     * Gets repository name encoded in this locator.
     * 
     * @return the name of repository.
     */
    public String getRepository() {
        return repository;
    }
    
    public void setRepository(String repository) {
        this.repository = repository;
    }
    /**
     * Gets workspace name encoded in this locator.
     * 
     * @return the name of workspace.
     */
    public String getWorkspace() {
        return workspace;
    }
    
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }
    
    /**
     * Gets node path encoded in this locator.
     * 
     * @return the path of the node
     */
    public String getPath() {
        return path;
    }
    
    /**
     * Modify node path.
     * 
     * @param path the new path value.
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    @Override
    public String toString() {
        return "/" + context + "/tree/" + repository + "/ws-" + workspace + path;
    }
}
