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
