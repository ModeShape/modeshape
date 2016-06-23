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

import org.modeshape.web.shared.JcrNode;
import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.Collection;
import java.util.Date;
import org.modeshape.web.shared.BackupParams;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.RepositoryName;
import org.modeshape.web.shared.RestoreParams;
import org.modeshape.web.shared.ResultSet;
import org.modeshape.web.shared.Stats;

/**
 * The async counterpart of
 * <code>GreetingService</code>.
 */
public interface JcrServiceAsync {

    public void getRequestedURI(AsyncCallback<String> result);
    
    public void getRepositories(AsyncCallback<Collection<RepositoryName>> results);
    public void findRepositories(String criteria, AsyncCallback<Collection<RepositoryName>> results);
    public void getWorkspaces(String name, AsyncCallback<String[]> result);
    

    public void getUserName(AsyncCallback<String> cb);
    
    public void login(String userName, String password, AsyncCallback<?> cb);
    
    public void logout(AsyncCallback<String> cb);
    
    public void refreshSession(String repository, String workspace, boolean keepChanges, AsyncCallback<?> res);
        
    public void node(String repository, String workspace, String path, AsyncCallback<JcrNode> result);

    public void childNodes(String repository, String workspace, String path, int index, int count, AsyncCallback<Collection<JcrNode>> res);

    public void repositoryInfo(String repository, AsyncCallback<JcrRepositoryDescriptor> result);

    public void nodeTypes(String repository, String workspace, AsyncCallback<Collection<JcrNodeType>> result);

    public void query(String repository, String workspace, String text, String lang, AsyncCallback<ResultSet> result);

    public void supportedQueryLanguages(String repository, String worspace, AsyncCallback<String[]> result);

    public void addNode(String repository, String workspace, String path, String name, String primaryType, AsyncCallback<JcrNode> cb);

    public void removeNode(String repository, String workspace, String path, AsyncCallback<?> cb);

    public void renameNode(String repository, String workspace, String path, String name, AsyncCallback<?> cb);
    
    public void addMixin(String repository, String workspace, String path, String mixin, AsyncCallback<?> cb);

    public void removeMixin(String repository, String workspace, String path, String mixin, AsyncCallback<?> cb);

    public void setProperty(JcrNode node, String name, String value, AsyncCallback<?> cb);
    public void setProperty(JcrNode node, String name, Boolean value, AsyncCallback<?> cb);
    public void setProperty(JcrNode node, String name, Date value, AsyncCallback<?> cb);
    
    public void addAccessList(String repository, String workspace, String path, String principal, AsyncCallback<?> cb);
    public void removeAccessList( String repository, String workspace, String path, String principal, AsyncCallback<?> cb );
    public void updateAccessList(String repository, String workspace, String path, String principal,
            JcrPermission permission, boolean enabled, AsyncCallback<?> cb);

    public void getPrimaryTypes(String repository, String workspace, 
            String superType, boolean allowAbstract, AsyncCallback<String[]> cb);

    public void getMixinTypes(String repository, String workspace, boolean allowAbstract, AsyncCallback<String[]> cb);

    public void save(String repository, String workspace, AsyncCallback<?> cb);
    
    public void backup(String repositoy, String name, BackupParams params, AsyncCallback<?> cb);
    public void restore(String repositoy, String name, RestoreParams params, AsyncCallback<?> cb);
    
    public void export(String repository, String workspace, String path, String location, 
            boolean skipBinary, boolean noRecurse, AsyncCallback<?> cb) ;
    public void importXML(String repository, String workspace, String path, String location, 
            int option, AsyncCallback<?> cb) ;
    
    public void getValueStats(String repository, String param, String tu, AsyncCallback<Collection<Stats>> cb);
    public void getDurationStats(String repository, String param, String tu, AsyncCallback<Collection<Stats>> cb);
    
    public void getValueMetrics(AsyncCallback<String[]> cb);
    public void getDurationMetrics(AsyncCallback<String[]> cb);
    public void getTimeUnits(AsyncCallback<String[]> cb);
    
}
