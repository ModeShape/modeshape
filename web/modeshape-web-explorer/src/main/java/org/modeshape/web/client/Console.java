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

import java.util.List;
import org.modeshape.web.shared.JcrNode;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class Console implements EntryPoint {

    /**
     * The message displayed to the user when the server cannot be reached or returns an error.
     */
    private static final String SERVER_ERROR = "An error occurred while "
                                               + "attempting to contact the server. Please check your network "
                                               + "connection and try again.";
    /**
     * Create a remote service proxy to talk to the server-side Greeting service.
     */
    protected final JcrServiceAsync jcrService = GWT.create(JcrService.class);

    private final VLayout mainForm = new VLayout();
    protected final NodePanel nodePanel = new NodePanel(this);
    private final RepositoryPanel repositoryPanel = new RepositoryPanel(this);
    private final QueryPanel queryPanel = new QueryPanel(this);

    protected Navigator navigator;

    protected final NewNodeDialog newNodeDialog = new NewNodeDialog("Create new node", this);
    protected final AddMixinDialog addMixinDialog = new AddMixinDialog("Add mixin", this);
    protected final RemoveMixinDialog removeMixinDialog = new RemoveMixinDialog("Remove mixin", this);
    protected final AddPropertyDialog addPropertyDialog = new AddPropertyDialog("Add property", this);

    protected JcrURL jcrURL = new JcrURL();
    protected HtmlHistory htmlHistory = new HtmlHistory();

    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        jcrService.getRequestedURI(new AsyncCallback<String>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess( String result ) {
                jcrURL.parse(result);
                LoginDialog loginDialog = new LoginDialog(Console.this);
                loginDialog.setJndiName(jcrURL.getRepository());
                loginDialog.setWorkspace(jcrURL.getWorkspace());
            }
        });
    }

    /**
     * Reconstructs URL and points browser to the requested node path.
     * 
     * @param repository
     * @param workspace
     */
    public void init( String repository,
                      String workspace ) {
        // upd
        jcrURL.setRepository(repository);
        jcrURL.setWorkspace(workspace);
        String path = jcrURL.getPath();
        if (path == null) path = "/";
        else path = parent(path);
        jcrService.childNodes(path, new AsyncCallback<List<JcrNode>>() {
            @Override
            public void onFailure( Throwable caught ) {
            }

            @Override
            public void onSuccess( List<JcrNode> result ) {
                for (JcrNode node : result) {
                    if (node.getPath().equals(jcrURL.getPath())) {
                        nodePanel.display(navigator.convert(node));
                    }
                }
            }
        });
    }

    public void showMainForm( String repository,
                              String workspace ) {
        mainForm.setLayoutMargin(5);
        mainForm.setWidth100();
        mainForm.setHeight100();
        mainForm.setBackgroundColor("#F0F0F0");
        // tool bar
        HLayout topPanel = new HLayout();

        topPanel.setAlign(Alignment.LEFT);
        topPanel.setOverflow(Overflow.HIDDEN);
        topPanel.setHeight("5%");
        topPanel.setBackgroundColor("#d3d3d3");

        HLayout bar = new HLayout();
        bar.addMember(new PathPanel(this));

        HLayout buttonBar = new HLayout();
        buttonBar.setMargin(1);
        buttonBar.setAlign(Alignment.RIGHT);

        Button saveButton = new Button("Save");
        saveButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick( ClickEvent event ) {
                jcrService.save(new AsyncCallback() {

                    @Override
                    public void onFailure( Throwable caught ) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess( Object result ) {
                    }
                });
            }
        });

        Button logoutButton = new Button("Logout");
        logoutButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick( ClickEvent event ) {
                mainForm.hide();
                new LoginDialog(Console.this).showDialog();
            }
        });

        buttonBar.addMember(saveButton);
        buttonBar.addMember(logoutButton);

        bar.addMember(buttonBar);

        topPanel.addMember(bar);

        // main area
        HLayout bottomPanel = new HLayout();

        VLayout viewPortLayout = new VLayout();
        viewPortLayout.setWidth("80%");

        TabSet viewPort = new TabSet();
        viewPort.setTabs(nodePanel, repositoryPanel, queryPanel);

        viewPortLayout.addMember(viewPort);

        navigator = new Navigator(this);

        bottomPanel.addMember(navigator);
        bottomPanel.addMember(viewPortLayout);

        HLayout sp1 = new HLayout();
        sp1.setHeight("1%");

        HLayout sp2 = new HLayout();
        sp2.setHeight("1%");

        HLayout statusBar = new HLayout();
        statusBar.setHeight("2%");
        statusBar.setBorder("1px solid #d3d3d3");
        Label statusLabel = new Label("Modeshape web browser, version 3.6");
        statusLabel.setWidth(300);
        statusBar.addMember(statusLabel);

        // mainForm.addMember(toolBar);
        mainForm.addMember(topPanel);
        mainForm.addMember(sp2);
        mainForm.addMember(bottomPanel);
        mainForm.addMember(statusBar);

        mainForm.draw();

        navigator.showRoot();
        repositoryPanel.display();
        queryPanel.init();

        this.init(repository, workspace);
    }

    private String parent( String path ) {
        return path == null ? null : path.substring(0, path.lastIndexOf('/'));
    }
}
