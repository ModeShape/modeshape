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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.modeshape.web.client.Console;
import org.modeshape.web.client.admin.AdminView;
import org.modeshape.web.client.nt.NodeTypesModalForm;
import org.modeshape.web.client.query.QueryModalForm;

/**
 *
 * @author kulikov
 */
public class RepositoryHeader extends HLayout {
    
    private final static int HEIGHT = 55;
    private final static int LOGO_WIDTH = 45;
    
    private Label label = new Label();
    private String repositoryName;
    
    //inner bar with
    private HLayout mainLayout = new HLayout();

    private final Console console;
    
    private final NodeTypesModalForm ntForm;
    private final QueryModalForm queryForm;
    private final DescriptorModalForm descriptorForm;
    private final AdminView adminView;
    
    public RepositoryHeader(final Console console) {
        this.console = console;
        
        ntForm = new NodeTypesModalForm(console);
        queryForm = new QueryModalForm(console);
        descriptorForm = new DescriptorModalForm(console);
        adminView = new AdminView(console);
        //set layout alignment for center        
        alignAndResize();

        //add main layout separated bounded with struts
        addMember(new Strut(10));
        addMember(mainLayout);
        addMember(new Strut(10));

        //resize main layout
        mainLayout.setWidth("80%"); //do not modify, otherwise it wont resize it properly (don't know why)
        mainLayout.setHeight(HEIGHT);
        
        //separate main layout on two columns
        HLayout col1 = new HLayout();
        col1.setLayoutAlign(VerticalAlignment.CENTER);
        col1.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        col1.setWidth100();
        
        VLayout col2 = new VLayout();
        col2.setHeight(30);
        col2.setLayoutAlign(Alignment.RIGHT);
        col2.setDefaultLayoutAlign(Alignment.RIGHT);
        col2.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        col2.setLayoutAlign(VerticalAlignment.CENTER);
        col2.setAlign(Alignment.RIGHT);
        col2.setAutoWidth();
        
        mainLayout.addMember(col1);
        mainLayout.addMember(col2);
        
        
        //put repository name label into left column(col1)
        label.setStyleName("repository-caption");
        label.setWidth("100%");
        label.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console.loadRepositoriesList();
            }
        });
        label.setValign(VerticalAlignment.CENTER);


        col1.addMember(new RepositoryIcon(LOGO_WIDTH, HEIGHT));
        col1.addMember(label);

        //fill column 2
        col2.addMember(new Toolbar(console));
    }

    private void alignAndResize() {
        setStyleName("repository");
        setLayoutAlign(Alignment.CENTER);
        setLayoutAlign(VerticalAlignment.CENTER);
        setDefaultLayoutAlign(Alignment.CENTER);
        setDefaultLayoutAlign(VerticalAlignment.CENTER);

        setHeight(HEIGHT);
        setWidth100();
    }

    /**
     * Set width of main layout.
     * 
     * @param value 
     */
    public void setLayoutWidth(String value) {
        mainLayout.setWidth(value);
    }

    /**
     * Gets the name of the repository.
     * 
     * @return the name of repository displayed.
     */
    public String repository() {
        return this.repositoryName;
    }

    /**
     * Displays given repository name.
     * 
     * @param name repository name.
     */
    public void show(String name) {
        this.repositoryName = name;
        label.setContents("Repository: " + name);
        show();
    }

    public void showRepositoryInfo() {
        descriptorForm.showModal();
    } 
    
    public void showNodeTypes() {
        ntForm.showModal();
    }
    
    public void showContent() {
        console.loadNodeSpecifiedByURL();
    }
     
    public void showQuery() {
        queryForm.showModal();
    }

    public void showAdmin() {
        console.display(adminView);
    }
    
    /**
     * Icon image near repository name
     */
    private class RepositoryIcon extends Img {

        private final static String IMG_PATH = "icons/attach.png";

        public RepositoryIcon(int width, int height) {
            setSrc(IMG_PATH);
            setWidth(width);
            setHeight(height);
            setValign(VerticalAlignment.CENTER);
        }
    }

    private class Toolbar extends HLayout {
        
        public Toolbar(final Console console) {
            setHeight(30);
            setLayoutAlign(Alignment.RIGHT);
            setDefaultLayoutAlign(Alignment.RIGHT);
            setAlign(Alignment.RIGHT);
            setStyleName("viewport");
            setAutoWidth();

            ToolButton explore = new ToolButton("Explore");
            explore.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showContent();
                }
            });

            ToolButton nodeTypes = new ToolButton("NodeTypes");
            nodeTypes.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showNodeTypes();
                }
            });

            ToolButton descriptor = new ToolButton("Descriptor");
            descriptor.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showRepositoryInfo();
                }
            });

            ToolButton query = new ToolButton("Query");
            query.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showQuery();
                }
            });

            ToolButton admin = new ToolButton("Admin");
            admin.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    showAdmin();
                }
            });

            addMember(new Spacer(10));
            addMember(explore);
            addMember(new Spacer(10));
            addMember(nodeTypes);
            addMember(new Spacer(10));
            addMember(descriptor);
            addMember(new Spacer(10));
            addMember(query);
            addMember(new Spacer(10));
            addMember(admin);
            addMember(new Spacer(10));
            setHeight(30);
            
        }
    }

    private class ToolButton extends Label {

        public ToolButton(String title) {
            super("<b>" + title + "</b>");
            setAlign(Alignment.RIGHT);
            setAutoWidth();
            setStyleName("tab-label");
        }
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
