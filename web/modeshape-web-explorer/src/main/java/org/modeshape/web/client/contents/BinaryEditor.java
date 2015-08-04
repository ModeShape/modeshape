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
package org.modeshape.web.client.contents;

import com.google.gwt.core.client.GWT;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.ContentsType;
import com.smartgwt.client.types.Encoding;
import com.smartgwt.client.types.FormMethod;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.HiddenItem;
import com.smartgwt.client.widgets.form.fields.UploadItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import org.modeshape.web.shared.JcrNode;

/**
 *
 * @author kulikov
 */
public class BinaryEditor extends HLayout {
    private HTMLPane htmlPane = new HTMLPane();
    private DynamicForm form = new DynamicForm();
    private HiddenItem repositoryField = new HiddenItem("repository");
    private HiddenItem workspaceField = new HiddenItem("workspace");
    private HiddenItem pathField = new HiddenItem("path");
    private HiddenItem pNameField = new HiddenItem("pname");
    private UploadItem fileItem = new UploadItem("Upload content");
    
    public BinaryEditor() {        
        this.setStyleName("grid-bg");
        this.setLayoutMargin(1);
        
        VLayout background = new VLayout();
        background.setWidth100();
        background.setHeight100();
        background.setStyleName("grid-panel");
        addMember(background);
        

        HLayout topPanel = new HLayout();
        topPanel.setHeight(30);
        topPanel.setAlign(VerticalAlignment.CENTER);
        topPanel.setAlign(Alignment.LEFT);
        
        topPanel.setLayoutMargin(3);
        topPanel.setBackgroundColor("#e6f1f6");
        
        Label label = new Label("Binary content");
        label.setWidth100();
        label.setStyleName("caption");
        topPanel.addMember(label);

        background.addMember(topPanel);
        

//        setAutoHeight();

        HLayout bottomPanel = new HLayout();
        bottomPanel.setHeight(30);
        bottomPanel.setBackgroundColor("#e6f1f6");

        
        background.addMember(bottomPanel);        
        form.setMethod(FormMethod.POST);
        form.setAction(GWT.getModuleBaseURL() + "binary-upload/content");
        form.setEncoding(Encoding.MULTIPART);
        
        VLayout vstack = new VLayout();
        vstack.setTop(20);
                        
        vstack.setWidth100();
        vstack.setHeight(550);
        
        htmlPane.setWidth100();
        htmlPane.setHeight(500);
        htmlPane.setContentsType(ContentsType.PAGE);
        htmlPane.setBorder("inset #d3d3d3 1px");
        htmlPane.setShowCustomScrollbars(false);
        vstack.addMember(htmlPane);
        
        HLayout panel = new HLayout();
        vstack.addMember(panel);
        
        Button submitButton = new Button("Submit");
        submitButton.addClickHandler(new ClickHandler() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onClick(ClickEvent event) {
                form.submitForm();
            }
        });
        submitButton.setValign(VerticalAlignment.CENTER);
        fileItem.setStartRow(true);
        fileItem.setEndRow(false);
        
        fileItem.setWidth("100%");
        submitButton.setTitle("Upload");
        
        form.setWidth100();
        
        panel.setLayoutAlign(Alignment.LEFT);
        panel.setLayoutAlign(VerticalAlignment.CENTER);
        panel.setBackgroundColor(null);
        panel.addMember(form);
        panel.addMember(submitButton);
        
        vstack.setAutoHeight();
        background.addMember(vstack);
        
        setVisible(false);
    }
    
    public void setValue(JcrNode node, String name, String reference) {
        repositoryField.setValue(node.getRepository());
        workspaceField.setValue(node.getWorkspace());
        pathField.setValue(node.getPath());
        pNameField.setValue(name);
        form.setItems(fileItem, repositoryField, workspaceField, pathField, pNameField);
        htmlPane.setContentsURL(reference);
        htmlPane.redraw();
    }
    
}
