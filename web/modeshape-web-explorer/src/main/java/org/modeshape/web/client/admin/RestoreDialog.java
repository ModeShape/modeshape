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

import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import org.modeshape.web.shared.ModalDialog;

/**
 * Dialog asking backup directory.
 * 
 * @author kulikov
 */
public class RestoreDialog extends ModalDialog {
    
    private final TextItem name = new TextItem("Backup name");
    private final RestoreControl control;
    
    public RestoreDialog(RestoreControl control) {
        super("Restore", 400, 200);
        this.control = control;
        
        StaticTextItem description = new StaticTextItem("");
        description.setValue("Specify backup name");
        
        setControls(description, name);
    }
    
    @Override
    public void onConfirm(ClickEvent event) {
        control.restore(name.getValueAsString());
    }
 
    
}
