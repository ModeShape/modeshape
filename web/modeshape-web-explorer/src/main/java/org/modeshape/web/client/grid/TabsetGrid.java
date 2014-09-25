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
    
    protected void showTab(int k) {
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
