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
package org.modeshape.web.client.repo;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.client.Console;
import org.modeshape.web.client.JcrServiceAsync;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public class RepositoriesList extends VLayout {
    private RepositoryItem[] items = new RepositoryItem[100];
    private JcrServiceAsync jcrService;
    private Console console;
    private String selected;
    
    public RepositoriesList(Console console, JcrServiceAsync jcrService) {
        this.console = console;
        this.jcrService = jcrService;
                
        for (int i = 0; i < items.length; i++) {
            items[i] = new RepositoryItem();
            addMember(items[i]);
        }
    }
    
    public String getSelected() {
        return selected;
    }
    
    public void show(Collection<RepositoryName> repos) {
        for (int i = 0; i < items.length; i++) {
            items[i].setVisible(false);
        }
        
        int i = 0;
        for (RepositoryName repo : repos) {
            items[i++].show(repo.getName(), repo.getDescriptor());
        }
    }
    
    public void load() {
        jcrService.getRepositories(new AsyncCallback<Collection<RepositoryName>>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess(Collection<RepositoryName> result) {
                try {
                    console.hideRepository();
                    show(result);
                    console.display(RepositoriesList.this);
                } catch (Exception e) {
                    SC.say(e.getMessage());
                }
            }
        });
    }
    
    public void select(String repository, String workspace, String path, boolean changeHistory) {
        selected = repository;
        console.displayContent(repository, workspace, path, changeHistory);
    }
    
    private class RepositoryItem extends VLayout {
        private Label name = new Label();
        private Canvas descriptor = new Label();
        
        public RepositoryItem() {
            super();
            setVisible(false);
            setStyleName("repository");
            
            name.setHeight(30);
            name.setStyleName("repository-name");
            name.setIcon("icons/logo-1.png");
            name.addClickHandler(new ClickHandler() {
                @SuppressWarnings( "synthetic-access" )
                @Override
                public void onClick(ClickEvent event) {
                    Label repo = (Label)event.getSource(); 
                    selected = repo.getContents();
                    console.displayContent(selected, true);
                }
            });
            addMember(name);
            addMember(descriptor);
        }
        
        public void show(String name, String descriptor) {
            this.name.setContents(name);
            this.descriptor.setContents(descriptor);
            this.show();
        }
    }
}
