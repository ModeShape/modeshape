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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import java.util.Collection;
import org.modeshape.web.shared.JcrNodeType;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.Param;

/**
 *
 * @author kulikov
 */
public class RepositoryPanel extends Tab {

    private final RepositoryInfoPanel repositoryInfo;
    private final NodeTypesPanel nodeTypes;
    private Console console;
    
    public RepositoryPanel(Console console) {
        super();
        this.console = console;
        setTitle("Repository");
        setIcon("icons/view_thumbnail.png");
        
        repositoryInfo = new RepositoryInfoPanel();
        nodeTypes = new NodeTypesPanel();

        VLayout layout = new VLayout();
        SectionStack stack = new SectionStack();

        SectionStackSection section1 = new SectionStackSection("Repository descriptor");
        section1.setExpanded(true);
        section1.addItem(repositoryInfo);
        stack.addSection(section1);


        SectionStackSection section2 = new SectionStackSection("Node type management");
        section2.setExpanded(false);
        section2.addItem(nodeTypes);
        stack.addSection(section2);

        layout.addMember(stack);
        setPane(layout);
    }

    public void showRepositoryInfo() {
    }

    public void display() {
        console.jcrService.repositoryInfo(new RepositoryInfoCallbackHandler());
        nodeTypes.display();
    }
    
    private class RepositoryInfoPanel extends VLayout {

        private ListGrid grid = new ListGrid();

        public RepositoryInfoPanel() {
            super();

            grid.setWidth(500);
            grid.setHeight(224);
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(true);
            grid.setEditEvent(ListGridEditEvent.CLICK);
            grid.setEditByCell(true);

            ListGridField iconField = new ListGridField("icon", " ");
            iconField.setType(ListGridFieldType.IMAGE);
            iconField.setImageURLPrefix("icons/bullet_");
            iconField.setImageURLSuffix(".png");
            iconField.setWidth(20);
            
            ListGridField nameField = new ListGridField("name", "Name");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);

            ListGridField valueField = new ListGridField("value", "Value");
            valueField.setShowHover(true);

            grid.setFields(iconField, nameField, valueField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();

            addMember(grid);
        }
    }

    
    private class NodeTypesPanel extends VLayout {
        private ListGrid grid = new ListGrid();

        public NodeTypesPanel() {
            super();
            
            grid.setWidth(500);
            grid.setHeight(224);
            grid.setAlternateRecordStyles(true);
            grid.setShowAllRecords(true);
            grid.setCanEdit(false);

            ListGridField iconField = new ListGridField("icon", " ");
            iconField.setType(ListGridFieldType.IMAGE);
            iconField.setImageURLPrefix("icons/bullet_");
            iconField.setImageURLSuffix(".png");
            iconField.setWidth(20);
            
            ListGridField nameField = new ListGridField("name", "Name");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);

            ListGridField isPrimaryField = new ListGridField("isPrimary", "Primary");
            isPrimaryField.setCanEdit(false);
            isPrimaryField.setShowHover(true);

            ListGridField isMixinField = new ListGridField("isMixin", "Mixin");
            isMixinField.setCanEdit(false);
            isMixinField.setShowHover(true);

            ListGridField isAbstractField = new ListGridField("isAbstract", "Abstract");
            isAbstractField.setCanEdit(false);
            isAbstractField.setShowHover(true);
            
            grid.setFields(iconField, nameField, isPrimaryField, isMixinField, isAbstractField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();

            addMember(grid);
            
        }
        
        public void display() {
            console.jcrService.nodeTypes(new AsyncCallback<Collection<JcrNodeType>>() {

                @Override
                public void onFailure(Throwable caught) {
                    SC.say(caught.getMessage());
                }


                @Override
                public void onSuccess(Collection<JcrNodeType> types) {
                    ListGridRecord[] data = new ListGridRecord[types.size()];
                    int i = 0;
                    for (JcrNodeType type: types) {
                        ListGridRecord record = new ListGridRecord();
                        record.setAttribute("icon", "blue");
                        record.setAttribute("name", type.getName());
                        record.setAttribute("isPrimary", type.isPrimary());
                        record.setAttribute("isMixin", type.isMixin());
                        record.setAttribute("isAbstract", type.isAbstract());
                        data[i++] = record;                        
                    }
                    grid.setData(data);
                }
            });
        }
    }

    public class RepositoryInfoCallbackHandler implements AsyncCallback<JcrRepositoryDescriptor> {

        @Override
        public void onFailure(Throwable caught) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onSuccess(JcrRepositoryDescriptor descriptor) {
            Collection<Param> params = descriptor.info();
            ListGridRecord[] data = new ListGridRecord[params.size()];
            int i = 0;
            for (Param p : params) {
                ListGridRecord record = new ListGridRecord();
                record.setAttribute("icon", "blue");
                record.setAttribute("name", p.getName());
                record.setAttribute("value", p.getValue());
                data[i++] = record;
            }
            RepositoryPanel.this.repositoryInfo.grid.setData(data);
            RepositoryPanel.this.repositoryInfo.draw();
        }
    }
}