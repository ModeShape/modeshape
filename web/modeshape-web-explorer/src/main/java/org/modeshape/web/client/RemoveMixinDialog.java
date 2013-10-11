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
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;

/**
 *
 * @author kulikov
 */
public class RemoveMixinDialog extends ModalDialog {
    
    private Console console;
    private ComboBoxItem mixins = new ComboBoxItem("Mixin type");
    
    public RemoveMixinDialog(String title, Console console) {
        super(title, 450, 150);
        this.console = console;
        
        StaticTextItem description = new StaticTextItem();
        description.setValue("Select mixin type");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setControls(description, mixins);
    }
    
    @Override
    public void showModal() {
        mixins.setValueMap(console.navigator.getSelectedNode().getMixins());
        super.showModal();
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        console.jcrService.removeMixin(console.navigator.getSelectedPath(), mixins.getValueAsString(), new AsyncCallback(){

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(Object result) {
                console.navigator.selectNode();
            }
            
        });
    }
    
}
