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
package org.modeshape.web.client.peditor;

import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import org.modeshape.web.client.contents.Contents;
import org.modeshape.web.shared.JcrNode;

/**
 * Default value editor which is using text representation of the value.
 * 
 * @author kulikov
 */
public class BooleanValueEditor extends BaseEditor implements ValueEditor<String> {
    //form title
    private final static String TITLE = "Modify property value";
    
    //form dimensions
    private final static int WIDTH = 350;
    private final static int HEIGHT = 150;
    
    //form fields
    private final CheckboxItem valueEditor = new CheckboxItem("Value");
    
    //'controller' - provides access to business logic
    private final Contents contents;
    
    //node/property references
    private JcrNode node;
    private String name;
    
    /**
     * Creates this property editor.
     * 
     * @param contents Contents view form.
     */
    public BooleanValueEditor(Contents contents) {
        super(TITLE, WIDTH, HEIGHT);
        this.contents = contents;
        
        valueEditor.setWidth(200);
        valueEditor.setStartRow(true);
        valueEditor.setEndRow(true);
        
        setControls(valueEditor);
    }
    
    @Override
    public void setValue(JcrNode node, String name, String value) {
        this.node = node;
        this.name = name;
        
        valueEditor.setTitle(name);
        valueEditor.setValue(value);
        
        showModal();
    }

    @Override
    public void onConfirm(ClickEvent event) {
        contents.setNodeProperty(node, name,valueEditor.getValueAsBoolean());
    }
    
}
