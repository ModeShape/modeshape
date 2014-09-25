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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.client.grid.NodeTypes;
import org.modeshape.web.shared.JcrNodeType;

/**
 *
 * @author kulikov
 */
public class NodeTypeView extends View {

    private final static String HINT = "<p>Each node in a Modeshape workspace "
            + "tree has a node type that defines the child nodes and properties "
            + "it may (or must) have. Developers can use node types to define a "
            + "custom content model for their application domain and have "
            + "Modeshape enforce the constraints of that model at the "
            + "repository level.</p>";
    
    private NodeTypes nodeTypes;
    private ComboBoxItem workspaces = new ComboBoxItem();
    private JcrServiceAsync jcrService;
    private String repository;
    
    public NodeTypeView(JcrServiceAsync jcrService, ViewPort viewPort) {
        super(viewPort, null);
        this.jcrService = jcrService;
        
        Canvas text = new Canvas();
        text.setContents(HINT);
        text.setWidth100();
        text.setAutoHeight();
        text.setStyleName("caption");
        
        nodeTypes = new NodeTypes();
        
        addMember(text);
        
        DynamicForm form = new DynamicForm();
        form.setFields(workspaces);
        workspaces.setTitle("Workspace");

        HLayout panel = new HLayout();
        panel.addMember(form);
        panel.setStyleName("viewport");
        panel.setHeight(35);
        panel.setLayoutAlign(VerticalAlignment.CENTER);

        addMember(panel);

        VLayout strut = new VLayout();
        strut.setHeight(20);
        
        addMember(panel);
        addMember(strut);
        addMember(nodeTypes);
    }

    public void show(String repository) {
        this.repository = repository;
        jcrService.getWorkspaces(repository, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess(String[] result) {
                workspaces.setValueMap(result);
                if (result.length > 0) {
                    workspaces.setValue(result[0]);
                }
                showTypes();
            }
        });
    }

    public void showTypes() {
        jcrService.nodeTypes(repository, workspaces.getValueAsString(), new AsyncCallback<Collection<JcrNodeType>> () {

            @Override
            public void onFailure(Throwable caught) {
//                hideLoadingIcon();
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess(Collection<JcrNodeType> result) {
//                hideLoadingIcon();
                try {
                    nodeTypes.show(result);
                } catch (Exception e) {
                    SC.say(e.getMessage());
                }
                viewPort().display(NodeTypeView.this);
            }
        });
    }
    
    public void show(Collection<JcrNodeType> types) {
        nodeTypes.show(types);
    }
    
}