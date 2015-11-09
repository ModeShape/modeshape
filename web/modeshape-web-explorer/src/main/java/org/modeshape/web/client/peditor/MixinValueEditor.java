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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.MultipleAppearance;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.ArrayList;
import org.modeshape.web.client.contents.Contents;
import org.modeshape.web.shared.Align;
import org.modeshape.web.shared.Columns;
import org.modeshape.web.shared.JcrNode;

/**
 * Default value editor which is using text representation of the value.
 * 
 * @author kulikov
 */
public class MixinValueEditor extends BaseEditor implements ValueEditor<String> {
    //form title
    private final static String TITLE = "Modify property value";
    
    //form dimensions
    private final static int WIDTH = 430;
    private final static int HEIGHT = 350;
    
    //form fields
    private final SelectItem mixTypes = new SelectItem("");
    private final SelectItem selectedTypes = new SelectItem("");
    
    //'controller' - provides access to business logic
    private final Contents contents;
    
    //node/property references
    private JcrNode node;
    private String name;
    
    private String[] allTypes;
    private String[] nodeTypes;
    private String[] originalNodeTypes;
    
    /**
     * Creates this property editor.
     * 
     * @param contents Contents view form.
     */
    public MixinValueEditor(Contents contents) {
        super(TITLE);
        this.contents = contents;

        setWidth(WIDTH);
        setHeight(HEIGHT);                
        
        Columns h1 = new Columns(Align.CENTER, Align.CENTER);
        h1.setHeight(15);
        
        Label l1 = new Label();
        l1.setWidth(150);
        l1.setContents("Mixin Types");

        Label l2 = new Label();
        l2.setWidth(100);
        l2.setContents("");
        
        Label l3 = new Label();
        l3.setWidth(150);
        l3.setContents("Node's types");
        
        h1.addMember(l1);
        h1.addMember(l2);
        h1.addMember(l3);
        
        addMember(h1);
        
        Columns layout = new Columns(Align.CENTER, Align.CENTER);
        layout.setWidth100();

        mixTypes.setMultiple(true);
        mixTypes.setMultipleAppearance(MultipleAppearance.GRID);
        mixTypes.setWidth(150);
        mixTypes.setHeight(200);
        
        selectedTypes.setMultiple(true);
        selectedTypes.setMultipleAppearance(MultipleAppearance.GRID);
        selectedTypes.setWidth(150);
        selectedTypes.setHeight(200);
        
        DynamicForm f1 = new DynamicForm();
        f1.setItems(mixTypes);
        f1.setWidth(150);
        f1.setHeight(200);
        
        DynamicForm f2 = new DynamicForm();
        f2.setItems(selectedTypes);
        f2.setWidth(150);
        f2.setHeight(200);
        
        VLayout buttons = new VLayout();
        buttons.setLayoutAlign(Alignment.CENTER);
        buttons.setDefaultLayoutAlign(Alignment.CENTER);
        buttons.setAlign(Alignment.CENTER);        
        buttons.setLayoutAlign(VerticalAlignment.CENTER);        
        buttons.setWidth(50);
        
        layout.addMember(f1);
        layout.addMember(buttons);
        layout.addMember(f2);
        
        Button b1 = new Button(">>");
        b1.setWidth(30);
        b1.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                allTypes = remove(allTypes, mixTypes.getValueAsString());
                nodeTypes = add(nodeTypes, mixTypes.getValueAsString());
                updateLists();
            }
        });
        
        Button b2 = new Button("<<");
        b2.setWidth(30);
        b2.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                nodeTypes = remove(nodeTypes, selectedTypes.getValueAsString());
                allTypes = add(allTypes, selectedTypes.getValueAsString());
                updateLists();
            }
        });
        
        buttons.addMember(b1);
        buttons.addMember(b2);
        
        addMember(layout);
        
        Columns h3 = new Columns(Align.RIGHT, Align.CENTER);
        h3.setHeight(50);
        
        Button okButton = new Button("OK");
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                onConfirm(null);
                hide();
            }
        });
        
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(com.smartgwt.client.widgets.events.ClickEvent event) {
                hide();
            }
        });
        
        h3.addMember(okButton);
        h3.addMember(cancelButton);
        
        addMember(h3);
        
    }
    
    @Override
    public void showModal() {
        contents.jcrService().getMixinTypes(contents.repository(), contents.workspace(),
                false, new AsyncCallback<String[]>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                allTypes = result;
                updateLists();
                MixinValueEditor.super.showModal();
            }
        });
        
    }
    
    private void updateLists() {
        mixTypes.setValueMap(diff(allTypes, nodeTypes));
        selectedTypes.setValueMap(diff(nodeTypes, allTypes));
    }
    
    private String[] parseValues(String value) {
        if (value.equalsIgnoreCase("N/A")) {
            return new String[]{};
        }
        return value.split(",");
    }
    
    @Override
    public void setValue(JcrNode node, String name, String value) {
        this.node = node;
        this.name = name;
        
        nodeTypes = parseValues(value);
        originalNodeTypes = parseValues(value);
        
        showModal();
    }

    private String[] remove(String[] values, String value) {
        String[] res = new String[values.length - 1];
        int j = 0;
        for (int i = 0; i < values.length; i++) {
            if (!values[i].equalsIgnoreCase(value)) {
                res[j++] = values[i];
            }
        }
        return res;
    }
    
    private String[] add(String[] values, String value) {
        String[] res = new String[values.length + 1];
        for (int i = 0; i < values.length; i++) {
            res[i] = values[i];
        }
        res[values.length] = value;
        return res;
    }

    @SuppressWarnings("unchecked")
    private String[] diff(String[] a, String[] b) {
        ArrayList<String> list = new ArrayList();
        for (int i = 0; i < a.length; i++) {
            boolean found = false;
            for (int j = 0; j < b.length; j++) {
                if (a[i].equalsIgnoreCase(b[j])) {
                    found = true;
                    break;
                }
            }
            if (!found) list.add(a[i]);
        }
        String[] map = new String[list.size()];
        list.toArray(map);
        return map;
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        //new types
        for (int i = 0; i < nodeTypes.length; i++) {
            boolean isNew = true;
            
            for (int j = 0; j < originalNodeTypes.length; j++) {
                if (nodeTypes[i].equalsIgnoreCase(originalNodeTypes[j])) {
                    isNew = false;
                    break;
                }
            }

            if (isNew)  contents.addMixin(nodeTypes[i]);
        }
        
        //removed types
        for (int i = 0; i < originalNodeTypes.length; i++) {
            boolean isRemoved = false;
            
            for (int j = 0; j < nodeTypes.length; j++) {
                if (originalNodeTypes[i].equalsIgnoreCase(nodeTypes[j])) {
                    isRemoved = false;
                    break;
                }
            }

            if (isRemoved)  contents.removeMixin(nodeTypes[i]);
        }
    }
 
}
