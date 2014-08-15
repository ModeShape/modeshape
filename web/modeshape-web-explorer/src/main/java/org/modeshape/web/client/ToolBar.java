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
