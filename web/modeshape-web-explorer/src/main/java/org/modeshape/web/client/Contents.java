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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Date;
import org.modeshape.web.client.grid.AccessList;
import org.modeshape.web.client.grid.Children;
import org.modeshape.web.client.grid.Properties;
import org.modeshape.web.client.grid.TabGrid;
import org.modeshape.web.client.grid.TabsetGrid;
import org.modeshape.web.shared.Acl;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.Policy;

/**
 *
 * @author kulikov
 */
@SuppressWarnings( "synthetic-access" )
public class Contents extends View {
    public final static int CHILDREN_PAGE = 0;
    public final static int PROPERTIES_PAGE = 1;
    public final static int ACL_PAGE = 2;
    
    private final static String ROOT_PATH = "/";
    
    private Console console;
    private JcrServiceAsync jcrService;
    private String repository;
    
    private JcrNode node;
    private String path;
    
    private ComboBoxItem workspaceComboBox = new ComboBoxItem();
    private PathLabel pathLabel = new PathLabel();
    
    private Children    children  = new Children(this);
    private Properties properties = new Properties(this);
    private AccessList accessList = new AccessList(this);
    
    private TabsetGrid mainGrid = new TabsetGrid(
            new String[]{"Children", "Properties", "Access list"},
            new TabGrid[]{children, properties, accessList}
            );
    
    private Button saveButton;
    
    public Contents(final Console console, final JcrServiceAsync jcrService, ViewPort viewPort) {
        super(viewPort, null);
        this.console = console;
        this.jcrService = jcrService;
        
        
        Canvas text = new Canvas();
        text.setContents("ModeShape is a distributed, hierarchical, transactional, and consistent data store with support for queries, full-text search, events, versioning, references, and flexible and dynamic schemas. It is very fast, highly available, extremely scalable, and it is 100% open source and written in Java. Clients use the JSR-283 standard Java API for content repositories (aka, JCR) or ModeShape's REST API, and can query content through JDBC and SQL.");
        text.setWidth100();
        text.setAutoHeight();
        text.setStyleName("caption");
        
        addMember(text);
        addMember(new Spacer(20));

        final DynamicForm form = new DynamicForm();
        form.setFields(workspaceComboBox);
        
        workspaceComboBox.setTitle("Workspace");
        workspaceComboBox.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                show(repository(), (String)event.getValue(), ROOT_PATH, true);
            }
        });
        
        VLayout wsPanel = new VLayout();
        wsPanel.addMember(form);
        wsPanel.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        wsPanel.setLayoutAlign(VerticalAlignment.CENTER);
        
        HLayout panel = new HLayout();
        panel.addMember(wsPanel);
        panel.setStyleName("viewport");
        panel.setHeight(45);
        panel.setLayoutAlign(VerticalAlignment.CENTER);
        panel.setDefaultLayoutAlign(VerticalAlignment.CENTER);

        HLayout buttonPanel = new HLayout();
        buttonPanel.setWidth100();
        
        saveButton = new Button();
        saveButton.disable();
        saveButton.setTitle("Save");
        saveButton.setIcon("icons/save.png");
        saveButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.save();
                saveButton.disable();
            }
        });
        
        
        panel.addMember(buttonPanel);        
        addMember(panel);

        panel.addMember(saveButton);
        
        HLayout spacer = new HLayout();
        spacer.setWidth(5);
        
        panel.addMember(spacer);
        
        VLayout strut = new VLayout();
        strut.setHeight(20);

        addMember(strut);
        addMember(pathLabel);
        addMember(mainGrid);        
    }

    /**
     * Expose interface to the server side.
     * 
     * @return the service
     */
    public JcrServiceAsync jcrService() {
        return this.jcrService;
    }
    
    private void showLoadIcon() {
        pathLabel.setVisible(false);
        mainGrid.setVisible(false);
        console.showLoadingIcon();
    }
    
    private void hideLoadIcon() {
        pathLabel.setVisible(true);
        mainGrid.setVisible(true);
        console.hideLoadingIcon();
    }
    
    /**
     * Shows content of the root node of the first reachable workspace of the 
     * given repository.
     * 
     * @param repository the name of the given repository.
     * @param changeHistory if true then this action of navigation will be 
     * reflected in the browser URL and will don't touch URL in case of false 
     * value.
     */
    public void show(String repository, final boolean changeHistory) {
        this.repository = repository;
        this.refreshWorkspacesAndReloadNode(null, ROOT_PATH, changeHistory);
    }

    /**
     * Shows nodes identified by repository, workspace and path to node.
     * 
     * @param repository the name of the repository
     * @param workspace the name of the workspace
     * @param path the path to node
     * @param changeHistory true if this action should be reflected in browser history.
     */
    public void show(final String repository, final String workspace, 
            final String path, final boolean changeHistory) {
        this.repository = repository;
        this.refreshWorkspacesAndReloadNode(null, path, changeHistory);
    }
    
    /**
     * Reloads values of the combo box with workspace names.
     * 
     * Gets values from server side, assigns to combo box and select given name.
     * @param name the name to be selected. 
     * @param path the path
     * @param changeHistory true if the history is to 
     */
    private void refreshWorkspacesAndReloadNode(final String name, final String path, 
            final boolean changeHistory) {
        showLoadIcon();
        jcrService.getWorkspaces(repository, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                hideLoadIcon();
                RemoteException e = (RemoteException) caught;
                SC.say(caught.getMessage());
                if (e.code() == RemoteException.SECURITY_ERROR) {
                    console.showRepositories();
                }
            }

            @Override
            public void onSuccess(String[] workspaces) {
                workspaceComboBox.setValueMap(workspaces);
                if (name != null) {
                    workspaceComboBox.setValue(name);
                } else if (workspaces.length > 0) {
                    workspaceComboBox.setValue(workspaces[0]);
                }
                
                getAndDisplayNode(path, changeHistory);
                hideLoadIcon();
            }
        });        
    }
    
    /**
     * Reads node with given path and selected repository and workspace.
     * 
     * @param path the path to the node.
     * @param changeHistory if true then path will be reflected in browser history.
     */
    public void getAndDisplayNode(final String path, final boolean changeHistory) {
        showLoadIcon();
        jcrService.node(repository(), workspace(), path, new AsyncCallback<JcrNode>() {
            @Override
            public void onFailure(Throwable caught) {
                hideLoadIcon();
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(JcrNode node) {
                displayNode(node, CHILDREN_PAGE);
                console.updateWorkspace(workspace(), changeHistory);
                console.updatePath(path, changeHistory);
                hideLoadIcon();
            }
        });
    }
    
    
    /**
     * Displays specified node.
     * 
     * @param node the node being displayed.
     * @param page the page number
     */
    private void displayNode(JcrNode node, int page) {
        this.node = node;
        this.path = node.getPath();
        pathLabel.display(node.getPath());
        
        //display childs, properties and ACLs
        children.show(node);
        properties.show(node);
        accessList.show(node);
        
        //bring this page on top
        viewPort().display(Contents.this);
        mainGrid.showTab(page);
    }

    /**
     * Exports contents to the given file.
     * 
     * @param name the name of the file.
     * @param skipBinary
     * @param noRecurse 
     */
    public void export(String name, boolean skipBinary, boolean noRecurse) {
        jcrService.export(repository, workspace(), path(), name, true, true, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                SC.say("Complete");
            }
        });
    }

    /**
     * Imports contents from the given file.
     * 
     * @param name
     * @param option 
     */
    public void importXML(String name, int option) {
        jcrService.importXML(repository, workspace(), path(), name, 
                option, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                SC.say("Complete");
            }
        });
    }
    
    
    public void removeNode() {
        final String parent = parent(path());
        jcrService.removeNode(repository(), workspace(), path(), new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                saveButton.enable();
                getAndDisplayNode(parent, true);
            }
        });
    }

    public void addMixin(String name) {
        jcrService.addMixin(repository(), workspace(), path(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }
    
    public void removeMixin(String name) {
        jcrService.removeMixin(repository(), workspace(), path(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }
    
    public void setNodeProperty(JcrNode node, String name, Boolean value) {
        jcrService.setProperty(node, name, value, new AsyncCallback<Object>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }
    
    public void setNodeProperty(JcrNode node, String name, Date value) {
        jcrService.setProperty(node, name, value, new AsyncCallback<Object>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }

    public void setNodeProperty(JcrNode node, String name, String value) {
        jcrService.setProperty(node, name, value, new AsyncCallback<Object>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }
    
    protected void addNode(String name, String primaryType) {
        jcrService.addNode(repository(), workspace(), path(), name, primaryType, new AsyncCallback<JcrNode>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(JcrNode node) {
                displayNode(node, PROPERTIES_PAGE);
                saveButton.enable();
            }
        });
    }
    
    protected void renameNode(String name) {
        jcrService.renameNode(repository(), workspace(), path(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                getAndDisplayNode(path(), false);
                saveButton.enable();
            }
        });
    }
        
    public void addAccessList(String name) {
        Acl acl = node.getAcl();
        if (acl == null) {
            acl = new Acl();
            node.setAcl(acl);
        }
        
        Policy policy = new Policy();
        policy.setPrincipal(name);
        policy.add(JcrPermission.ALL);
        
        acl.addPolicy(policy);
        accessList.show();
        saveButton.enable();
    }

    public void updateAccessList(String principal, JcrPermission permission, boolean enabled) {
        jcrService.updateAccessList(repository, workspace(), path(), principal, permission, enabled, new AsyncCallback<Object>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }
    
    public void removeAccessList(String name) {
        node.getAccessList().remove(name);
        accessList.show();
    }
    
    public void applyAccessList() {
        jcrService.updateAccessList(repository(), workspace(), path(), node.getAccessList(), new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
                saveButton.enable();
            }
        });
    }
    
    public String repository() {
        return repository;
    }

    public String workspace() {
        return workspaceComboBox.getValueAsString();
    }

    public JcrNode node() {
        return node;
    }

    public String path() {
        return path == null ? "/" : path;
    }

    private String parent(String path) {
        if (path == null) {
            return "/";
        }

        path = path.substring(0, path.lastIndexOf('/'));
        if (path.length() == 0) {
            return "/";
        }

        return path;
    }
    
    private class PathLabel extends HLayout {

        private Label[] segments = new Label[50];
        private Label[] separators = new Label[50];

        public PathLabel() {
            super();
            setHeight(30);

            Label path = new Label();
            path.setAutoWidth();
            path.setContents("<b>Node:</b>");
            path.setStyleName("text");
            
            addMember(path);


            for (int i = 0; i < segments.length; i++) {
                segments[i] = new Label();
                segments[i].setAutoWidth();
                segments[i].setStyleName("segment");
                segments[i].addClickHandler(new ClickHandler() {
                    @Override
                    public void onClick(ClickEvent event) {
                        Label label = (Label) event.getSource();
                        getAndDisplayNode(label.getDataPath(), true);
                    }
                });
                
                separators[i] = new Label();
                separators[i].setContents("/");
                separators[i].setVisible(false);
                separators[i].setAutoWidth();
                separators[i].setStyleName("segment-separator");
                separators[i].setMargin(3);
                
                addMember(segments[i]);
                addMember(separators[i]);
            }
        }

        public void display(String url) {
            for (int i = 0; i < segments.length; i++) {
                segments[i].setContents("");
                separators[i].setVisible(false);
            }

            String[] tokens = url.split("/");
            if (tokens.length == 0) {
                tokens = new String[]{"/"};
            }

            for (int i = 0; i < tokens.length; i++) {
//                String s = i >= 1 ? "/" + tokens[i] : tokens[i];

                segments[i].setContents(tokens[i]);

                String path = "";
                for (int j = 0; j <= i; j++) {
                    path += ("/" + segments[j].getContents());
                }

                segments[i].setTooltip(path);
                segments[i].setDataPath(path);
                segments[i].draw();
                
                if (i < tokens.length - 1) {
                    separators[i].setVisible(true);
                }
            }
        }
    }

    private class Spacer extends VLayout  {
        public Spacer(int size) {
            super();
            setHeight(size);
        }
    }
    
}
