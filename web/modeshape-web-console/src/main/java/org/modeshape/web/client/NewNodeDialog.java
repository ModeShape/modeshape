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
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VStack;

/**
 *
 * @author kulikov
 */
public class NewNodeDialog extends DynamicForm {
    private Window window = new Window();
    private TextItem name = new TextItem();
    private TextItem primaryType = new TextItem();

    private Console console;
    
    public NewNodeDialog(Console console) {
        super();
        this.console = console;
        
        setID("newNodeDialog");
        setNumCols(2);
        setPadding(25);

        name.setName("name");
        name.setTitle("Node name");
        name.setDefaultValue("");
        name.setWidth(250);
        name.setRequired(true);
        name.setVisible(true);

        primaryType.setName("primaryType");
        primaryType.setTitle("Primary Type");
        primaryType.setDefaultValue("");
        primaryType.setWidth(250);
        primaryType.setRequired(true);

        SubmitItem okButton = new SubmitItem("OK");
        okButton.setTitle("OK");
        okButton.setWidth(100);

        VStack vStack = new VStack();

        SpacerItem spacerItem1 = new SpacerItem();
        SpacerItem spacerItem2 = new SpacerItem();
        spacerItem1.setStartRow(true);
        spacerItem1.setEndRow(true);
        spacerItem2.setStartRow(true);
        spacerItem2.setEndRow(false);

        okButton.setStartRow(false);
        okButton.setEndRow(true);
        
        this.addSubmitValuesHandler(new AddNodeHandler());
        
        setItems(spacerItem1, name, spacerItem1, primaryType,
                spacerItem1, spacerItem2, okButton);

        vStack.setTop(30);
        vStack.addMember(this);

        window.addChild(vStack);
        window.setTitle("Login");
        window.setCanDragReposition(true);
        window.setCanDragResize(false);
        window.setShowMinimizeButton(false);
        window.setShowCloseButton(false);
        window.setHeight(350);
        window.setWidth(400);
        window.setAutoCenter(true);
        window.show();

        name.focusInItem();
        
    }
    
    public void showDialog() {
        window.show();
    }
    
    public void hideDialog() {
        window.hide();
    }
    
    private class AddNodeHandler implements SubmitValuesHandler {

        @Override
        public void onSubmitValues(SubmitValuesEvent event) {
            String path = console.navigator.getSelectedPath();
            console.jcrService.addNode(path, name.getValueAsString(), primaryType.getValueAsString(), new AddNodeAsyncHandler());
        }
        
    }
    
    private class AddNodeAsyncHandler implements AsyncCallback {

        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Object result) {
            console.navigator.selectNode();
        }
        
    }
}
