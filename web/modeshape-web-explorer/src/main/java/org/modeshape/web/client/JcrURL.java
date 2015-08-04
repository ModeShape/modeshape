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
        return path == null ? "/" : path;
    }
    
    /**
     * Modify node path.
     * 
     * @param path the new path value.
     */
    public void setPath(String path) {
        this.path = path;
    }
    
    /**
     * Parses given URL and extracts repository name, workspace and path.
     * 
     * @param uri URL started with http prefix.
     */
    public void parse2(String uri) {
        String path = uri;
        path = path.replace("http://", "");
        int pos = path.indexOf("/");
        path = path.substring(pos);
        parse(path);
    }
    
    /**
     * Parses given URL and extracts repository name, workspace and path.
     * 
     * @param uri URL started with prefix.
     */
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
    
    @Override
    public String toString() {
        return "/" + context + "/tree/" + repository + "/ws-" + workspace + path;
    }
}
