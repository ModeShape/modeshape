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
package org.modeshape.web.client.grid;

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Pattern for grid view.
 * 
 * @author kulikov
 */
public class TabsetGrid extends VLayout {
    private final TabGrid<?,?>[] tabs;
    private Label[] labels;
    public TabsetGrid(String[] caption, TabGrid<?,?>[] tbs) {
        super();
        this.tabs = tbs;
        
        init(caption);
    }
    
    private void init(String... caption) {
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
        
        labels = new Label[tabs.length];
        for (int i = 0; i < tabs.length; i++) {
            Label label = new Label(caption[i]);
//            label.setWidth100();
            label.setDataPath(Integer.toString(i));
            label.setStyleName("caption");
            label.setWidth(100);
            
            label.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Label l = (Label) event.getSource();
                    int i = Integer.parseInt(l.getDataPath());
                    showTab(i);
                }
            });
            labels[i] = label;
            topPanel.addMember(label);
        }
        
        background.addMember(topPanel);
        for (int i = 0; i < tabs.length; i++) {
            background.addMember(tabs[i]);
        }

        setAutoHeight();

        HLayout bottomPanel = new HLayout();
        bottomPanel.setHeight(30);
        bottomPanel.setBackgroundColor("#e6f1f6");

//        viewPort.setAutoHeight();
//        background.addMember(viewPort);
        background.addMember(bottomPanel);
        
        showTab(0);
    }
    
    public void showTab(int k) {
        for (int i = 0; i < tabs.length; i++) {
            tabs[i].setVisible(i == k);
        }
        for (int i = 0; i < labels.length; i++) {
            if (i == k) {
                labels[i].setStyleName("caption-selected");
            } else {
                labels[i].setStyleName("caption");
            }
        }        
    }
    
    protected class Strut extends HLayout {
        public Strut(int size) {
            super();
            setWidth(size);                    
        }
    }
    
}
