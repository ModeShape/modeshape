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
package org.modeshape.web.shared;

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.FormItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import com.smartgwt.client.widgets.form.fields.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VStack;

/**
 * Implements basic features of "OK-Cancel" dialog.
 * 
 * @author kulikov
 */
public abstract class ModalDialog {
    //components required to display dialog window
    private Window window = new Window();
    private DynamicForm form = new DynamicForm();
    
    //ok and cancel buttons
    private SubmitItem confirmButton = new SubmitItem("OK");
    private SubmitItem cancelButton = new SubmitItem("Cancel");
    private VStack vStack = new VStack();
    
    /**
     * Creates new Dialog.
     * 
     * @param title title of the dialog window.
     * @param width the width of the dialog.
     * @param height the height of the dialog.
     */
    public ModalDialog(String title, int width, int height) {
        form.setNumCols(2);
        form.setPadding(25);
        
        vStack.setTop(10);
        vStack.addMember(form);
        
        window.addChild(vStack);        
        window.setTitle(title);
        window.setCanDragReposition(true);
        window.setCanDragResize(false);
        window.setShowMinimizeButton(false);
        window.setShowCloseButton(true);
        window.setWidth(width);
        window.setHeight(height);
        window.setAutoCenter(true);
        
        window.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                hide();
            }            
        });
        
        confirmButton.setTitle("OK");
        confirmButton.setWidth(100);
        confirmButton.setStartRow(true);
        confirmButton.setEndRow(false);
        confirmButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onConfirm(event);
                hide();
            }            
        });
        
        cancelButton.setTitle("Cancel");
        cancelButton.setWidth(100);
        cancelButton.setStartRow(false);
        cancelButton.setEndRow(true);
        cancelButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                hide();
            }            
        });
        
    }

    protected void addMember(Canvas canvas) {
        vStack.addMember(canvas);
    }
    
    /**
     * Adds controls to this dialog.
     * 
     * @param items controls
     */
    public void setControls(FormItem... items) {
        FormItem[] controls = new FormItem[items.length + 3];

        int i = 0;
        for (FormItem item : items) {
            controls[i++] = item;
        }
        
        controls[i++] = new SpacerItem();
        controls[i++] = confirmButton;
        controls[i++] = cancelButton;
        
        form.setItems(controls);
    }
    
    /**
     * Shows this dialog modal.
     */
    public void showModal() {
        window.show();
    }
    
    /**
     * Hides this dialog.
     */
    public void hide() {
        window.hide();
    }
    
    /**
     * Executes action when 'OK' button clicked.
     * 
     * @param event 
     */
    public abstract void onConfirm(ClickEvent event);
    
    public void setAction(String action) {
        form.setAction(action);
    }
    
    public void submitForm() {
        form.submitForm();
    }
    
    protected DynamicForm form() {
        return form;
    }
    
    protected Window window() {
        return window;
    }
}
