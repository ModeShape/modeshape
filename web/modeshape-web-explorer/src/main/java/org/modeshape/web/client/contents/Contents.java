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
package org.modeshape.web.client.contents;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Date;
import java.util.HashMap;
import org.modeshape.web.client.Console;
import org.modeshape.web.client.JcrServiceAsync;
import org.modeshape.web.shared.BaseCallback;
import org.modeshape.web.shared.RemoteException;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrPermission;
import org.modeshape.web.shared.JcrProperty;

/**
 * Displays contents.
 * 
 *  |--------------------------------|
 *  | session & workspace            |
 *  |--------------------------------|
 *  | path & buttons                 |
 *  |--------------------------------|
 *  | child nodes, binary & props    |
 *  |--------------------------------|
 * 
 * 
 * 
 * @author kulikov
 */
@SuppressWarnings("synthetic-access")
public class Contents extends VLayout {

    private final static String ROOT_PATH = "/";
    
    private Console console;
    
    //
    private String repository;
    private JcrNode node;
    private String path;
    
    private WorkspacePanel wsp = new WorkspacePanel(this);
    private PathControl pathLabel = new PathControl(this);
    
    //frames
    private ChildrenEditor childrenEditor = new ChildrenEditor(this);
    private PropertiesEditor propertiesEditor = new PropertiesEditor(this);
    private PermissionsEditor permissionsEditor = new PermissionsEditor(this);
    private BinaryEditor binaryEditor = new BinaryEditor();
    
    //layouts
    private DetailsLayout details = new DetailsLayout(propertiesEditor, permissionsEditor);
    private ContentsLayout contentsLayout = new ContentsLayout(childrenEditor, binaryEditor, details);
    
    //dialog
    private final AddNodeDialog addNodeDialog = new AddNodeDialog(this);
    private final ExportDialog exportDialog = new ExportDialog(this);
    private final ImportDialog importDialog = new ImportDialog(this);
    
    @SuppressWarnings("unchecked")
    private final HashMap<String, Session> sessions = new HashMap();

    /**
     * Creates contents instance.
     * 
     * @param console
     */
    public Contents(final Console console) {
        this.console = console;

        addMember(description());
        addMember(new Spacer(20));
        
        addMember(wsp);
        addMember(new Spacer(20));
        addMember(pathLabel);
        addMember(contentsLayout);
    }

    private Canvas description() {
        Canvas text = new Canvas();
        text.setContents("ModeShape is a distributed, hierarchical, transactional, and consistent data store with support for queries, full-text search, events, versioning, references, and flexible and dynamic schemas. It is very fast, highly available, extremely scalable, and it is 100% open source and written in Java. Clients use the JSR-283 standard Java API for content repositories (aka, JCR) or ModeShape's REST API, and can query content through JDBC and SQL.");
        text.setWidth100();
        text.setAutoHeight();
        text.setStyleName("caption");
        return text;
    }
    
    private Session session() {
        Session s = sessions.get(repository() + "$" + workspace());
        if (s == null) {
            s = new Session();
            sessions.put(repository() + "$" + workspace(), s);
        }
        return s;
    }
    
    /**
     * Expose interface to the server side.
     *
     * @return the service
     */
    public JcrServiceAsync jcrService() {
        return this.console.jcrService();
    }

    private void showLoadIcon() {
        pathLabel.setVisible(false);
        contentsLayout.setVisible(false);
        console.showLoadingIcon();
    }

    private void hideLoadIcon() {
        console.hideLoadingIcon();
        pathLabel.setVisible(true);
        contentsLayout.setVisible(true);
    }

    protected void toggleDetails() {
        contentsLayout.setShowDetails(!contentsLayout.showDetails());
    }
    
    /**
     * Displays root node of the specified workspace.
     *
     * @param name the name of the workspace.
     */
    public void changeWorkspace(final String name) {
        this.getAndDisplayNode(ROOT_PATH, true);
        updateControls();
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
        refreshWorkspacesAndReloadNode(null, ROOT_PATH, changeHistory);
    }

    /**
     * Shows nodes identified by repository, workspace and path to node.
     *
     * @param repository the name of the repository
     * @param workspace the name of the workspace
     * @param path the path to node
     * @param changeHistory true if this action should be reflected in browser
     * history.
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
     *
     * @param name the name to be selected.
     * @param path the path
     * @param changeHistory true if the history is to
     */
    private void refreshWorkspacesAndReloadNode(final String name, final String path,
            final boolean changeHistory) {
        showLoadIcon();
        console.jcrService().getWorkspaces(repository, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                hideLoadIcon();
                RemoteException e = (RemoteException) caught;
                SC.say(caught.getMessage());
                if (e.code() == RemoteException.SECURITY_ERROR) {
                    console.loadRepositoriesList();
                }
            }

            @Override
            public void onSuccess(String[] workspaces) {
                wsp.setWorkspaceNames(workspaces);
                getAndDisplayNode(path, changeHistory);
                hideLoadIcon();
            }
        });
    }

    /**
     * Reads node with given path and selected repository and workspace.
     *
     * @param path the path to the node.
     * @param changeHistory if true then path will be reflected in browser
     * history.
     */
    public void getAndDisplayNode(final String path, final boolean changeHistory) {
        showLoadIcon();
        console.jcrService().node(repository(), workspace(), path, new AsyncCallback<JcrNode>() {
            @Override
            public void onFailure(Throwable caught) {
                hideLoadIcon();
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(JcrNode node) {
                displayNode(node);
                console.changeWorkspaceInURL(workspace(), changeHistory);
                console.changePathInURL(path, changeHistory);
                hideLoadIcon();
            }
        });
    }

    /**
     * Displays specified node.
     *
     * @param node the node being displayed.
     */
    private void displayNode(JcrNode node) {
        this.node = node;
        this.path = node.getPath();

        pathLabel.display(node.getPath());

        //display childs, properties and ACLs
        childrenEditor.show(node);
        propertiesEditor.show(node);
        permissionsEditor.show(node);

        displayBinaryContent(node);

        //bring this page on top
//        console.display(Contents.this);
    }

    private void displayBinaryContent(JcrNode node) {
        //check for binary content
        binaryEditor.setVisible(false);
        for (JcrProperty property : node.getProperties()) {
            if (property.isBinary()) {
                binaryEditor.setVisible(true);
                binaryEditor.setValue(node, property.getName(), property.getValue());
            }
        }
    }

    /**
     * Save session's changes.
     */
    public void save() {
        SC.ask("Do you want to save changes", new BooleanCallback() {
            @Override
            public void execute(Boolean yesSelected) {
                if (yesSelected) {
                    jcrService().save(repository(), workspace(), new BaseCallback<Object>() {
                            @Override
                            public void onSuccess(Object result) {
                                session().setHasChanges(false);
                                updateControls();
                            }
                    });
                }
            }
        });
    }
    
    
    public void refreshSession(boolean keepChanges) {
        console.jcrService().refreshSession(repository(), workspace(), keepChanges, new AsyncCallback() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                getAndDisplayNode(path(), true);
            }
        });
    }

    /**
     * Prepares dialog for creating new node.
     *
     */
    public void showAddNodeDialog() {
        jcrService().getPrimaryTypes(node.getRepository(),
                node.getWorkspace(),
                null,
                false, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                addNodeDialog.setPrimaryTypes(result);
                addNodeDialog.showModal();
            }
        });
    }

    /**
     * Initiates export dialog
     */
    public void showExportDialog() {
        exportDialog.showModal();
    }

    /**
     * Shows import node dialog
     */
    public void showImportDialog() {
        importDialog.showModal();
    }

    /**
     * Exports contents to the given file.
     *
     * @param name the name of the file.
     * @param skipBinary
     * @param noRecurse
     */
    public void exportXML(String name, boolean skipBinary, boolean noRecurse) {
        console.jcrService().export(repository, workspace(), path(), name, true, true, new AsyncCallback<Object>() {
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
        console.jcrService().importXML(repository, workspace(), path(), name,
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

    public void removeNode(JcrNode node) {
        final String parent = parent(node.getPath());
        console.jcrService().removeNode(repository(), workspace(), node.getPath(), new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                getAndDisplayNode(parent, true);
                updateControls();
            }
        });
    }

    public void addMixin(String name) {
        console.jcrService().addMixin(repository(), workspace(), path(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                show();
                updateControls();
            }
        });
    }

    public void removeMixin(String name) {
        console.jcrService().removeMixin(repository(), workspace(), path(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                show();
                updateControls();
            }
        });
    }

    public void setNodeProperty(JcrNode node, String name, Boolean value) {
        console.jcrService().setProperty(node, name, value, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                show();
                updateControls();
            }
        });
    }

    public void setNodeProperty(JcrNode node, String name, Date value) {
        console.jcrService().setProperty(node, name, value, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                show();
                updateControls();
            }
        });
    }

    public void setNodeProperty(JcrNode node, String name, String value) {
        console.jcrService().setProperty(node, name, value, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                show();
                updateControls();
            }
        });
    }

    protected void addNode(String name, String primaryType) {
        console.jcrService().addNode(repository(), workspace(), path(), name, primaryType, new AsyncCallback<JcrNode>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(JcrNode node) {
                session().setHasChanges(true);
                getAndDisplayNode(path(), false);
                updateControls();
            }
        });
    }

    protected void renameNode(JcrNode node, String name) {
        console.jcrService().renameNode(repository(), workspace(), node.getPath(), name, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                getAndDisplayNode(path(), false);
                updateControls();
            }
        });
    }

    public void addAccessList(String name) {
        console.jcrService().addAccessList(repository(), workspace(), path(), name, new AsyncCallback() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                getAndDisplayNode(path(), false);
                updateControls();
            }
        });
    }

    public void updateAccessList(String principal, JcrPermission permission, boolean enabled) {
        console.jcrService().updateAccessList(repository, workspace(), path(), principal, permission, enabled, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                getAndDisplayNode(path(), false);
                updateControls();
            }
        });
    }

    public void removeAccessList(String name) {
        console.jcrService().removeAccessList(repository(), workspace(), path(), name, new AsyncCallback() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                session().setHasChanges(true);
                getAndDisplayNode(path(), false);
                updateControls();
            }
        });
    }

    public String repository() {
        return repository;
    }

    public String workspace() {
        return wsp.getSelectedWorkspace();
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

    private void updateControls() {
        wsp.setEnabled(session().hasChanges());
    }
    
    private class Spacer extends VLayout {
        public Spacer(int size) {
            super();
            setHeight(size);
        }
    }
}
