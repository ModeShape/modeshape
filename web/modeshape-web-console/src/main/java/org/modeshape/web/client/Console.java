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

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.TabSet;

/**
 * Entry point classes define
 * <code>onModuleLoad()</code>.
 */
public class Console implements EntryPoint {

    /**
     * The message displayed to the user when the server cannot be reached or
     * returns an error.
     */
    private static final String SERVER_ERROR = "An error occurred while "
            + "attempting to contact the server. Please check your network "
            + "connection and try again.";
    /**
     * Create a remote service proxy to talk to the server-side Greeting
     * service.
     */
    protected final JcrServiceAsync jcrService = GWT.create(JcrService.class);

    private final VLayout mainForm = new VLayout();
    private final ToolBar toolBar = new ToolBar(this);
    protected final NodePanel nodePanel = new NodePanel();
    private final RepositoryPanel repositoryPanel = new RepositoryPanel(this);
    private final QueryPanel queryPanel = new QueryPanel(this);

    protected Navigator navigator;
    
    protected final NewNodeDialog newNodeDialog = new NewNodeDialog("Create new node", this);
    protected final AddMixinDialog addMixinDialog = new AddMixinDialog("Add mixin", this);
    /**
     * This is the entry point method.
     */
    @Override
    public void onModuleLoad() {
        new LoginDialog(this).showDialog();
    }

    public void showMainForm() {
        mainForm.setLayoutMargin(5);
        mainForm.setWidth100();
        mainForm.setHeight100();
        mainForm.setBackgroundColor("#F0F0F0");
        //tool bar
        HLayout topPanel = new HLayout();

        topPanel.setAlign(Alignment.LEFT);
        topPanel.setOverflow(Overflow.HIDDEN);
        topPanel.setHeight("5%");
        topPanel.setBackgroundColor("#d3d3d3");
        topPanel.addMember(new PathPanel(this));

        //main area
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
        
        mainForm.addMember(toolBar);
        mainForm.addMember(topPanel);
        mainForm.addMember(sp2);
        mainForm.addMember(bottomPanel);
        mainForm.addMember(statusBar);
        
        mainForm.draw();
        
        navigator.showRoot();        
        repositoryPanel.display();
        queryPanel.init();
    }
}
