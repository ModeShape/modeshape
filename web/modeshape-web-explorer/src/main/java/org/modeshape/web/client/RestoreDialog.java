/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
