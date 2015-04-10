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

import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author kulikov
 */
public class UploadRestoreControl extends VLayout {

    private final UploadBackupDialog uploadBackupDialog = new UploadBackupDialog(this);
    private final AdminView adminView;
    
    public UploadRestoreControl(final AdminView adminView) {
        super();
        this.adminView = adminView;
        
        setStyleName("admin-control");

        Label label = new Label("Upload & Restore");
        label.setIcon("icons/documents.png");
        label.setStyleName("button-label");
        label.setHeight(25);
        label.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                uploadBackupDialog.showModal(adminView.repository());
            }
        });

        Canvas text = new Canvas();
        text.setAutoHeight();
        text.setContents("Once you have a complete backup on disk, you can "
                + "then restore a repository back to the state captured "
                + "within the backup. To do that, simply start a repository "
                + "(or perhaps a new instance of a repository with a "
                + "different configuration) and, before it’s used by "
                + "any applications, load into the new repository all of " + "the content in the backup. ");

        addMember(label);
        addMember(text);
    }
    
    protected void showLoadIcon() {
        adminView.console().showLoadingIcon();
    }
}
