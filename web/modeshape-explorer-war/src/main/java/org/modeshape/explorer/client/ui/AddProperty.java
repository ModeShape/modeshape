package org.modeshape.explorer.client.ui;

import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.callback.CRUDServiceCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * 
 * @author James Pickup
 *
 */
public class AddProperty {
	
	public Window addNewPropertyBox(Explorer jackrabbitExplorer) {
		final Window addNewPropertyWindow = new Window();
		addNewPropertyWindow.setShowMinimizeButton(false);  
		addNewPropertyWindow.setIsModal(true);  
		addNewPropertyWindow.setShowModalMask(true);  
		addNewPropertyWindow.centerInPage();
		addNewPropertyWindow.setTitle("Add New Property");
		addNewPropertyWindow.setCanDragReposition(true);
		addNewPropertyWindow.setCanDragResize(true);
		addNewPropertyWindow.setHeight(200);
		addNewPropertyWindow.setWidth(350);
		addNewPropertyWindow.setAutoCenter(true);
		final TextItem newPropName = new TextItem();
		final TextItem newPropValue = new TextItem();
		
		final DynamicForm addNewPropForm = new DynamicForm();
		addNewPropForm.setPadding(10);
		addNewPropForm.setNumCols(2);
		newPropName.setName("newPropName");
		newPropName.setTitle("Name");
		newPropName.setWidth(250);
		newPropName.setRequired(true);
		newPropValue.setName("newPropValue");
		newPropValue.setTitle("Value");
		newPropValue.setWidth(250);
		SubmitItem addPropertySubmitItem = new SubmitItem("addProperty");
	    addPropertySubmitItem.setTitle("Add Property");
	    addPropertySubmitItem.setWidth(100);
	    class AddPropertySubmitValuesHandler implements SubmitValuesHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public AddPropertySubmitValuesHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	    	public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
	        	if (addNewPropForm.validate()) {
		        	String path = Explorer.cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
		        	Explorer.showLoadingImg();
		        	Explorer.service.addNewProperty(path, newPropName.getValue().toString(), newPropValue.getValue().toString(),
		        			new CRUDServiceCallback(jackrabbitExplorer, path, null));
	        	}
	      }  
	    };
	    addNewPropForm.addSubmitValuesHandler(new AddPropertySubmitValuesHandler(jackrabbitExplorer));
	    addNewPropForm.setSaveOnEnter(true);
	    class AddPropertyButtonHandler implements ClickHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public AddPropertyButtonHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	        public void onClick(ClickEvent event) {  
	        	if (addNewPropForm.validate()) {
		        	String path = Explorer.cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
		        	Explorer.showLoadingImg();
		        	Explorer.service.addNewProperty(path, newPropName.getValue().toString(), newPropValue.getValue().toString(),
		        			new CRUDServiceCallback(jackrabbitExplorer, path, null));
	        	}
	      }  
	    };
	    addPropertySubmitItem.addClickHandler(new AddPropertyButtonHandler(jackrabbitExplorer));
	    SubmitItem cancelAddPropertySubmitItem = new SubmitItem("cancelAddProperty");
	    cancelAddPropertySubmitItem.setTitle("Cancel");
	    cancelAddPropertySubmitItem.setWidth(100);
	    class CancelAddPropertyButtonHandler implements ClickHandler {  
	        public void onClick(ClickEvent event) {  
	        	addNewPropertyWindow.destroy();
	      }  
	    };
	    cancelAddPropertySubmitItem.addClickHandler(new CancelAddPropertyButtonHandler());
	    VStack vStack = new VStack();
	    SpacerItem spacerItem1 = new SpacerItem();
	    spacerItem1.setStartRow(true);
	    spacerItem1.setEndRow(true);
	    addPropertySubmitItem.setStartRow(true);
	    addPropertySubmitItem.setEndRow(false);
	    cancelAddPropertySubmitItem.setStartRow(false);
	    cancelAddPropertySubmitItem.setEndRow(true);
        addNewPropForm.setItems(newPropName, newPropValue, spacerItem1);        
        vStack.addMember(addNewPropForm);
		DynamicForm addNewPropertySubmitForm = new DynamicForm();
		addNewPropertySubmitForm.setNumCols(2);
		addNewPropertySubmitForm.setItems(addPropertySubmitItem, cancelAddPropertySubmitItem);
        //vStack.setTop(50);
        HStack hStack = new HStack();
        hStack.addMember(addNewPropertySubmitForm);
        hStack.setHeight(10);
        hStack.setWidth100();
        hStack.setAlign(Alignment.RIGHT);
        vStack.setHeight(100);
        vStack.setWidth100();
        vStack.setPadding(10);
        vStack.addMember(hStack);
        addNewPropertyWindow.addItem(vStack);
        addNewPropertyWindow.show();
        newPropName.focusInItem();
		return addNewPropertyWindow;
	}
}
