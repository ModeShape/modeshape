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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;

/**
 * Dialog for asking mixin.
 * 
 * @author kulikov
 */
public class AddMixinDialog extends ModalDialog {

    protected ComboBoxItem name = new ComboBoxItem();
    protected Console console;

    public AddMixinDialog( String title,
                           Console console ) {
        super(title, 450, 150);
        this.console = console;

        name.setName("name");
        name.setTitle("Mixin");
        name.setDefaultValue("");
        name.setWidth(250);
        name.setRequired(true);
        name.setVisible(true);
        name.setStartRow(true);
        name.setEndRow(true);

        StaticTextItem description = new StaticTextItem();
        description.setValue("Select mixin type");
        description.setTitle("");
        description.setStartRow(true);
        description.setEndRow(true);

        setControls(description, name);
        name.focusInItem();
    }

    @Override
    public void showModal() {
        console.jcrService.getMixinTypes(false, new AsyncCallback<String[]>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess( String[] result ) {
                name.setValueMap(result);
                AddMixinDialog.super.showModal();
            }
        });
    }

    @Override
    public void onConfirm( com.smartgwt.client.widgets.form.fields.events.ClickEvent event ) {
        String path = AddMixinDialog.this.console.navigator.getSelectedPath();
        AddMixinDialog.this.console.jcrService.addMixin(path, name.getValueAsString(), new AsyncCallback<Object>() {
            @Override
            public void onFailure( Throwable caught ) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess( Object result ) {
                AddMixinDialog.this.console.navigator.selectNode();
            }
        });
    }
}
