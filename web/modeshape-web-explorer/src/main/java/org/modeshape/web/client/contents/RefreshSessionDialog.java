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

import com.smartgwt.client.widgets.form.fields.CheckboxItem;
import org.modeshape.web.shared.ModalDialog;

/**
 * Dialog for asking mixin.
 * 
 * @author kulikov
 */
public class RefreshSessionDialog extends ModalDialog {

    private final static String TITLE = "Refresh session";
    
    private CheckboxItem keepChanges = new CheckboxItem();
    private Contents contents;
    
    public RefreshSessionDialog(Contents contents) {
        super(TITLE, 450, 150);
        this.contents = contents;
        
        keepChanges.setName("name");
        keepChanges.setTitle("Discard changes");
        keepChanges.setDefaultValue("");
        keepChanges.setWidth(250);
        keepChanges.setRequired(true);
        keepChanges.setVisible(true);
        keepChanges.setStartRow(true);
        keepChanges.setEndRow(true);

        setControls(keepChanges);
        keepChanges.focusInItem();
    }

    @Override
    public void onConfirm(com.smartgwt.client.widgets.form.fields.events.ClickEvent event) {
        contents.refreshSession(!keepChanges.getValueAsBoolean());
    }
}
