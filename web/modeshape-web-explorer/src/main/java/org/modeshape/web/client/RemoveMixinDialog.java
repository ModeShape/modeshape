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

import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;

/**
 *
 * @author kulikov
 */
public class RemoveMixinDialog extends ModalDialog {
    
    private Contents contents;
    private ComboBoxItem mixins = new ComboBoxItem("Mixin type");
    
    public RemoveMixinDialog(Contents contents) {
        super("Remove mixin", 450, 150);
        this.contents = contents;
        
        StaticTextItem description = new StaticTextItem();
        description.setValue("Select mixin type");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setControls(description, mixins);
    }
    
    public void updateMixinTypes(String[] mixins) {
        this.mixins.setValueMap(mixins);
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        contents.removeMixin(mixins.getValueAsString());
    }
    
}
