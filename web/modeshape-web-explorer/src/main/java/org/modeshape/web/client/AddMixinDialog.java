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

    public void updateMixinTypes(String[] mixins) {
        name.setValueMap(mixins);
    }
    
    @Override
    public void onConfirm(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
        contents.addMixin(name.getValueAsString());
    }
}
