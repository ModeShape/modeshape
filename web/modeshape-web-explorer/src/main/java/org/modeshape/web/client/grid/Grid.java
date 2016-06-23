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
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;

/**
 * Pattern for grid view.
 * 
 * @author kulikov
 * @param <R>
 * @param <V>
 */
public abstract class Grid<R extends HLayout, V> extends VLayout {
    private R[] records;
    private final VLayout viewPort = new VLayout();
    private final HLayout bottomPanel = new HLayout();
    
    public Grid(String caption) {
        super();
        init(caption);
    }
    
    private void init(String caption) {
        this.records = records();
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
        
        Label label = new Label(caption);
        label.setWidth100();
        label.setStyleName("caption");
        topPanel.addMember(label);

        background.addMember(topPanel);
        
        HLayout toolBar = this.toolBar();
        if (toolBar != null) {
            background.addMember(toolBar);
        }
        
        HLayout header = this.tableHeader();
        if (header != null) {
            background.addMember(header);
        }

        setAutoHeight();

        bottomPanel.setHeight(30);
        bottomPanel.setBackgroundColor("#e6f1f6");
        bottomPanel.setLayoutAlign(VerticalAlignment.CENTER);
        bottomPanel.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        
        viewPort.setAutoHeight();
        
        background.addMember(viewPort);
        background.addMember(bottomPanel);
    }
    
    protected abstract R[] records();
    protected abstract HLayout tableHeader();
    protected abstract HLayout toolBar();
    
    protected void setValues(Collection<V> values) {
        try {
            for (int i = 0; i < records.length; i++) {
                records[i].setVisible(false);
                viewPort.removeMember(records[i]);
            }
        } catch (Exception e) {
        }

        if (values.isEmpty()) {
            viewPort.addMember(records[0]);
            updateRecord(-1, records[0], null);
            records[0].show();
            return;
        }

        viewPort.addMember(records[0]);
        records[0].show();
        updateRecord(0, records[0], null);
        
        int i = 1;
        for (V value : values) {
            viewPort.addMember(records[i]);
            records[i].show();
            updateRecord(i, records[i], value);
            i++;
        }
    }
    
    protected abstract void updateRecord(int pos, R record, V value);
    
    public void setFooterContent(HLayout component) {
        component.setWidth(100);
        bottomPanel.addMember(component);
    }
    
    protected class Strut extends HLayout {
        public Strut(int size) {
            super();
            setWidth(size);                    
        }
    }
    
}
