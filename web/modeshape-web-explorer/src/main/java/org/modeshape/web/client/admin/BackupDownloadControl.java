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
package org.modeshape.web.client.admin;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.layout.VLayout;
import org.modeshape.web.shared.BackupParams;

/**
 *
 * @author kulikov
 */
public class BackupDownloadControl extends VLayout {

    private final DynamicForm form = new DynamicForm();
    private final HiddenItem fileField = new HiddenItem("file");
    
    private final AdminView adminView;
    private final BackupOptionsDialog optionsDialog = new BackupOptionsDialog(this);
    
    public BackupDownloadControl(final AdminView adminView) {
        super();
        this.adminView = adminView;
        
        setStyleName("admin-control");

        form.setItems(fileField);
        
        Label label = new Label("Backup & Download");
        label.setStyleName("button-label");
        label.setHeight(25);
        label.setIcon("icons/data.png");
        label.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                optionsDialog.showModal();
            }
        });

        Canvas text = new Canvas();
        text.setAutoHeight();
        text.setContents("Create backups of an entire repository (even when "
                + "the repository is in use) and download zip archive "
                + "This works regardless of where the repository content " + "is persisted.");

        addMember(label);
        addMember(text);
        addMember(form);

    }

    protected void backupAndDownload(BackupParams params) {
        final String name = Long.toString(System.currentTimeMillis());
        fileField.setValue(name);
        adminView.jcrService().backup(adminView.repository(), name, params, new AsyncCallback<Object>() {
            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @SuppressWarnings("synthetic-access")
            @Override
            public void onSuccess(Object result) {
                form.setAction(GWT.getModuleBaseForStaticFiles() + "backup/da?file=" + name);
                form.setMethod(FormMethod.GET);
                form.submitForm();
            }
        });
    }
}
