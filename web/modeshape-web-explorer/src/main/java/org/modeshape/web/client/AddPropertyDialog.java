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
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;

/**
 *
 * @author kulikov
 */
public class AddPropertyDialog extends ModalDialog {
    
    private Contents contents;
    
    private ComboBoxItem name = new ComboBoxItem("Property name");
    private TextItem value = new TextItem("Value");
    
    public AddPropertyDialog(Contents contents) {
        super("Property", 400, 200);
        this.contents = contents;        
        StaticTextItem description = new StaticTextItem();
        description.setValue("Select property name and specify value");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setControls(description, name, value);
    }
    
    @Override
    public void showModal() {
        contents.updatePropertyDefs();
        super.showModal();
    }
    
    protected void updatePropertyDefs(String[] defs) {
        name.setValueMap(defs);
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        contents.setNodeProperty(name.getValueAsString(), value.getValueAsString());
    }
    
}
