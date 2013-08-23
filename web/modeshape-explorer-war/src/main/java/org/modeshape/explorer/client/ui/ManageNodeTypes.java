package org.modeshape.explorer.client.ui;

import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.callback.AddNodeTypesServiceCallback;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.tab.Tab;

/**
 *
 * @author James Pickup
 *
 */
public class ManageNodeTypes {

    private TextAreaItem addNodeTypesTxt = new TextAreaItem();

    public Tab nodeTypeTab() {
        Tab nodeTypeTab = new Tab();
        nodeTypeTab.setTitle("Manage Node Types");

        final DynamicForm nodeTypeForm = new DynamicForm();
        nodeTypeForm.setID("nodeTypeForm");
        nodeTypeForm.setNumCols(3);
        addNodeTypesTxt.setName("addNodeTypesTxt");
        addNodeTypesTxt.setTitle("Enter CND");
        addNodeTypesTxt.setWidth(250);
        addNodeTypesTxt.setRequired(true);
        SubmitItem nodeTypeSubmitItem = new SubmitItem("nodeTypeSubmitItem");
        nodeTypeSubmitItem.setTitle("Submit");
        nodeTypeSubmitItem.setWidth(100);
        class AddNodeTypesSubmitValuesHandler implements SubmitValuesHandler {

            public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
                if (nodeTypeForm.validate()) {
                    Explorer.showLoadingImg();
                    Explorer.service.addNodeTypes(addNodeTypesTxt.getValue().toString(), new AddNodeTypesServiceCallback());
                }
            }
        };
        nodeTypeForm.addSubmitValuesHandler(new AddNodeTypesSubmitValuesHandler());
        nodeTypeForm.setSaveOnEnter(true);
        addNodeTypesTxt.setStartRow(true);
        addNodeTypesTxt.setEndRow(false);
        nodeTypeSubmitItem.setStartRow(false);
        nodeTypeSubmitItem.setEndRow(true);
        nodeTypeForm.setItems(addNodeTypesTxt, nodeTypeSubmitItem);
        nodeTypeTab.setPane(nodeTypeForm);
        return nodeTypeTab;
    }
}
