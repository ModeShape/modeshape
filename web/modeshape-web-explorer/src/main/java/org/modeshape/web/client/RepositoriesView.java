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

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public class RepositoriesView extends View {
    private RepositoryItem[] items = new RepositoryItem[100];
    private JcrServiceAsync jcrService;
    private Console console;
    private String selected;
    
    public RepositoriesView(Console console, JcrServiceAsync jcrService, ViewPort viewPort) {
        super(viewPort, null);
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
                    console.hideRepo();
                    show(result);
                    viewPort().display(RepositoriesView.this);
                } catch (Exception e) {
                    SC.say(e.getMessage());
                }
            }
        });
    }
    
    public void select(String repository, String workspace, String path, boolean changeHistory) {
        selected = repository;
        console.showContent(repository, workspace, path, changeHistory);
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
                    console.showContent(selected, true);
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
