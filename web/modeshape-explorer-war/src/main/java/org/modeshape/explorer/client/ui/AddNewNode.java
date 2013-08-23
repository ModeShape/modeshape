package org.modeshape.explorer.client.ui;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.callback.AvailableNodeTypesServiceCallback;
import org.modeshape.explorer.client.callback.CRUDServiceCallback;
import org.modeshape.explorer.client.fileupload.Upload;
import org.modeshape.explorer.client.fileupload.UploadListener;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.SelectItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HStack;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * 
 * @author James Pickup
 *
 */
public class AddNewNode {
	public static String fileNameUploaded = "";
	
	public Window addNewNodeBox(Explorer jackrabbitExplorer) {
		final Window addNewNodeWindow = new Window();
		addNewNodeWindow.setShowMinimizeButton(false);  
		addNewNodeWindow.setIsModal(true);  
		addNewNodeWindow.setShowModalMask(true);  
		//addNewNodeWindow.centerInPage();
        addNewNodeWindow.setTitle("Add New Node");
		addNewNodeWindow.setCanDragReposition(true);
		addNewNodeWindow.setCanDragResize(true);
		addNewNodeWindow.setHeight(200);
		addNewNodeWindow.setWidth(550);
		addNewNodeWindow.setTop("30%");
		addNewNodeWindow.setLeft("35%");
		//addNewNodeWindow.setAutoCenter(true);

        final DynamicForm addNewNodeForm = new DynamicForm();
		//addNewNodeForm.setPadding(10);s
		addNewNodeForm.setNumCols(2);

		final TextItem newNodeName = new TextItem();
		newNodeName.setName("newNodeName");
		newNodeName.setTitle("Node&nbsp;Name");
		newNodeName.setWidth(250);
		newNodeName.setRequired(true);
		
		final ComboBoxItem newNodeType = new ComboBoxItem();
		if (!AvailableNodeTypesServiceCallback.valueMap.containsKey(AvailableNodeTypesServiceCallback.DEFAULT_NODE_TYPE)
				&& !AvailableNodeTypesServiceCallback.valueMap.containsValue(AvailableNodeTypesServiceCallback.DEFAULT_NODE_TYPE)) {
			AvailableNodeTypesServiceCallback.valueMap.put(AvailableNodeTypesServiceCallback.DEFAULT_NODE_TYPE, AvailableNodeTypesServiceCallback.DEFAULT_NODE_TYPE);
		}
		newNodeType.setValueMap(AvailableNodeTypesServiceCallback.valueMap);
		newNodeType.setDefaultValue(AvailableNodeTypesServiceCallback.DEFAULT_NODE_TYPE);
		newNodeType.setType("Select");
		//newNodeType.setName("newNodeType");
		newNodeType.setTitle("Primary&nbsp;Type");
		newNodeType.setWidth(250);
		newNodeType.setRequired(true);
		SubmitItem addNodeSubmitItem = new SubmitItem("addNode");
		addNodeSubmitItem.setTitle("Add Node");
		addNodeSubmitItem.setWidth(100);
	    class AddNodeSubmitValuesHandler implements SubmitValuesHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public AddNodeSubmitValuesHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	    	public void onSubmitValues(com.smartgwt.client.widgets.form.events.SubmitValuesEvent event) {
	        	if (addNewNodeForm.validate()) {
	        		String path = Explorer.cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
	        		Explorer.showLoadingImg();
	        		Explorer.service.addNewNode(path, newNodeName.getValue().toString(), newNodeType.getValue().toString(),
							fileNameUploaded, false, new CRUDServiceCallback(jackrabbitExplorer, path, null));
	        	}
	        }
	   };
	    
	    addNewNodeForm.addSubmitValuesHandler(new AddNodeSubmitValuesHandler(jackrabbitExplorer));
	    addNewNodeForm.setSaveOnEnter(true);
	    class AddNodeButtonHandler implements ClickHandler {  
	    	private Explorer jackrabbitExplorer;
	    	public AddNodeButtonHandler(Explorer jackrabbitExplorer) {
	    		this.jackrabbitExplorer = jackrabbitExplorer;
	    	}
	        public void onClick(ClickEvent event) {  
	        	if (addNewNodeForm.validate()) {
	        		String path = Explorer.cellMouseDownTreeGrid.getSelectedRecord().getAttribute("path");
	        		Explorer.showLoadingImg();
	        		Explorer.service.addNewNode(path, newNodeName.getValue().toString(), newNodeType.getValue().toString(),
							fileNameUploaded, false, new CRUDServiceCallback(jackrabbitExplorer, path, null));
	        	}
	      }  
	    };
	    addNodeSubmitItem.addClickHandler(new AddNodeButtonHandler(jackrabbitExplorer));
	    SubmitItem cancelAddNodeSubmitItem = new SubmitItem("cancelAddNode");
	    cancelAddNodeSubmitItem.setTitle("Cancel");
	    cancelAddNodeSubmitItem.setWidth(100);
	    class CancelAddNodeButtonHandler implements ClickHandler {  
	        public void onClick(ClickEvent event) {  
	        	//service call to remove sessionId temp direcotry
				Explorer.service.addNewNode("", "", "","", true, new AsyncCallback<String>() {
					@Override
					public void onSuccess(String result) {
					}
					@Override
					public void onFailure(Throwable caught) {
					}
				});
				addNewNodeWindow.destroy();
	      }  
	    };
	    
	    addNewNodeWindow.addCloseClickHandler(new CloseClickHandler() {  
            public void onCloseClick(CloseClientEvent event) {  
	        	//service call to remove sessionId temp direcotry
				Explorer.service.addNewNode("", "", "","", true, new AsyncCallback<String>() {
					@Override
					public void onSuccess(String result) {
					}
					@Override
					public void onFailure(Throwable caught) {
					}
				});
				addNewNodeWindow.destroy();
            }  
        }); 
        
	    cancelAddNodeSubmitItem.addClickHandler(new CancelAddNodeButtonHandler());
	    VStack addNodeVStack = new VStack();
	    SpacerItem spacerItem1 = new SpacerItem();
	    spacerItem1.setStartRow(true);
	    spacerItem1.setEndRow(true);
	    addNodeSubmitItem.setStartRow(true);
	    addNodeSubmitItem.setEndRow(false);
	    cancelAddNodeSubmitItem.setStartRow(false);
	    cancelAddNodeSubmitItem.setEndRow(true);
	    addNewNodeForm.setItems(newNodeName, newNodeType, spacerItem1);
	    addNodeVStack.addMember(addNewNodeForm);
		DynamicForm addNewNodeSubmitForm = new DynamicForm();
		addNewNodeSubmitForm.setNumCols(2);
		addNewNodeSubmitForm.setItems(addNodeSubmitItem, cancelAddNodeSubmitItem);
		addNodeVStack.setTop(50);
        HStack hStack = new HStack();
        hStack.addMember(addNewNodeSubmitForm);
        hStack.setHeight(25);
        hStack.setWidth100();
        hStack.setAlign(Alignment.RIGHT);
        addNodeVStack.setWidth100();
        addNodeVStack.setPadding(10);
        addNodeVStack.addMember(hStack);
        
        //File Upload
        Label jcrContentLabel = new Label("<nobr><b>jcr:content - binary data upload</b></nobr>");
        jcrContentLabel.setHeight(20);
        VStack fileUploadedStatusVStack = new VStack();
        fileUploadedStatusVStack.setVisible(false);
        HStack fileUploadedStatusHStack = new HStack();
        Img fileUploadedImg = new Img("icons/ok.png", 16, 16);
        Label fileUploadLabel = new Label();
        fileUploadLabel.setID("fileUploadLabel");
        fileUploadedStatusHStack.addMember(fileUploadedImg);
        fileUploadedStatusHStack.addMember(fileUploadLabel);
        fileUploadedStatusHStack.setHeight(16);
        fileUploadedStatusHStack.setPadding(10);
        fileUploadedStatusHStack.setVisible(false);
		Upload upload = new Upload();
		class FileUploadListener implements UploadListener {
			HStack fileUploadedStatusHStack = null;
			FileUploadListener(HStack fileUploadedStatusHStack) {
				this.fileUploadedStatusHStack = fileUploadedStatusHStack;
			}
			@Override
			public void uploadComplete(String fileName) {
				fileUploadedStatusHStack.setVisible(true);
				fileUploadedStatusHStack.getMember("fileUploadLabel").setContents(
						"<nobr>&nbsp;" + fileName + " - Uploaded successfully.</nobr>");
				fileNameUploaded = fileName;
			}
			@Override
			public void uploadFailed(String msg) {
				SC.warn("Upload failed: " + msg);
			}
		};
		upload.setUploadListener(new FileUploadListener(fileUploadedStatusHStack));
		upload.setAction("/jackrabbitexplorer/UploadServlet");
		fileUploadedStatusVStack.addMember(jcrContentLabel);
		fileUploadedStatusVStack.addMember(fileUploadedStatusHStack);
		fileUploadedStatusVStack.addMember(upload);
		addNodeVStack.addMember(fileUploadedStatusVStack);
        addNewNodeWindow.addItem(addNodeVStack);
        //addNewNodeWindow.addMember(upload);
		
		class NodeTypeChangedHandler implements ChangedHandler {
			VStack fileUploadedStatusVStack = null;
			NodeTypeChangedHandler(VStack fileUploadedStatusVStack) {
				this.fileUploadedStatusVStack = fileUploadedStatusVStack;
			}
			@Override
			public void onChanged(ChangedEvent event) {
				if (event.getValue().toString().contains("file") || event.getValue().toString().contains("File")) {
					addNewNodeWindow.setHeight(315);
					fileUploadedStatusVStack.setVisible(true);
				} else {
					addNewNodeWindow.setHeight(200);
					fileUploadedStatusVStack.setVisible(false);
				}
			}
		};
		newNodeType.addChangedHandler(new NodeTypeChangedHandler(fileUploadedStatusVStack));
		addNewNodeWindow.show();
		newNodeName.focusInItem();
		return addNewNodeWindow;
	}
}
