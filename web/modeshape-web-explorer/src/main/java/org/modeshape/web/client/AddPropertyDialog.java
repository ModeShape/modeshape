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
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;

/**
 *
 * @author kulikov
 */
public class AddPropertyDialog extends ModalDialog{
    
    private Console console;
    private ComboBoxItem name = new ComboBoxItem("Property name");
    private TextItem value = new TextItem("Value");
    
    public AddPropertyDialog(String title, Console console) {
        super(title, 400, 200);
        StaticTextItem description = new StaticTextItem();
        description.setValue("Select property name and specify value");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setControls(description, name, value);
        this.console = console;
    }
    
    @Override
    public void showModal() {
        JcrTreeNode node = console.navigator.getSelectedNode();
        if (node != null) {
            name.setValueMap(node.getPropertyDefs());
        } 
        super.showModal();
    }
        
    @Override
    public void onConfirm(ClickEvent event) {
        String path = console.navigator.getSelectedPath();
        console.jcrService.setProperty(path, name.getValueAsString(), value.getValueAsString(), new AsyncCallback() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
            }
        });
    }
    
}
