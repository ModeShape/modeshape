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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import org.modeshape.web.client.contents.PropertiesEditor.PropertyRecord;
import org.modeshape.web.client.grid.TabGrid;
import org.modeshape.web.client.peditor.BaseEditor;
import org.modeshape.web.client.peditor.ValueEditor;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrProperty;

/**
 *
 * @author kulikov
 */
@SuppressWarnings("synthetic-access")
public class PropertiesEditor extends TabGrid<PropertyRecord, JcrProperty> {

    private JcrNode node;
    private final Contents contents;
        
    public PropertiesEditor(Contents contents) {
        super("Properties");
        this.contents = contents;
    }

    public void show(JcrNode node) {
        this.node = node;
        setValues(node.getProperties());
    }

    @Override
    protected PropertyRecord[] records() {
        PropertyRecord[] records = new PropertyRecord[100];
        for (int i = 0; i < records.length; i++) {
            records[i] = new PropertyRecord();
        }
        return records;
    }

    @Override
    protected HLayout tableHeader() {
        HLayout header = new HLayout();
        header.setHeight(30);
        header.setBackgroundColor("#e6f1f6");

        Label name = new Label("<b>Name</b>");
        name.setWidth100();

        Label value = new Label("<b>Value</b>");
        value.setWidth100();
        
        header.addMember(name);
        header.addMember(value);
        
        return header;
    }

    @Override
    protected HLayout toolBar() {
        HLayout header = new HLayout();
        header.setBackgroundColor("#ffffff");
        header.setAlign(Alignment.LEFT);
        header.setDefaultLayoutAlign(Alignment.LEFT);
        header.setLayoutAlign(Alignment.LEFT);
        header.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        header.setLayoutAlign(VerticalAlignment.CENTER);
        header.setAlign(VerticalAlignment.CENTER);
        header.setHeight(30);
        header.setContents("Click respective to edit property value");
        return header;
    }

    @Override
    protected void updateRecord(int pos, PropertyRecord record, JcrProperty value) {
        record.setValue(value);
    }

    protected class PropertyRecord extends HLayout {
        //visible height of the recorod
        private final static int RECORD_HEIGHT = 30;
        
        private Label name = new Label();
        private Label value = new Label();
        
        private Button editButton = new Button();
        private ValueEditor<String> editor;
        
        public PropertyRecord() {
            super();                        
            setStyleName("grid");
            setHeight(RECORD_HEIGHT);

            setDefaultLayoutAlign(VerticalAlignment.CENTER);
            setDefaultLayoutAlign(Alignment.LEFT);

            setLayoutAlign(VerticalAlignment.CENTER);
            setLayoutAlign(Alignment.CENTER);

            setAlign(VerticalAlignment.CENTER);
            setAlign(Alignment.LEFT);

            name.setIcon("icons/sprocket.png");
            name.setStyleName("text");
            name.setWidth100();

            
            value.setStyleName("text");
            value.setWidth100();
            value.setOverflow(Overflow.HIDDEN);
            value.setAlign(Alignment.RIGHT);
            value.setLayoutAlign(Alignment.RIGHT);

            editButton.setTitle("...");
            editButton.setWidth(RECORD_HEIGHT);
            editButton.setHeight(RECORD_HEIGHT);
            
            addMember(name);
            addMember(value);
            addMember(editButton);
        }

        private void setValue(final JcrProperty property) {
            name.setContents(property.getName());
            value.setContents(property.getDisplayValue());
            editor = BaseEditor.getValueEditor(property.getName(), 
                    property.getType(), PropertiesEditor.this.contents);
            editButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    //show modal form with reference to property.getValue()
                    editor.setValue(node, property.getName(), property.getValue());
                }
            });
        }
    }

}
