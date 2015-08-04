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
package org.modeshape.web.client.contents;

import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import org.modeshape.web.shared.ModalDialog;

/**
 * Dialog asking node's name and primary type and creating new node.
 *
 * @author kulikov
 */
public class AddNodeDialog extends ModalDialog {

    private TextItem name = new TextItem();
    private ComboBoxItem primaryType = new ComboBoxItem();
    private Contents contents;
    
    public AddNodeDialog(Contents contents) {
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

    public void setPrimaryTypes(String[] primaryTypes) {
        primaryType.setValueMap(primaryTypes);
    }
    
    @Override
    public void onConfirm(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
        contents.addNode(name.getValueAsString(), primaryType.getValueAsString());
    }
}
