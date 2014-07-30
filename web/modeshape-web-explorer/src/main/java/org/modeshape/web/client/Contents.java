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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.FormMethod;
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

    private Console console;
    private JcrServiceAsync jcrService;
    private String repository;
    
    private JcrNode node;
    private String path;
    
    private ComboBoxItem workspaces = new ComboBoxItem();
    private PathLabel pathLabel = new PathLabel();
    
    private Children children = new Children(this);
    private Properties properties = new Properties(this);
    private AccessList accessList = new AccessList(this);
    
    private TabsetGrid mainGrid = new TabsetGrid(new String[]{"Children", "Properties", "Access list"},
            new TabGrid[]{children, properties, accessList});
    
    private NewNodeDialog newNodeDialog = new NewNodeDialog(this);
    private RenameNodeDialog renameNodeDialog = new RenameNodeDialog(this);
    private AddMixinDialog addMixinDialog = new AddMixinDialog(this);
    private RemoveMixinDialog removeMixinDialog = new RemoveMixinDialog(this);
    private AddPropertyDialog setPropertyDialog = new AddPropertyDialog(this);
    private AddPrincipalDialog addAccessListDialog = new AddPrincipalDialog(this);
    private BackupDialog backupDialog = new BackupDialog(this);
    private RestoreDialog restoreDialog = new RestoreDialog(this);
    private ExportDialog exportDialog = new ExportDialog(this);
    private ImportDialog importDialog = new ImportDialog(this);
    
    public Contents(Console console, final JcrServiceAsync jcrService, ViewPort viewPort) {
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
        form.setFields(workspaces);
        
        workspaces.setTitle("Workspace");
        workspaces.addChangedHandler(new ChangedHandler() {
            @Override
            public void onChanged(ChangedEvent event) {
                select(repository(), (String)event.getValue(), path(), true);
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
        
        Button backup = new Button("Backup");
        backup.setValign(VerticalAlignment.CENTER);
        backup.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                backupDialog.showModal();
            }
        });
        
        Button restore = new Button("Restore");
        restore.setValign(VerticalAlignment.CENTER);
        restore.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                restoreDialog.showModal();
            }
        });

        Button importButton = new Button("Download");
        importButton.setTooltip("Backup repository and download");
        importButton.setValign(VerticalAlignment.CENTER);
        importButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                jcrService.backup(repository(), workspace(), "zzz", new AsyncCallback<Object>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Object result) {
                       form.setAction(GWT.getModuleBaseForStaticFiles() + "backup/do?file=zzz");
                       form.setMethod(FormMethod.GET);
                       form.submitForm();
                    }
                });
            }
        });

        panel.addMember(buttonPanel);
        panel.addMember(backup);
        panel.addMember(new Strut(5));
        panel.addMember(restore);
        panel.addMember(new Strut(5));
        panel.addMember(importButton);
        
        addMember(panel);

        VLayout strut = new VLayout();
        strut.setHeight(20);

        addMember(strut);
        addMember(pathLabel);
        addMember(mainGrid);
/*        addMember(children);
        addMember(new Spacer(20));
        addMember(properties);
        addMember(new Spacer(20));
        addMember(accessList);
        */ 
    }

    public void show(String repository, final boolean changeHistory) {
        this.repository = repository;
        jcrService.getWorkspaces(repository, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                workspaces.setValueMap(result);
                if (result.length > 0) {
                    workspaces.setValue(result[0]);
                }
                select(path(), changeHistory);
            }
        });
    }

    public void select(final String repository, final String workspace, 
            final String path, final boolean changeHistory) {
        this.repository = repository;
        jcrService.getWorkspaces(repository, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                workspaces.setValueMap(result);
                workspaces.setValue(workspace);
                select(path, changeHistory);
            }
        });        
    }
    
    public void select(final String path, final boolean changeHistory) {
        console.showLoadingIcon();
        this.path = path;
        jcrService.node(repository(), workspace(), path, new AsyncCallback<JcrNode>() {
            @Override
            public void onFailure(Throwable caught) {
                console.hideLoadingIcon();
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(JcrNode result) {
                console.hideLoadingIcon();
                node = result;
                
                Contents.this.path = result.getPath();
                pathLabel.display(result.getPath());
                
                console.updateWorkspace(workspace(), changeHistory);
                console.updatePath(path, changeHistory);
                
                children.show(node);
                properties.show(node);
                accessList.show(node);
                
                viewPort().display(Contents.this);
            }
        });
    }
    
    public void backup(String name) {
        jcrService.backup(repository, workspace(), name, new AsyncCallback<Object>() {
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

    public void restore(String name) {
        jcrService.restore(repository, workspace(), name, new AsyncCallback<Object>() {
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
    
    
    public void export() {
        exportDialog.showModal();
    }

    public void importXML() {
        importDialog.showModal();
    }
    
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
        final String path = parent(path());
        jcrService.removeNode(repository(), workspace(), path(), new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                select(path, true);
            }
        });
    }

    public void addNode() {
        newNodeDialog.showModal();
    }
    
    public void renameNode() {
        renameNodeDialog.showModal();
    }
    
    public void updateMixinTypes() {
        jcrService.getMixinTypes(repository(), workspace(), false, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                addMixinDialog.updateMixinTypes(result);
                removeMixinDialog.updateMixinTypes(result);
            }
        });
    }
    
    public void addMixin() {
        addMixinDialog.showModal();
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
            }
        });
    }
    
    public void removeMixin() {
        removeMixinDialog.showModal();
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
            }
        });
    }
    
    protected void updatePropertyDefs() {
        setPropertyDialog.updatePropertyDefs(node.getPropertyDefs());
    }
    
    public void setProperty() {
        setPropertyDialog.showModal();
    }
    
    protected void setNodeProperty(String name, String value) {
        jcrService.setProperty(repository(), workspace(), path(), name, value, new AsyncCallback<Object>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
            }
        });
    }
    protected void addNode(String name, String primaryType) {
        jcrService.addNode(repository(), workspace(), path(), name, primaryType, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                show();
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
                show();
            }
        });
    }
    
    protected void updatePrimaryTypes() {
        jcrService.getPrimaryTypes(repository(), workspace(), true, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                newNodeDialog.updatePrimaryTypes(result);
            }
        });
    }
    
    public void addAccessList() {
        addAccessListDialog.showModal();
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
        accessList.show(node);
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
            }
        });
    }
    
    public void removeAccessList(String name) {
        node.getAccessList().remove(name);
        accessList.show(node);
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
            }
        });
    }
    
    public void setNode(JcrNode node) {
        this.node = node;
        this.pathLabel.display(node.getPath());
    }
    
    public String repository() {
        return repository;
    }

    public String workspace() {
        return workspaces.getValueAsString();
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
                        select(label.getDataPath(), true);
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
    
    private class Strut extends HLayout {
        public Strut(int size) {
            super();
            setWidth(size);                    
        }
    }
}