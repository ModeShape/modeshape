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
