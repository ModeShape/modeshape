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
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

/**
 * Tool bar for the repository management.
 * 
 * @author kulikov
 */
public class AclToolbar extends AbstractToolbar {
    protected AddPrincipalDialog addPrincipalDialog;
    
    public AclToolbar(Console console) {
        super(console);
        addPrincipalDialog = new AddPrincipalDialog("Add principal", console);

        button("", "icons/group_blue_add.png", "Add principal", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                addPrincipalDialog.showModal();
            }            
        });

        button("", "icons/group_blue_remove.png", "Remove principal", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                final String principal = console().nodePanel.principal();
                SC.ask("Remove principal " + principal + "?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean value) {
                        if (value) {
                            //pick up selected node and entered principal name
                            JcrTreeNode node = console().navigator.getSelectedNode();
        
                            //remove entry with all permissions and display it
                            node.getAccessList().remove(principal);
                            console().nodePanel.display(node.getAccessList());
                        }
                    }                    
                });
            }            
        });
        
        
        button("", "icons/apply.png", "Apply changes", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SC.ask("Apply ACL changes?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean value) {
                        if (value) {
                            //pick up selected node and entered principal name
                            JcrTreeNode node = console().navigator.getSelectedNode();
                            console().jcrService.updateAccessList(node.getPath(), node.getAccessList(), new AsyncCallback<Object>() {

                                @Override
                                public void onFailure(Throwable caught) {
                                    SC.say(caught.getMessage());
                                }

                                @Override
                                public void onSuccess(Object result) {
                                }
                            });
                        }
                    }                    
                });
            }            
        });

    }

}
