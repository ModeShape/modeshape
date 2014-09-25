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
