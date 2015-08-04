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

import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import org.modeshape.web.client.ModalDialog;
import org.modeshape.web.shared.BackupParams;

/**
 * Dialog asking backup directory.
 * 
 * @author kulikov
 */
public class BackupOptionsDialog extends ModalDialog {
    
    private final CheckboxItem incBinariesField = new CheckboxItem("Include binaries");
    private final CheckboxItem compressField = new CheckboxItem("Compress files");
    private final TextItem docsPerFileField = new TextItem("Documents per file");
    private final BackupDownloadControl control;
    
    public BackupOptionsDialog(BackupDownloadControl control) {
        super("Backup", 400, 200);
        this.control = control;
        
        setDefaults();        
        setControls(incBinariesField, compressField, docsPerFileField);
    }
    
    private void setDefaults() {
        incBinariesField.setValue(true);
        compressField.setValue(true);
        docsPerFileField.setValue("10000");
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        //prepare clean options
        BackupParams params = new BackupParams();
        
        params.setIncludeBinaries(incBinariesField.getValueAsBoolean());
        params.setCompress(compressField.getValueAsBoolean());
        params.setDocumentsPerFile(Long.valueOf(docsPerFileField.getValueAsString()));
        
        control.backupAndDownload(params);
    }
 
    
}
