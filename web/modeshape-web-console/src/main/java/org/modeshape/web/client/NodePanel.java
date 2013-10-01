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
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.VisibilityMode;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.CellSavedEvent;
import com.smartgwt.client.widgets.grid.events.CellSavedHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.menu.IMenuButton;
import com.smartgwt.client.widgets.menu.Menu;
import com.smartgwt.client.widgets.menu.MenuItem;
import com.smartgwt.client.widgets.menu.MenuItemSeparator;
import com.smartgwt.client.widgets.tab.Tab;
import java.util.Collection;
import org.modeshape.web.shared.JcrACLEntry;
import org.modeshape.web.shared.JcrAccessControlList;
import org.modeshape.web.shared.JcrPermissions;
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
        accessControl.display(node.getAccessList());
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

    private class PropertiesPanel extends VLayout {

        private ListGrid grid = new ListGrid();
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

            ListGridField nameField = new ListGridField("name", "Name");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);

            ListGridField typeField = new ListGridField("type", "Type");
            typeField.setCanEdit(false);
            typeField.setShowHover(true);
            
            ListGridField valueField = new ListGridField("value", "Value");
            valueField.setShowHover(true);

            grid.setFields(nameField, typeField, valueField);

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

//            addMember(toolBar);
            addMember(grid);
        }

        public void setData(Collection<JcrProperty> props) {
            ListGridRecord[] data = new ListGridRecord[props.size()];
            int i = 0;
            for (JcrProperty p : props) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("name", p.getName());
                record.setAttribute("type", p.getType());
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
    }

    private class PropertiesToolBar extends HLayout {

        public PropertiesToolBar() {
            super();
            setMargin(3);

            Menu nodeMenu = new Menu();
            nodeMenu.setShowShadow(true);
            nodeMenu.setShadowDepth(10);

            MenuItem newNode = new MenuItem("New", "icons/26.png", "Ctrl+N");
            MenuItem renameNode = new MenuItem("Rename node", "icons/69.png", "Ctrl+R");
            MenuItem removeNode = new MenuItem("Remove node", "icons/33.png", "Ctrl+D");

            MenuItemSeparator separator = new MenuItemSeparator();

            nodeMenu.setItems(newNode, renameNode, separator, removeNode);

            IMenuButton nodeButton = new IMenuButton("Node", nodeMenu);
            nodeButton.setWidth(100);
            
            this.addMember(nodeButton);

            Menu propsMenu = new Menu();
            propsMenu.setShowShadow(true);
            propsMenu.setShadowDepth(10);

            MenuItem newProperty = new MenuItem("New");
            MenuItem renameProperty = new MenuItem("Rename node");
            MenuItem removeProperty = new MenuItem("Remove node");

            propsMenu.setItems(newProperty, renameProperty, separator, removeProperty);

            IMenuButton propertyButton = new IMenuButton("Property", propsMenu);
            propertyButton.setWidth(100);
            
//            this.addMember(new Strut(5));            
            this.addMember(propertyButton);
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
            
            principalCombo.setTitle("Principal");
            form.setItems(principalCombo);
            
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(true);
            grid.setEditEvent(ListGridEditEvent.CLICK);
            grid.setEditByCell(true);

            ListGridField nameField = new ListGridField("permission", "Permission");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);

            ListGridField statusField = new ListGridField("status", "Status");
            statusField.setCanEdit(true);
            statusField.setShowHover(true);
            

            grid.setFields(nameField, statusField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();
            
            addMember(principalPanel);
            addMember(grid);
        }
        
        public void display(JcrAccessControlList acl) {
            this.acl = acl;
            
            Collection<JcrACLEntry> entries = acl.entries();
            String[] principals = new String[entries.size()];
            
            int i = 0;
            for (JcrACLEntry entry : entries) {
                principals[i++] = entry.getPrincipal();
            }
            
            principalCombo.setValueMap(principals);
            principalCombo.setValue(principals[0]);
            
            displayPermissions();
        }
        
        /**
         * Displays permissions for the current node and current principal
         */
        private void displayPermissions() {
            JcrPermissions permissions = new JcrPermissions();
            String principal = (String)principalCombo.getValue();
            
            //look up access list
            JcrACLEntry list = null;
            for (JcrACLEntry entry : acl.entries()) {
                if (entry.getPrincipal().equals(principal)) {
                    list = entry;
                    break;
                }
            }
            
            if (list != null) {
            //test permissions and build grid
                ListGridRecord[] records = permissions.test(list.getPermissions());
                
                grid.setData(records);
                grid.show();
            } 
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
