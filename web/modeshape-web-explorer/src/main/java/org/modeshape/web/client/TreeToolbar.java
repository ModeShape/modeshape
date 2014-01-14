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
 *
 * @author kulikov
 */
public class TreeToolbar extends AbstractToolbar {
    public TreeToolbar(Console console) {
        super(console);
        this.setBackgroundColor("#d3d3d3");
        
        //add node button       
        button("", "icons/folder_modernist_add.png", "Add new node", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console().newNodeDialog.showModal();
            }            
        });
        
        //remove node button
        button("", "icons/folder_modernist_remove.png", "Delete node", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SC.ask("Remove node", "Do you want to remove node?", new BooleanCallback() {
                    @Override
                    public void execute(Boolean confirmed) {
                        if (!confirmed) {
                            return;
                        }
                        String path = console().navigator.getSelectedPath();
                        console().jcrService.removeNode(path, new AsyncCallback<Object>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                SC.say(caught.getMessage());
                            }
                            
                            @Override
                            public void onSuccess(Object result) {
                                console().navigator.selectNode();
                            }                            
                        });
                    }                    
                });
            }            
        });
        
    }
}
