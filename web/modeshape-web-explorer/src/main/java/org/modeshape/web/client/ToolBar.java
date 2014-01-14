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
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Tool bar for the repository management.
 * 
 * @author kulikov
 */
@SuppressWarnings( "synthetic-access" )
public class ToolBar extends HLayout {
    private Console console;

    public ToolBar( Console console ) {
        super();
        this.console = console;
        this.setHeight(30);

        // add node button
        button("", "icons/folder_modernist_add.png", "Add new node", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.newNodeDialog.showModal();
            }
        });

        // remove node button
        button("", "icons/folder_modernist_remove.png", "Delete node", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                SC.ask("Remove node", "Do you want to remove node?", new BooleanCallback() {
                    @Override
                    public void execute( Boolean confirmed ) {
                        if (!confirmed) {
                            return;
                        }
                        String path = ToolBar.this.console.navigator.getSelectedPath();
                        ToolBar.this.console.jcrService.removeNode(path, new AsyncCallback<Object>() {
                            @Override
                            public void onFailure( Throwable caught ) {
                                SC.say(caught.getMessage());
                            }

                            @Override
                            public void onSuccess( Object result ) {
                                ToolBar.this.console.navigator.selectNode();
                            }
                        });
                    }
                });
            }
        });

        // Add mixin
        button("", "icons/hcards_add.png", "Add mixin to the node", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addMixinDialog.showModal();
            }
        });

        // Remove mixin
        button("", "icons/hcards_remove.png", "Remove mixin", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.removeMixinDialog.showModal();
            }
        });

        // Remove mixin
        button("", "icons/tag_add.png", "Add property", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addPropertyDialog.showModal();
            }
        });

        spacer();

        // Save button
        button("", "icons/save.png", "Save", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addPropertyDialog.showModal();
            }
        });

        spacer();

        button("", "icons/hcard_add.png", "Add access control list", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addPropertyDialog.showModal();
            }
        });

        button("", "icons/hcard_remove.png", "Remove access control list", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addPropertyDialog.showModal();
            }
        });

        button("", "icons/group_blue_add.png", "Add principal", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addPropertyDialog.showModal();
            }
        });

        button("", "icons/group_blue_remove.png", "Remove principal", new ClickHandler() {
            @Override
            public void onClick( ClickEvent event ) {
                ToolBar.this.console.addPropertyDialog.showModal();
            }
        });

    }

    private void button( String title,
                         String icon,
                         String toolTip,
                         ClickHandler handler ) {
        Button button = new Button();
        button.setWidth(30);
        button.setHeight(30);
        button.setTitle(title);
        button.setIcon(icon);
        button.setTooltip(toolTip);
        button.setMargin(1);
        button.addClickHandler(handler);
        addMember(button);
    }

    private void spacer() {
        HLayout spacer = new HLayout();
        spacer.setWidth(5);
        addMember(spacer);
    }
}
