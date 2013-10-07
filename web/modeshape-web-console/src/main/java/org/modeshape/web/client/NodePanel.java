/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.Record;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.events.ChangeEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangeHandler;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;
import com.smartgwt.client.widgets.grid.events.CellSavedEvent;
import com.smartgwt.client.widgets.grid.events.CellSavedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import java.util.Collection;
import org.modeshape.web.shared.JcrPolicy;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrProperty;

/**
 *
 * @author kulikov
 */
public class NodePanel extends Tab {

    private GeneralNodeInformationPanel generalInfo = new GeneralNodeInformationPanel();
    private PropertiesPanel properties;
    private AccessControlPanel accessControl;
    
    private JcrTreeNode node;
    
    private String path;
    private Console console;
    
    public NodePanel(Console console) {
        super();
        this.console = console;
        setIcon("icons/view_table.png");
        
        setTitle("Node Properties");

        properties = new PropertiesPanel();
        accessControl = new AccessControlPanel();

        VLayout vLayout = new VLayout();

        vLayout.addMember(new Strut(15));
        vLayout.addMember(generalInfo);
        vLayout.addMember(new Strut(15));

        SectionStack stack = new SectionStack();
        stack.setWidth100();
        stack.setHeight100();
        stack.setVisibilityMode(VisibilityMode.MULTIPLE);

        SectionStackSection psection = new SectionStackSection("Properties");
        psection.setExpanded(true);


        psection.addItem(properties);
        stack.addSection(psection);

        SectionStackSection acl = new SectionStackSection("Access Control");
        acl.addItem(accessControl);
        acl.setExpanded(true);

        stack.addSection(acl);

        vLayout.addMember(stack);
        setPane(vLayout);
    }

    public String principal() {
        return this.accessControl.principal();
    }
    
    /**
     * Displays given node.
     * 
     * @param node the node to display.
     */
    public void display(JcrTreeNode node) {
        this.node = node;
        path = node.getPath();
        generalInfo.setNode(node);
        properties.setData(node.getProperties());
        accessControl.display(node.getAccessList(), null);
    }
    
    public void setProperties(Collection<JcrProperty> props) {
        properties.setData(props);
    }
    
    public void setCanEditProperties(boolean flag) {
        this.properties.setCanEdit(flag);
    }

    public void refreshProperties() {
        properties.refresh();
    }

    public void display(JcrAccessControlList acl) {
        accessControl.display(acl, null);
    }
    
    public void display(JcrAccessControlList acl, String principal) {
        accessControl.display(acl, principal);
    }
    
    private class PropertiesPanel extends VLayout {

        private ListGrid grid = new ListGrid();
        private ListGridField valueField;
        
//        private PropertiesToolBar toolBar = new PropertiesToolBar();

        public PropertiesPanel() {
            super();
            this.setBackgroundColor("#d3d3d3");

            grid.setWidth(500);
            grid.setHeight(224);
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(true);
            grid.setEditEvent(ListGridEditEvent.CLICK);
            grid.setEditByCell(true);

            ListGridField iconField = new ListGridField("icon", " ");
            iconField.setCanEdit(false);
            iconField.setShowHover(true);
            iconField.setWidth(20);
            iconField.setType(ListGridFieldType.IMAGE);
            iconField.setImageURLPrefix("icons/bullet_");
            iconField.setImageURLSuffix(".png");
            iconField.setTitle(" ");
            iconField.setAlign(Alignment.CENTER);
            
            ListGridField nameField = new ListGridField("name", "Name");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);
            nameField.setIcon("icons/folder_modernist_add.png");

            ListGridField typeField = new ListGridField("type", "Type");
            typeField.setCanEdit(false);
            typeField.setShowHover(true);
            typeField.setWidth(100);
            typeField.setIcon("icons/tag.png");
            
            ListGridField isProtectedField = new ListGridField("isProtected", "Protected");
            isProtectedField.setCanEdit(false);
            isProtectedField.setShowHover(true);
            isProtectedField.setIcon("icons/document_letter_locked.png");
            isProtectedField.setWidth(100);
            isProtectedField.setType(ListGridFieldType.IMAGE);
            isProtectedField.setImageURLPrefix("icons/");
            isProtectedField.setImageURLSuffix(".png");
            isProtectedField.setAlign(Alignment.CENTER);
            
            ListGridField isMultipleField = new ListGridField("isMultiple", "Multiple");
            isMultipleField.setCanEdit(false);
            isMultipleField.setShowHover(true);
            isMultipleField.setIcon("icons/documents.png");
            isMultipleField.setWidth(100);
            isMultipleField.setType(ListGridFieldType.IMAGE);
            isMultipleField.setImageURLPrefix("icons/");
            isMultipleField.setImageURLSuffix(".png");
            isMultipleField.setAlign(Alignment.CENTER);
            
            valueField = new ListGridField("value", "Value");
            valueField.setShowHover(true);
            valueField.setIcon("icons/tag_edit.png");
            
            grid.setFields(iconField, nameField, typeField, isProtectedField, 
                    isMultipleField, valueField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();

            grid.addCellSavedHandler(new CellSavedHandler(){
                @Override
                public void onCellSaved(CellSavedEvent event) {
                    String name = event.getRecord().getAttribute("name");
                    String value = (String) event.getNewValue();
                    console.jcrService.setProperty(path, name, value, new AsyncCallback() {
                        @Override
                        public void onFailure(Throwable caught) {
                            SC.say(caught.getMessage());
                            display(node);
                        }
                        @Override
                        public void onSuccess(Object result) {
                            display(node);
                        }
                    });
                }                
            });
            
//            ListGridRecord listGridRecord = new ListGridRecord();
//            grid.setData(new ListGridRecord[]{listGridRecord});

//        grid.addCellSavedHandler(new PropertiesCellSavedHandler(Explorer.this));
//        grid.addCellClickHandler(new PropertiesCellClickHandler());

//        grid.setContextMenu(createPropertyRightClickMenu());

            addMember(new PropsToolbar(console));
            addMember(grid);
        }

        public void setData(Collection<JcrProperty> props) {            
            ListGridRecord[] data = new ListGridRecord[props.size()];
            int i = 0;
            for (JcrProperty p : props) {
                ListGridRecord record = new ListGridRecord();
                valueField.setType(ListGridFieldType.SEQUENCE);
                record.setAttribute("icon", "blue");
                record.setAttribute("name", p.getName());
                record.setAttribute("type", p.getType());
                record.setAttribute("isProtected", Boolean.toString(p.isProtected()));
                record.setAttribute("isMultiple", Boolean.toString(p.isMultiValue()));
                record.setAttribute("value", p.getValue());
                data[i++] = record;
            }
            grid.setData(data);
            grid.draw();
        }
        
        public void setCanEdit(boolean flag) {
            this.grid.setCanEdit(flag);
        }

        public void refresh() {
//            grid.setData(propertiesListGridRecords);
        }
        
        private ListGridFieldType typeOf(JcrProperty property) {
            if (property.getType().equals("Boolean")) {
                return ListGridFieldType.BOOLEAN;
            }
            return ListGridFieldType.TEXT;
        }
    }

    private class AccessControlPanel extends VLayout {
        private HLayout principalPanel = new HLayout();
        private ComboBoxItem principalCombo = new ComboBoxItem();
        private ListGrid grid = new ListGrid();
        private JcrAccessControlList acl;
        
        public AccessControlPanel() {
            super();
            setHeight(250);
            
            DynamicForm form = new DynamicForm();
            principalPanel.addMember(form);
            principalPanel.setHeight(30);
            principalPanel.addMember(new AclToolbar(console));
            principalPanel.setBackgroundColor("#d3d3d3");
            
            principalCombo.setTitle("Principal");
            principalCombo.addChangeHandler(new ChangeHandler() {
                @Override
                public void onChange(ChangeEvent event) {
                    String principal = (String) event.getValue();
                    displayPermissions(principal);
                }
            });
            
            form.setItems(principalCombo);
            
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(true);
            grid.setEditEvent(ListGridEditEvent.CLICK);
            grid.setEditByCell(true);

            ListGridField iconField = new ListGridField("icon", " ");
            iconField.setCanEdit(false);
            iconField.setShowHover(true);
            iconField.setWidth(20);
            iconField.setType(ListGridFieldType.IMAGE);
            iconField.setImageURLPrefix("icons/bullet_");
            iconField.setImageURLSuffix(".png");
            iconField.setTitle(" ");
            iconField.setAlign(Alignment.CENTER);

            ListGridField signField = new ListGridField("sign", " ");
            signField.setCanEdit(false);
            signField.setShowHover(true);
            signField.setWidth(30);
            signField.setType(ListGridFieldType.IMAGE);
            signField.setImageURLPrefix("icons/");
            signField.setImageURLSuffix(".png");
            signField.setTitle(" ");
            signField.setAlign(Alignment.CENTER);
            
            ListGridField nameField = new ListGridField("permission", "Permission");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);
            nameField.setIcon("icons/sprocket.png");
            
            ListGridField statusField = new ListGridField("status", "Status");
            statusField.setCanEdit(false);
            statusField.setShowHover(true);
            statusField.setIcon("icons/shield.png");
            
            grid.setFields(iconField, nameField, signField, statusField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();
            
            grid.addCellClickHandler(new CellClickHandler() {
                @Override
                public void onCellClick(CellClickEvent event) {
                    Record record = event.getRecord();
                    String action = record.getAttribute("permission");
                    String status = record.getAttribute("status");
                    if (status.equals("Allow")) {
                        record.setAttribute("status", "Deny");
                        acl.modify(principal(), action, "Deny");
                    } else {
                        record.setAttribute("status", "Allow");
                        acl.modify(principal(), action, "Allow");
                    }
                    displayPermissions();
                }
            });
            
            addMember(principalPanel);
            addMember(grid);
        }
        
        public String principal() {
            return principalCombo.getValueAsString();
        }
        
        public void display(JcrAccessControlList acl, String principal) {
            this.acl = acl;
            
            Collection<JcrPolicy> entries = acl.entries();
            String[] principals = new String[entries.size()];
            
            int i = 0;
            for (JcrPolicy entry : entries) {
                principals[i++] = entry.getPrincipal();
            }
            
            principalCombo.setValueMap(principals);
            if (principal != null) {
                principalCombo.setValue(principal);
            } else {
                principalCombo.setValue(principals[0]);
            }
            
            displayPermissions();
        }
        
        /**
         * Displays permissions for the current node and current principal
         */
        private void displayPermissions() {
            String principal = (String)principalCombo.getValue();
            grid.setData(acl.test(principal));
            grid.show();
        }
        
        /**
         * Displays permissions for the current node and current principal
         */
        private void displayPermissions(String principal) {
            grid.setData(acl.test(principal));
            grid.show();
        }
        
    }

    private class GeneralNodeInformationPanel extends VLayout {

        private RecordPane currentNodeName = new RecordPane("Current Node");
        private RecordPane primaryType = new RecordPane("Primary Type");
        private RecordPane versions = new RecordPane("Number of Versions");
        private RecordPane mixins = new RecordPane("Mixin types");
        
        public GeneralNodeInformationPanel() {
            super();
            setWidth100();
            addMember(currentNodeName);
            addMember(primaryType);
            addMember(versions);
            addMember(mixins);            
        }
        
        /**
         * Displays general properties of the given node.
         * 
         * @param node the node to be displayed.
         */
        public void setNode(JcrTreeNode node) {
            currentNodeName.setValue(node.getName());
            primaryType.setValue(node.getPrimaryType());
            versions.setValue("Versions here");
            mixins.setValue(combine(node.getMixins()));
        }
        
        /**
         * Combines list of text items into single line.
         * 
         * @param text 
         * @return 
         */
        private String combine(String[] text) {
            if (text == null) {
                return "";
            }
            
            if (text.length == 1) {
                return text[0];
            }
                        
            String s = text[0];
            for (int i = 1; i < text.length; i++) {
                s += "," + text[i];
            }
            
            return s;
        }
    }

    private class RecordPane extends HLayout {

        private Label valueLabel;

        public RecordPane(String title) {
            Label titleLabel = new Label();
            titleLabel.setContents(title + ":");
            titleLabel.setWidth(200);

            valueLabel = new Label();
            valueLabel.setContents("N/A");

            addMember(titleLabel);
            addMember(valueLabel);
        }

        public void setValue(String value) {
            valueLabel.setContents(value);
        }
    }

    private class Strut extends HLayout {

        public Strut(int size) {
            super();
            setHeight(size);
        }
    }

}
