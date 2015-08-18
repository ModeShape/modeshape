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
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.modeshape.web.shared.Align;
import org.modeshape.web.shared.Columns;

/**
 * Panel for session/workspace actions.
 * 
 * Layout:
 * 
 * |----------------------------------------------|
 * |       col1             |    col2             |
 * |----------------------------------------------|
 * 
 * @author kulikov
 */
public class WorkspacePanel extends Columns {
    
    private final static String STYLE_NAME = "viewport";
    private final static int LAYOUT_MARGIN = 5;
    
    private final static String COL2_WIDTH = "25%";
            
    private final Contents contents;
    
    private final Column1 col1 = new Column1();
    private final Column2 col2 = new Column2();

    private RefreshSessionDialog refreshDialog;
    /**
     * Create new instance.
     * 
     * @param contents 
     */
    public WorkspacePanel(final Contents contents) {
        super(Align.LEFT, Align.CENTER);
        this.contents = contents;
        refreshDialog = new RefreshSessionDialog(contents);
        
        setStyleName(STYLE_NAME);
        setLayoutMargin(LAYOUT_MARGIN);

        HLayout strut = new HLayout();
        strut.setWidth(40);
        
        addMember(col1);
        addMember(strut);
        addMember(col2);
    }

    public void setEnabled(boolean enabled) {
        col1.setEnabled(enabled);
    }
    
    /**
     * Assigns workspace names to the combo box into column 2.
     * 
     * @param values 
     */
    public void setWorkspaceNames(String[] values) {
        col2.combo.setValueMap(values);
        if (values.length > 0) {
            col2.combo.setValue(values[0]);
        }
    }
    
    /**
     * Gets selected combo box value.
     * 
     * @return 
     */
    public String getSelectedWorkspace() {
        return col2.combo.getValueAsString();
    }
    
    /**
     * |---------------------------------|
     * | combo_title : combox_box        |
     * |---------------------------------|
     */
    private class Column2 extends Columns {

        private ComboBoxItem combo = new ComboBoxItem();

        public Column2() {
            super(Align.LEFT, Align.CENTER);
            setWidth(COL2_WIDTH);
            
            final DynamicForm form = new DynamicForm();
            form.setFields(combo);

            combo.setTitle("Workspace");
            combo.setWidth(250);

            combo.addChangedHandler(new ChangedHandler() {
                @Override
                public void onChanged(ChangedEvent event) {
                    contents.changeWorkspace((String) event.getValue());
                }
            });

            addMember(form);
        }

    }

    /**
     * |---------------------------------|
     * |               row1              |
     * |---------------------------------|
     * |               row2              |
     * |---------------------------------|
     */
    private class Column1 extends VLayout {
        private final Label saveButton = new Label();
        
        public Column1() {
            setWidth100();

            //append rows
            addMember(Row1());
            addMember(Row2());
        }
        
        public void setEnabled(boolean enabled) {
            if (enabled) {
                saveButton.enable();
                saveButton.setStyleName("button-label");
            } else {
                saveButton.disable();
                saveButton.setStyleName("button-label-disable");
            }
            saveButton.redraw();
        }
        
        private HLayout Row1() {
            Columns top = new Columns(Align.LEFT, Align.CENTER);
            top.setAlign(Alignment.LEFT);

            top.setWidth100();
            top.setHeight(30);

            final Label caption = new Label();
            caption.setContents("Session ");
            caption.setAutoWidth();

            saveButton.setIcon("icons/cd.png");
            saveButton.setWidth(16);
            saveButton.setTooltip("Save session");
            saveButton.setStyleName("button-label-disabled");
            saveButton.setContents("Save");
            saveButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    contents.save();
                }
            });

            final Label refreshButton = new Label();
            refreshButton.setIcon("icons/apply.png");
            refreshButton.setWidth(16);
            refreshButton.setTooltip("Refresh session");
            refreshButton.setStyleName("button-label");
            refreshButton.setContents("Refresh");
            refreshButton.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    refreshDialog.showModal();
                }
            });

            top.addMember(caption);
            top.addStrut(5);
            top.addMember(saveButton);
            top.addStrut(5);
            top.addMember(refreshButton);

            return top;
        }

        private HLayout Row2() {
            Columns panel = new Columns(Align.LEFT, Align.CENTER);
            panel.setWidth100();
            panel.setHeight(50);
            panel.setStyleName("session-hint");
            
            final Label hint = new Label();
            hint.setWidth100();
            hint.setHeight100();
            hint.setContents("Each Session object is associated one-to-one with a "
                    + "Workspace object. The Workspace object represents a \"view\" "
                    + "of an actual repository workspace entity as seen through "
                    + "the authorization settings of its associated Session. ");
            panel.addMember(hint);
            return panel;
        }
    }
}
