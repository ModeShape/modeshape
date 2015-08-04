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

import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 *
 * @author kulikov
 */
public class PathControl extends HLayout {
    private final static String PATH_SEPARATOR = "/";
    
    private final Label[] segments = new Label[50];
    private final Label[] separators = new Label[50];

    private final Label addButton = new Label();
    private final Label importButton = new Label();
    private final Label exportButton = new Label();
    
    public PathControl(final Contents contents) {
        super();
        setHeight(30);

        Label path = new Label();
        path.setAutoWidth();
        path.setContents("<b>Node:</b>");
        path.setStyleName("text");

        addMember(path);


        for (int i = 0; i < segments.length; i++) {
            segments[i] = new Label();
            segments[i].setAutoWidth();
            segments[i].setStyleName("segment");
            segments[i].addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Label label = (Label) event.getSource();
                    contents.getAndDisplayNode(label.getDataPath(), true);
                }
            });

            separators[i] = new Label();
            separators[i].setContents(PATH_SEPARATOR);
            separators[i].setVisible(false);
            separators[i].setAutoWidth();
            separators[i].setStyleName("segment-separator");
            separators[i].setMargin(3);

            addMember(segments[i]);
            addMember(separators[i]);
            
        }
        
        
        addButton.setIcon("icons/add.png");
        addButton.setWidth(16);
        addButton.setTooltip("Add new node");
        addButton.setStyleName("button-label");
        addButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.showAddNodeDialog();
            }
        });
        
        importButton.setIcon("icons/folder_add.png");
        importButton.setWidth(16);
        importButton.setTooltip("Import nodes under this node");
        importButton.setStyleName("button-label");
        importButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.showImportDialog();
            }
        });
        
        exportButton.setIcon("icons/folder_go.png");
        exportButton.setWidth(16);
        exportButton.setTooltip("Export this node");
        exportButton.setStyleName("button-label");
        exportButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                contents.showExportDialog();
            }
        });
        
        addMember(addButton);
        addMember(importButton);
        addMember(exportButton);
    }

    
    public void display(String url) {
        for (int i = 0; i < segments.length; i++) {
            segments[i].setContents("");
            separators[i].setVisible(false);
        }

        String[] tokens = url.split(PATH_SEPARATOR);
        if (tokens.length == 0) {
            tokens = new String[]{PATH_SEPARATOR};
        }

        for (int i = 0; i < tokens.length; i++) {

            segments[i].setContents(tokens[i]);

            String path = "";
            for (int j = 0; j <= i; j++) {
                path += (PATH_SEPARATOR + segments[j].getContents());
            }

            segments[i].setTooltip(path);
            segments[i].setDataPath(path);
            segments[i].draw();

            if (i < tokens.length - 1) {
                separators[i].setVisible(true);
            }
        }
    }
}
