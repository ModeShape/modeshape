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
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.VStack;

/**
 *
 * @author kulikov
 */
public class LoginDialog extends DynamicForm {

    private Window window = new Window();
    private TextItem userName = new TextItem();
    private PasswordItem password = new PasswordItem();

    public LoginDialog(final Console console) {
        super();
        
        setID("loginDialog");
        setNumCols(2);
        setPadding(25);

        userName.setName("username");
        userName.setTitle("Username");
        userName.setDefaultValue("");
        userName.setWidth(250);
        userName.setRequired(true);

        password.setName("password");
        password.setTitle("Password");
        password.setDefaultValue("");
        password.setWidth(250);

        SubmitItem okButton = new SubmitItem("login");
        okButton.setTitle("Login");
        okButton.setWidth(100);

        VStack vStack = new VStack();

        SpacerItem spacerItem1 = new SpacerItem();
        SpacerItem spacerItem2 = new SpacerItem();
        spacerItem1.setStartRow(true);
        spacerItem1.setEndRow(true);
        spacerItem2.setStartRow(true);
        spacerItem2.setEndRow(false);

        okButton.setStartRow(false);
        okButton.setEndRow(true);
        
        this.addSubmitValuesHandler(new SubmitValuesHandler() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                console.jcrService.login(userName.getValueAsString(), 
                    password.getValueAsString(), new AsyncCallback<Object>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Object result) {
                        LoginDialog.this.hideDialog();
                        console.updateUserName(userName.getValueAsString());
                    }
                });
            }
        });
        
        StaticTextItem description = new StaticTextItem();
        description.setTitle("");
        description.setValue("Please specify your username and password");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setItems(description, userName, password,  spacerItem2, okButton);

        vStack.setTop(30);
        vStack.addMember(this);

        window.addChild(vStack);
        window.setTitle("Login");
        window.setCanDragReposition(true);
        window.setCanDragResize(false);
        window.setShowMinimizeButton(false);
        window.setShowCloseButton(false);
        window.setHeight(200);
        window.setWidth(400);
        window.setAutoCenter(true);
        window.show();

        userName.focusInItem();
    }

    public void showDialog() {
        window.show();
    }

    public void hideDialog() {
        window.hide();
    }
    
}
