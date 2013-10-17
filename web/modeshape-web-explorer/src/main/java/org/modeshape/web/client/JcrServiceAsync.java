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
          String workspace, AsyncCallback cb);
  public void getRootNode(AsyncCallback<JcrNode> result);
  public void childNodes(String path, AsyncCallback<List<JcrNode>> result);
  public void repositoryInfo(AsyncCallback<JcrRepositoryDescriptor> result);
  public void nodeTypes(AsyncCallback<Collection<JcrNodeType>> result);
  
  public void query(String text, String lang, AsyncCallback<ResultSet> result);
  public void supportedQueryLanguages(AsyncCallback<String[]> result);
  public void addNode(String path, String name, String primaryType, AsyncCallback cb);
  public void removeNode(String path, AsyncCallback cb);
  public void addMixin(String path, String mixin, AsyncCallback cb);
  public void removeMixin(String path, String mixin, AsyncCallback cb);
  public void setProperty(String path, String name, String value, AsyncCallback cb);
  public void addAccessList(String path, String principal, AsyncCallback cb);
  public void updateAccessList(String path, JcrAccessControlList acl, AsyncCallback cb);
  public void updateAccessList(String path, String principal, JcrPermission[] permissions, AsyncCallback cb);
  public void removeAccessList(String path, String principal, AsyncCallback cb);
  public void getPrimaryTypes(boolean allowAbstract, AsyncCallback<String[]> cb);
  public void getMixinTypes(boolean allowAbstract, AsyncCallback<String[]> cb);
  public void save(AsyncCallback cb);
}
