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
package org.modeshape.web.client.contents;

import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import javax.jcr.ImportUUIDBehavior;
import org.modeshape.web.shared.ModalDialog;

/**
 * Dialog asking backup directory.
 * 
 * @author kulikov
 */
public class ImportDialog extends ModalDialog {
    
    private TextItem name = new TextItem("Import from");
    private Contents contents;
    private RadioGroupItem radioGroupItem = new RadioGroupItem();
    
    public ImportDialog(Contents contents) {
        super("Import", 400, 300);
        this.contents = contents;
        
        StaticTextItem description = new StaticTextItem("");
        description.setValue("Specify name");
        
        
        radioGroupItem.setTitle("Options");
        radioGroupItem.setValueMap("Create new", "Remove existing", 
                "Replace existing", "Collision throw");
        radioGroupItem.setWidth(150);
        setControls(description, name, radioGroupItem);
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        String selection = radioGroupItem.getValueAsString();
        int option = 0;
        if (selection.equals("Create new")) {
            option = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        } else if (selection.equals("Remove existing")) {
            option = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING;
        } else if (selection.equals("Replace existing")) {
            option = ImportUUIDBehavior.IMPORT_UUID_COLLISION_REPLACE_EXISTING;
        } else if (selection.equals("Collision throw")) {
            option = ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW;
        }
        
        contents.importXML(name.getValueAsString(), option);
    }
    
}
