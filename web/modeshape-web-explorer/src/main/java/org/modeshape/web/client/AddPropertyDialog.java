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
