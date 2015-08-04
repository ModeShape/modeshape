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

import org.modeshape.web.client.repo.RepositoryHeader;
import org.modeshape.web.client.repo.RepositoriesList;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.client.contents.Contents;
import org.modeshape.web.shared.BaseCallback;
import org.modeshape.web.shared.RepositoryName;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Console implements EntryPoint, ValueChangeHandler<String> {
    private final static String LAYOUT_WIDTH = "85%";
    
    /**
     * Create a remote service proxy to talk to the server-side Greeting service.
     */
    private final JcrServiceAsync jcrService = GWT.create(JcrService.class);
    
    //Main form
    private final VLayout mainForm = new VLayout();

    //Browser's URL and history.
    private final JcrURL jcrURL = new JcrURL();
    private final HtmlHistory htmlHistory = new HtmlHistory();

    private final VLayout viewPort = new VLayout();        
    
    private final Header header = new Header(this);
    private final Footer footer = new Footer();
    
    private final RepositoriesList  repositoriesList = new RepositoriesList(this, jcrService);
    private final RepositoryHeader  repositoryHeader = new RepositoryHeader(this);    
    private final Contents contents = new Contents(this);
    
    private final LoadingIcon loadingIcon = new LoadingIcon();
    
    /**
     * Provides data access.
     * 
     * @return 
     */
    public JcrServiceAsync jcrService() {
        return this.jcrService;
    }
    
    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        //start from the requested URL
        jcrService.getRequestedURI(new BaseCallback<String>() {

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess( String result ) {
                //parse requested url to determine navigation
                jcrURL.parse(result);
  
                //before navigate to the requested URL we need to
                //check is this user already logged in or not yet.
                getCredentials();
            }
        });
    }

    /**
     * Checks user's credentials.
     */
    private void getCredentials() {
        jcrService.getUserName(new BaseCallback<String>() {
            @Override
            public void onSuccess(String name) {
                showMainForm(name);
            }
        });
    }
    
    /**
     * Reconstructs URL and points browser to the requested node path.
     */
    public void loadNodeSpecifiedByURL() {
        repositoriesList.select(jcrURL.getRepository(), jcrURL.getWorkspace(), jcrURL.getPath(), true);
    }

    /**
     * Displays name of the logged in user.
     * 
     * @param userName 
     */
    protected void changeUserName(String userName) {
        header.setUserName(userName);
    }
    
    /**
     * Gets selected repository name.
     * 
     * @return repository name;
     */
    public String repository() {
        return this.repositoryHeader.repository();
    }
    
    /**
     * Shows main page for the logged in user.
     * 
     * @param userName the name of the user.
     */
    public void showMainForm(String userName) {
        align();
        changeUserName(userName);

        mainForm.addMember(header); 
        mainForm.addMember(repositoryHeader);
        mainForm.addMember(viewPort);
        mainForm.addMember(strut(30));
        mainForm.addMember(footer);
           
        setLayoutWidth(LAYOUT_WIDTH);     
        loadData();
        
        //init HTML history
        htmlHistory.addValueChangeHandler(this);
        mainForm.draw();        
    }

    /**
     * Align components
     */
    private void align() {
        mainForm.setLayoutMargin(5);
        mainForm.setWidth100();
        mainForm.setHeight100();
        mainForm.setBackgroundColor("#FFFFFF");
        mainForm.setAlign(Alignment.CENTER);
        mainForm.setLayoutAlign(Alignment.CENTER);
        mainForm.setDefaultLayoutAlign(Alignment.CENTER);
        
        viewPort.setWidth(LAYOUT_WIDTH);
        viewPort.setLayoutAlign(Alignment.CENTER);
        viewPort.setAlign(Alignment.CENTER);
        viewPort.setDefaultLayoutAlign(Alignment.CENTER);
    }
    
    /**
     * Tests is initial URL points to the specific node.
     * 
     * @return true if URL points to the node.
     */
    private boolean isInitialUrlPointsNode() {
        return jcrURL.getRepository() != null && jcrURL.getRepository().length() > 0;
    }

    /**
     * Loads data.
     */
    private void loadData() {
        if (isInitialUrlPointsNode()) {
            loadNodeSpecifiedByURL();
        } else {
            loadRepositoriesList();
        }
    }
    
    /**
     * Load repositories available for the logged in user.
     */
    public void loadRepositoriesList() {
        repositoriesList.load();
    }
        
    /**
     * Synchronously aligns header and view port.
     * 
     * @param value the relative width;
     */
    private void setLayoutWidth(String value) {
        repositoryHeader.setLayoutWidth(value);
        viewPort.setWidth(value);
    }
    
    /**
     * 
     * @return 
     */
    public Contents contents() {
        return contents;
    }
    
    /**
     * Hides contents and shows "animated" load icon.
     */
    public void showLoadingIcon() {
        loadingIcon.show(mainForm.getWidth() / 2, mainForm.getHeight() / 2);
    }

    /**
     * Hides "animated" load icon and shows contents.
     */
    public void hideLoadingIcon() {
        loadingIcon.hide();
    }
    
    /**
     * Display repository header with given name.
     * 
     * @param name the name of the repository.
     */
    public void displayRepository(String name) {
        repositoryHeader.show(name);
    }
    
    /**
     * Hides repository header.
     */
    public void hideRepository() {
        repositoryHeader.hide();
    }
    
    /**
     * Changes repository name in URL displayed by browser.
     * 
     * @param name the name of the repository.
     * @param changeHistory if true store URL changes in history.
     */
    public void changeRepositoryInURL(String name, boolean changeHistory) {
        jcrURL.setRepository(name);
        if (changeHistory) {
            htmlHistory.newItem(jcrURL.toString(), false);
        }
    }

    /**
     * Changes workspace in the URL displayed by browser.
     * 
     * @param name the name of the repository.
     * @param changeHistory if true store URL changes in browser's history.
     */
    public void changeWorkspaceInURL(String name, boolean changeHistory) {
        jcrURL.setWorkspace(name);
        if (changeHistory) {
            htmlHistory.newItem(jcrURL.toString(), false);
        }
    }
    
    /**
     * Changes node path in the URL displayed by browser.
     * 
     * @param path the path to the node.
     * @param changeHistory if true store URL changes in browser's history.
     */
    public void changePathInURL(String path, boolean changeHistory) {
        jcrURL.setPath(path);
        if (changeHistory) {
            htmlHistory.newItem(jcrURL.toString(), false);
        }
    }

    @Override
    public void onValueChange(ValueChangeEvent<String> event) {
        jcrURL.parse2(event.getValue());
        repositoriesList.select(jcrURL.getRepository(), jcrURL.getWorkspace(), jcrURL.getPath(), false);
    }
    
    /**
     * Displays list of availables repositories.
     * 
     * @param names names of repositories.
     */
    public void showRepositories(Collection<RepositoryName> names) {
        repositoriesList.show(names);
        display(repositoriesList);
        this.hideRepository();
    }
  
    /**
     * Displays node for the given repository, workspace and path.
     * 
     * @param repository the name of the repository.
     * @param workspace the name of workspace.
     * @param path the path to the node.
     * @param changeHistory store changes in browser history.
     */
    public void displayContent(String repository, String workspace, String path,
            boolean changeHistory) {
        contents.show(repository, workspace, path, changeHistory);
        displayRepository(repository);
        display(contents);
        changeRepositoryInURL(repository, changeHistory);
    }
    
    /**
     * Displays root node for given repository and first available workspace.
     * 
     * @param repository the name of the repository.
     * @param changeHistory store changes in browser history or no.
     */
    public void displayContent(String repository, boolean changeHistory) {
        contents.show(repository, changeHistory);
        displayRepository(repository);
        display(contents);
        changeRepositoryInURL(repository, changeHistory);
    }
    
    public void display(Canvas view) {
        Canvas[] members = viewPort.getMembers();
        for (Canvas canvas : members) {
            canvas.setVisible(false);
            viewPort.removeChild(canvas);
        }
                
        viewPort.addMember(view);
        view.setVisible(true);
        view.show();
    }
    
    private VLayout strut( int height ) {
        VLayout s = new VLayout();
        s.setHeight(height);
        return s;
    }
        
}
