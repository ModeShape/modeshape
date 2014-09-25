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
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import com.smartgwt.client.widgets.toolbar.ToolStrip;
import com.smartgwt.client.widgets.toolbar.ToolStripButton;
import java.util.Collection;
import org.modeshape.web.shared.RepositoryName;

/**
 *
 * @author kulikov
 */
public class ToolBar extends HLayout {
    
    private Label userName = new Label();

    public ToolBar(final Console console) {
        super();        
        
        setHeight(50);
        setWidth100();
        
        Img logo = new Img();
        logo.setSrc("icons/logo.png");
        
        logo.setHeight(30);
        logo.setWidth(190);
        logo.setValign(VerticalAlignment.CENTER);
        
        
        ToolStrip strip = new ToolStrip();
        strip.setHeight(50);
        strip.setWidth100();
        
        strip.addSpacer(120);
        strip.addMember(logo);
        strip.addSpacer(10);
        strip.addSeparator();
        
//        Img homeImg = new Img();
//        homeImg.setSrc("icons/bullet_blue.png");
//        homeImg.setWidth(5);
//        homeImg.setHeight(5);
                
//        strip.addMember(homeImg);
//        strip.addSeparator();
        
        strip.addFill();
        
        
        VLayout p = new VLayout();
        p.setAlign(VerticalAlignment.CENTER);
        DynamicForm form = new DynamicForm();
        
//        form.setNumCols(1);
        p.addMember(form);
        p.setWidth(300);
        
        final TextItem search = new TextItem();
        search.setTitle("Search");
        search.setWidth(300);
        search.setValue("");
        search.setTop(30);
        
        form.setItems(search);
        
        strip.setAlign(VerticalAlignment.CENTER);
        strip.addMember(p);
       
        ToolStripButton go = new ToolStripButton();
        go.setTitle("Go");
        go.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.jcrService.findRepositories(search.getValueAsString(), new AsyncCallback<Collection<RepositoryName>>() {

                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Collection<RepositoryName> result) {
                        console.showRepositories(result);
                    }
                });
            }
        });
        
        strip.addButton(go);
        
        userName.setContents("okulikov");
        userName.setIcon("icons/bullet_blue.png");
        
        strip.addSpacer(140);
        strip.addSeparator();
        
        strip.addMember(userName);
        strip.addSeparator();

        ToolStripButton loging = new ToolStripButton();
        loging.setTitle("Log in");
        loging.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new LoginDialog(console);
            }
        });
        
        ToolStripButton logout = new ToolStripButton();
        logout.setTitle("Log out");
        logout.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.jcrService.login(null, null, new AsyncCallback<Object>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Object result) {
                        console.updateUserName(null);
                    }
                });
            }
        });
        
        strip.addButton(loging);
        strip.addButton(logout);
        
        addMember(strip);
        setBackgroundColor("#d3d3d3");
        
    }
    
    public void setUserName(String userName) {
        if (userName != null && userName.length() > 0) {
            this.userName.setContents(userName);
        } else {
            this.userName.setContents("anonymous");
        }
    }
    
}
