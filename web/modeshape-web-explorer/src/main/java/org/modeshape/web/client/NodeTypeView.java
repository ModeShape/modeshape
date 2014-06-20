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
        
        HLayout vstrut = new HLayout();
        vstrut.setHeight(15);

        HLayout bottomStrut = new HLayout();
        bottomStrut.setHeight(15);
        
        addMember(text);
//        addMember(vstrut);
        
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
                SC.say("123");
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
                SC.say("124");
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