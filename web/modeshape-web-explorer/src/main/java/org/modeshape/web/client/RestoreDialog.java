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

import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;

/**
 * Dialog asking backup location.
 *
 * @author kulikov
 */
public class RestoreDialog extends ModalDialog {

    private TextItem name = new TextItem("Backup name");
    private AdminView admin;

    public RestoreDialog(AdminView admin) {
        super("Restore repository", 400, 200);
        this.admin = admin;

        StaticTextItem description = new StaticTextItem();
        description.setValue("Specify backup name");

        setControls(description, name);
    }

    @Override
    public void onConfirm(ClickEvent event) {
        admin.restore(name.getValueAsString());
    }

    public void uploadComplete(String fileName) {
        SC.say("Upload complete");
    }

    private native void initComplete(RestoreDialog dialog) /*-{
     $wnd.uploadComplete = function (fileName) {
     dialog.@org.modeshape.web.client.RestoreDialog::uploadComplete(Ljava/lang/String;)(fileName);
     };
     }-*/;
}
