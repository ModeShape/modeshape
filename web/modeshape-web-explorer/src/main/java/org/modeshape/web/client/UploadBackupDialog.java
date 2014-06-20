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

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.NamedFrame;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.widgets.form.fields.FileItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;

/**
 * Dialog asking backup directory.
 *
 * @author kulikov
 */
public class UploadBackupDialog extends ModalDialog {
    final String TARGET = "uploadTarget";
    private FileItem name = new FileItem();

    public UploadBackupDialog(Contents contents) {
        super("Backup", 400, 200);

        StaticTextItem description = new StaticTextItem("");
        description.setValue("Specify backup name");

        setControls(description, name);
        setAction(GWT.getModuleBaseForStaticFiles() + "restore/do");

        NamedFrame frame = new NamedFrame(TARGET);
        frame.setWidth("1px");
        frame.setHeight("1px");
        frame.setVisible(false);
        
        form().setEncoding(Encoding.MULTIPART);
        form().setMethod(FormMethod.POST);
        form().setTarget(TARGET);
        
        window().addMember(frame);
    }

    @Override
    public void onConfirm(ClickEvent event) {
        this.submitForm();
    }

    
}
