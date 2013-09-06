/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.PasswordItem;
import com.smartgwt.client.widgets.form.fields.SpacerItem;
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
        jndiName.setTitle("jndiname");
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
        
        setItems(spacerItem1, jndiName, spacerItem1, userName,
                password, workspace, spacerItem1, spacerItem2, okButton);

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
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onSuccess(Object result) {
            LoginDialog.this.hideDialog();
            console.showMainForm();
        }
        
    }
}
