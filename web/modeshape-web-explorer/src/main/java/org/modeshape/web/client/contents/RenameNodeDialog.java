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

import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.ModalDialog;

/**
 * Dialog asking node's name and primary type and creating new node.
 *
 * @author kulikov
 */
public class RenameNodeDialog extends ModalDialog {

    private TextItem name = new TextItem();
    private Contents contents;
    private JcrNode node;
    
    public RenameNodeDialog(Contents contents) {
        super("Create new node", 450, 150);
        this.contents = contents;

        name.setName("name");
        name.setTitle("Node name");
        name.setDefaultValue("");
        name.setWidth(250);
        name.setRequired(true);
        name.setVisible(true);
        name.setStartRow(true);
        name.setEndRow(true);

        StaticTextItem description = new StaticTextItem();
        description.setValue("Please specify the new name of node");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);

        setControls(description, name);
    }

    public void showModal(JcrNode node) {
        this.node = node;
        showModal();
    }
    
    @Override
    public void onConfirm(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
        contents.renameNode(node, name.getValueAsString());
    }
}
