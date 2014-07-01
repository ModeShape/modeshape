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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.RepositoryName;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Console implements EntryPoint {

    /**
     * Create a remote service proxy to talk to the server-side Greeting service.
     */
    protected final JcrServiceAsync jcrService = GWT.create(JcrService.class);

    private final VLayout mainForm = new VLayout();

    protected JcrURL jcrURL = new JcrURL();
    protected HtmlHistory htmlHistory = new HtmlHistory();

    private ToolBar toolBar;
    private RepositoryNameView repositoryNamePanel = new RepositoryNameView(this);
    private ViewPort viewPort;
    private RepositoriesView repos;
    private Contents contents;    
    private RepositoryInfo repositoryInfo = new RepositoryInfo(this, viewPort, null);    
    private NodeTypeView nodeTypes;  
    private QueryPanel queryView;
    
    private static Img loadingImg = new Img("loading.gif");    
    public static HLayout disabledHLayout = new HLayout();
        
    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        //start from the requested URL
        jcrService.getRequestedURI(new AsyncCallback<String>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say("Error" + caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess( String result ) {
                //parse requested url to determine navigation
                jcrURL.parse(result);
  
                //before navigate to the requested URL we need to
                //check is this user already logged in or not yet.
                testCredentials();
            }
        });
    }

    /**
     * Checks user's credentials.
     */
    private void testCredentials() {
        jcrService.getUserName(new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String name) {
                showMainForm(name);
            }
        });
    }
    
    /**
     * Reconstructs URL and points browser to the requested node path.
     */
    public void init() {
        String path = jcrURL.getPath();
        if (path == null) path = "/";
        repos.select(jcrURL.getRepository(), jcrURL.getWorkspace(), path);
    }

    protected void updateUserName(String userName) {
        toolBar.setUserName(userName);
    }
    
    public void showMainForm(String userName) {
        mainForm.setLayoutMargin(5);
        mainForm.setWidth100();
        mainForm.setHeight100();
        mainForm.setBackgroundColor("#FFFFFF");

        
        viewPort = new ViewPort(jcrService);        
        mainForm.setAlign(Alignment.CENTER);
        
        repos = new RepositoriesView(this, jcrService, viewPort);
        contents = new Contents(this, jcrService, viewPort);
        nodeTypes = new NodeTypeView(jcrService, viewPort);
        queryView = new QueryPanel(this, jcrService, viewPort);
        
        toolBar = new ToolBar(this);
        toolBar.setUserName(userName);
        
        mainForm.addMember(toolBar); 
        mainForm.addMember(repositoryNamePanel);
        mainForm.addMember(viewPort);
        
        
        mainForm.addMember(new VLayout());
        mainForm.addMember(new Strut(30));
        mainForm.addMember(new Footer());
   
        if (jcrURL.getRepository() != null && jcrURL.getRepository().length() > 0) {
            init();
        } else {
            repos.load();
        }
        
        mainForm.draw();        
    }

    public void save() {
        jcrService.save(contents.repository(), contents.workspace(), new AsyncCallback<Object>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
            }
        });
    }
    
    protected Contents contents() {
        return contents;
    }
    
    protected void showLoadingIcon() {
        disabledHLayout.setSize("100%", "100%");
        disabledHLayout.setStyleName("disabledBackgroundStyle");
        disabledHLayout.show();

        loadingImg.setSize("100px", "100px");
        loadingImg.setTop(mainForm.getHeight() / 2); //loading image height is 50px
        loadingImg.setLeft(mainForm.getWidth() / 2); //loading image width is 50px
        loadingImg.show();
        loadingImg.bringToFront();
    }

    protected void hideLoadingIcon() {
        loadingImg.hide();
        disabledHLayout.hide();
    }
    
    protected void showRepo(String name) {
        repositoryNamePanel.show(name);
    }
    
    protected void hideRepo() {
        repositoryNamePanel.hide();
    }
    
    public void updateRepository(String repository) {
        jcrURL.setRepository(repository);
        htmlHistory.newItem(jcrURL.toString(), false);
    }

    public void updateWorkspace(String workspace) {
        jcrURL.setWorkspace(workspace);
        htmlHistory.newItem(jcrURL.toString(), false);
    }
    
    public void updatePath(String path) {
        jcrURL.setPath(path);
        htmlHistory.newItem(jcrURL.toString(), false);
    }
    
    private class Strut extends VLayout {
        public Strut(int size) {
            super();
            setHeight(size);
        }
    }
    
    public void showRepositoryInfo() {
        this.showLoadingIcon();
        jcrService.repositoryInfo(repos.getSelected(), new AsyncCallback<JcrRepositoryDescriptor>() {

            @Override
            public void onFailure(Throwable caught) {
                hideLoadingIcon();
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess(JcrRepositoryDescriptor result) {
                hideLoadingIcon();
                repositoryInfo.show(result);
                viewPort.display(repositoryInfo);
            }
        });
    }

    public void showRepositories() {
        repos.load();
    }

    public void showRepositories(Collection<RepositoryName> names) {
        repos.show(names);
        viewPort.display(repos);
        this.hideRepo();
    }
    
    public void showNodeTypes() {
        nodeTypes.show(repos.getSelected());
    }
    
    public void showContent() {
        init();
    }
     
    public void showQuery() {
        queryView.init();
    }
    
    protected void showContent(String repository, String workspace, String path) {
        contents.select(repository, workspace, path);
//        contents.select(workspace, path);
        showRepo(repository);
        viewPort.display(contents);
        updateRepository(repository);
    }
    
    protected void showContent(String repository) {
        contents.show(repository);
        showRepo(repository);
        viewPort.display(contents);
        updateRepository(repository);
    }
    
    protected void display(View view) {
        viewPort.display(view);
    }
}
