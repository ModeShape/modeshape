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

/**
 * Dialog for asking mixin.
 * 
 * @author kulikov
 */
public class AddMixinDialog extends ModalDialog {

    private ComboBoxItem name = new ComboBoxItem();
    private Contents contents;
    
    public AddMixinDialog(Contents contents) {
        super("Add mixin", 450, 150);
        this.contents = contents;
        
        name.setName("name");
        name.setTitle("Mixin");
        name.setDefaultValue("");
        name.setWidth(250);
        name.setRequired(true);
        name.setVisible(true);
        name.setStartRow(true);
        name.setEndRow(true);

        StaticTextItem description = new StaticTextItem();
        description.setValue("Select mixin type");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setControls(description, name);
        name.focusInItem();
    }

    @Override
    public void showModal() {
        contents.updateMixinTypes();
        super.showModal();
    }
    
    protected void updateMixinTypes(String[] mixins) {
        name.setValueMap(mixins);
    }
    
    @Override
    public void onConfirm(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
        contents.addMixin(name.getValueAsString());
    }
}
