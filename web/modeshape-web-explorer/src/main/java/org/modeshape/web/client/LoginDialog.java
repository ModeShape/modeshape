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

import com.google.gwt.user.client.History;
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
    private TextItem jndiName = new TextItem();
    private TextItem userName = new TextItem();
    private TextItem workspace = new TextItem();
    private PasswordItem password = new PasswordItem();

    private Console console;
    
    public LoginDialog(Console console) {
        super();
        this.console = console;
        
        setID("loginDialog");
        setNumCols(2);
        setPadding(25);

        jndiName.setName("jndiName");
        jndiName.setTitle("Repository name");
        jndiName.setDefaultValue("jcr/sample");
        jndiName.setWidth(250);
        jndiName.setRequired(true);
        jndiName.setVisible(true);

        workspace.setName("workspace");
        workspace.setTitle("Workspace");
        workspace.setDefaultValue("default");
        workspace.setWidth(250);
        workspace.setRequired(true);

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
        
        this.addSubmitValuesHandler(new LoginHandler());
        
        StaticTextItem description = new StaticTextItem();
        description.setTitle("");
        description.setValue("Specify either repository name or jndi name");
        description.setStartRow(true);
        description.setEndRow(true);
        
        setItems(spacerItem1, description, jndiName, workspace, spacerItem1, userName,
                password,  spacerItem2, okButton);

        vStack.setTop(30);
        vStack.addMember(this);

        window.addChild(vStack);
        window.setTitle("Login");
        window.setCanDragReposition(true);
        window.setCanDragResize(false);
        window.setShowMinimizeButton(false);
        window.setShowCloseButton(false);
        window.setHeight(350);
        window.setWidth(400);
        window.setAutoCenter(true);
        window.show();

        userName.focusInItem();
    }

    public void setJndiName(String name) {
        this.jndiName.setValue(name);
    }
    
    public void setWorkspace(String name) {
        this.workspace.setValue(name);
    }
    
    public void showDialog() {
        window.show();
    }

    public void hideDialog() {
        window.hide();
    }
    
    private class LoginHandler implements SubmitValuesHandler {
        @Override
        public void onSubmitValues(SubmitValuesEvent event) {
            console.jcrService.login(
                    jndiName.getValueAsString(), 
                    userName.getValueAsString(), 
                    password.getValueAsString(),
                    workspace.getValueAsString(), 
                    new LoginCallback());
        }        
    }
    
    private class LoginCallback implements AsyncCallback {

        @Override
        public void onFailure(Throwable caught) {
            SC.say(caught.getMessage());
        }

        @Override
        public void onSuccess(Object result) {
            LoginDialog.this.hideDialog();
            console.showMainForm(jndiName.getValueAsString(), workspace.getValueAsString());
        }
        
    }
}
