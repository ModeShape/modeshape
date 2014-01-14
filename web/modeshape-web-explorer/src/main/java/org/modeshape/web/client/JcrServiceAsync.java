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
import java.util.List;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface JcrServiceAsync {
  public void getRequestedURI(AsyncCallback<String> result);
  public void login(String jndiName, String userName, String password,
          String workspace, AsyncCallback<?> cb);
  public void getRootNode(AsyncCallback<JcrNode> result);
  public void childNodes(String path, AsyncCallback<List<JcrNode>> result);
  public void repositoryInfo(AsyncCallback<JcrRepositoryDescriptor> result);
  public void nodeTypes(AsyncCallback<Collection<JcrNodeType>> result);
  
  public void query(String text, String lang, AsyncCallback<ResultSet> result);
  public void supportedQueryLanguages(AsyncCallback<String[]> result);
  public void addNode(String path, String name, String primaryType, AsyncCallback<?> cb);
  public void removeNode(String path, AsyncCallback<?> cb);
  public void addMixin(String path, String mixin, AsyncCallback<?> cb);
  public void removeMixin(String path, String mixin, AsyncCallback<?> cb);
  public void setProperty(String path, String name, String value, AsyncCallback<?> cb);
  public void addAccessList(String path, String principal, AsyncCallback<?> cb);
  public void updateAccessList(String path, JcrAccessControlList acl, AsyncCallback<?> cb);
  public void updateAccessList(String path, String principal, JcrPermission[] permissions, AsyncCallback<?> cb);
  public void removeAccessList(String path, String principal, AsyncCallback<?> cb);
  public void getPrimaryTypes(boolean allowAbstract, AsyncCallback<String[]> cb);
  public void getMixinTypes(boolean allowAbstract, AsyncCallback<String[]> cb);
  public void save(AsyncCallback<?> cb);
}
