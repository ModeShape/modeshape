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
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import org.modeshape.web.shared.ModalDialog;

/**
 * Dialog asking backup directory.
 *
 * @author kulikov
 */
public class UploadBackupDialog extends ModalDialog {
    
    private final HiddenItem repositoryField = new HiddenItem("repository");
    private final UploadItem fileItem = new UploadItem("Upload content");
    private final CheckboxItem incBinaries = new CheckboxItem("Include binaries");
    private final CheckboxItem reindexOnFinish = new CheckboxItem("Reindex on finish");
    private final UploadRestoreControl control;
    
    public UploadBackupDialog(UploadRestoreControl control) {
        super("Upload", 400, 200);
        this.control = control;
        
        StaticTextItem description = new StaticTextItem("");
        description.setValue("Specify backup name");

        setControls(description,repositoryField, fileItem, incBinaries, reindexOnFinish);
        setAction(GWT.getModuleBaseForStaticFiles() + "backup-upload/content");

        form().setEncoding(Encoding.MULTIPART);
        form().setMethod(FormMethod.POST);
        form().setAction(GWT.getModuleBaseForStaticFiles() + "backup-upload/content");
    }

    @Override
    public void onConfirm(ClickEvent event) {
        control.showLoadIcon();
        this.submitForm();
    }

    public void showModal(String repository) {
        repositoryField.setValue(repository);
        showModal();
    }
}
