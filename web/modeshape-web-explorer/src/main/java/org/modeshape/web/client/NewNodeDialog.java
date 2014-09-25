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

import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

/**
 * Dialog asking node's name and primary type and creating new node.
 *
 * @author kulikov
 */
public class NewNodeDialog extends ModalDialog {

    private TextItem name = new TextItem();
    private ComboBoxItem primaryType = new ComboBoxItem();
    private Contents contents;
    
    public NewNodeDialog(Contents contents) {
        super("Create new node", 450, 250);
        this.contents = contents;

        name.setName("name");
        name.setTitle("Node name");
        name.setDefaultValue("");
        name.setWidth(250);
        name.setRequired(true);
        name.setVisible(true);
        name.setStartRow(true);
        name.setEndRow(true);

        primaryType.setName("primaryType");
        primaryType.setTitle("Primary Type");
        primaryType.setDefaultValue("nt:unstructured");
        primaryType.setWidth(250);
        primaryType.setRequired(true);
        primaryType.setStartRow(true);
        primaryType.setEndRow(true);

        StaticTextItem description = new StaticTextItem();
        description.setValue("Please specify the name of node and choose type");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);

        setControls(description, name, primaryType);
    }

    @Override
    public void showModal() {
        contents.updatePrimaryTypes();
        super.showModal();
    }

    protected void updatePrimaryTypes(String[] primaryTypes) {
        primaryType.setValueMap(primaryTypes);
    }
    
    @Override
    public void onConfirm(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
        contents.addNode(name.getValueAsString(), primaryType.getValueAsString());
    }
}
