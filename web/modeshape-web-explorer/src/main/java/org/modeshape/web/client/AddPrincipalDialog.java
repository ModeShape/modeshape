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

import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import org.modeshape.web.shared.JcrPolicy;

/**
 * Dialog asking principal's name.
 * 
 * @author kulikov
 */
public class AddPrincipalDialog extends ModalDialog {
    
    private TextItem name = new TextItem("Principal");
    private Console console;
    
    public AddPrincipalDialog(String title, Console console) {
        super(title, 400, 200);
        this.console = console;
        
        StaticTextItem description = new StaticTextItem();
        description.setValue("Specify principal's name");
        
        setControls(description, name);
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        //pick up selected node and entered principal name
        JcrTreeNode node = console.navigator.getSelectedNode();
        String principal = name.getValueAsString();
        
        //add new entry with all permissions and display it
        node.getAccessList().add(new JcrPolicy(principal));
        console.nodePanel.display(node.getAccessList(), principal);
    }
    
}
