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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author kulikov
 */
public class RepositoryNameView extends VLayout {

    private Label label = new Label();
    private String repositoryName;
    
    public RepositoryNameView(final Console console) {
        super();
        setStyleName("repository");
        setLayoutAlign(Alignment.CENTER);
        setLayoutAlign(VerticalAlignment.CENTER);
        setDefaultLayoutAlign(Alignment.CENTER);
        setDefaultLayoutAlign(VerticalAlignment.CENTER);

        setHeight(55);
        setWidth100();

        Img logo = new Img();
        logo.setSrc("icons/attach.png");

        logo.setHeight(55);
        logo.setWidth(45);
        logo.setValign(VerticalAlignment.CENTER);

        HLayout panel0 = new HLayout();

        HLayout panel1 = new HLayout();

        panel1.setLayoutAlign(Alignment.RIGHT);
        panel1.setDefaultLayoutAlign(Alignment.RIGHT);
        panel1.setAlign(Alignment.RIGHT);
        HLayout panel = new HLayout();

//            panel.setAlign(Alignment.LEFT);
        panel.setLayoutAlign(VerticalAlignment.CENTER);
        panel.setDefaultLayoutAlign(VerticalAlignment.CENTER);

        label.setStyleName("repository-caption");
        label.setWidth("100%");
        label.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.showRepositories();
            }
        });
        label.setBorder("1 px solid black");
        label.setValign(VerticalAlignment.CENTER);

        Label explore = new Label("<b>Explore</b>");
        explore.setAlign(Alignment.RIGHT);
        explore.setBorder("1ps solid green");
        explore.setAutoFit(true);
        explore.setStyleName("tab-label");
        explore.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.showContent();
            }
        });

        Label nodeTypes = new Label("<b>NodeTypes</b>");
        nodeTypes.setAlign(Alignment.RIGHT);
        nodeTypes.setAutoFit(true);
        nodeTypes.setStyleName("tab-label");
        nodeTypes.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.showNodeTypes();
            }
        });

        Label descriptor = new Label("<b>Descriptor</b>");
        descriptor.setAlign(Alignment.RIGHT);
        descriptor.setAutoFit(true);
        descriptor.setStyleName("tab-label");
        descriptor.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.showRepositoryInfo();
            }
        });

        Label query = new Label("<b>Query</b>");
        query.setAlign(Alignment.RIGHT);
        query.setAutoFit(true);
        query.setStyleName("tab-label");
        query.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.showQuery();
            }
        });

        Label admin = new Label("<b>Admin</b>");
        admin.setAlign(Alignment.RIGHT);
        admin.setAutoFit(true);
        admin.setStyleName("tab-label");
        admin.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.showAdmin();
            }
        });
        
        panel.addMember(logo);
        panel.addMember(label);

        panel1.addMember(explore);
        panel1.addMember(new Spacer(10));
        panel1.addMember(nodeTypes);
        panel1.addMember(new Spacer(10));
        panel1.addMember(descriptor);
        panel1.addMember(new Spacer(10));
        panel1.addMember(query);
        panel1.addMember(new Spacer(10));
        panel1.addMember(admin);
        panel1.setHeight(55);
        
        panel0.setWidth("70%");
        panel0.setHeight(55);

        panel0.addMember(panel);
        panel0.addMember(panel1);

        addMember(new Strut(10));
        addMember(panel0);
        addMember(new Strut(10));
        setVisible(false);
    }

    public String repository() {
        return this.repositoryName;
    }
    
    public void show(String name) {
        this.repositoryName = name;
        label.setContents("Repository: " + name);
        show();
    }

    private class Spacer extends HLayout {

        protected Spacer(int size) {
            super();
            setWidth(size);
        }
    }
    
    private class Strut extends VLayout {
        public Strut(int size) {
            super();
            setHeight(size);
        }
    }
    
}
