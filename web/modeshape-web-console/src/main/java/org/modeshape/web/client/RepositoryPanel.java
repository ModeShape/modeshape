/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridEditEvent;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.layout.SectionStack;
import com.smartgwt.client.widgets.layout.SectionStackSection;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.tab.Tab;
import java.util.Collection;
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

            ListGridField nameField = new ListGridField("name", "Name");
            nameField.setCanEdit(false);
            nameField.setShowHover(true);

            ListGridField valueField = new ListGridField("value", "Value");
            valueField.setShowHover(true);

            grid.setFields(nameField, valueField);

            grid.setCanResizeFields(true);
            grid.setWidth100();
            grid.setHeight100();

            addMember(grid);
        }
    }

    public void display() {
            console.jcrService.repositoryInfo(new RepositoryInfoCallbackHandler());
    }
    
    private class NodeTypesPanel extends VLayout {

        public NodeTypesPanel() {
            super();
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
                record.setAttribute("name", p.getName());
                record.setAttribute("value", p.getValue());
                data[i++] = record;
            }
            RepositoryPanel.this.repositoryInfo.grid.setData(data);
            RepositoryPanel.this.repositoryInfo.draw();
        }
    }
}