package org.modeshape.web.client;

import org.modeshape.web.shared.JcrNode;
import com.google.gwt.user.client.rpc.AsyncCallback;
import java.util.Collection;
import java.util.List;
import org.modeshape.web.shared.JcrProperty;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.ResultSet;

/**
 * The async counterpart of <code>GreetingService</code>.
 */
public interface JcrServiceAsync {
  public void login(String jndiName, String userName, String password,
          String workspace, AsyncCallback cb);
  public void childNodes(String path, AsyncCallback<List<JcrNode>> result);
  public void repositoryInfo(AsyncCallback<JcrRepositoryDescriptor> result);
  public void query(String text, String lang, AsyncCallback<ResultSet> result);
  public void supportedQueryLanguages(AsyncCallback<String[]> result);
}
