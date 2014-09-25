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

import com.smartgwt.client.widgets.form.fields.RadioGroupItem;
import com.smartgwt.client.widgets.form.fields.StaticTextItem;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.form.fields.events.ClickEvent;
import javax.jcr.ImportUUIDBehavior;

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
