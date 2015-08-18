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

import com.smartgwt.client.util.SC;
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
public abstract class TabGrid<R extends HLayout, V> extends VLayout {
    private R[] records;
    private final VLayout viewPort = new VLayout();

    public TabGrid(String caption) {
        super();
        init(caption);
    }
    
    private void init(String caption) {
        this.records = records();
        this.setStyleName("grid-bg");
//        this.setLayoutMargin(1);
        

        HLayout toolBar = this.toolBar();
        if (toolBar != null) {
            addMember(toolBar);
        }
        
        HLayout header = this.tableHeader();
        if (header != null) {
            addMember(header);
        }

        setAutoHeight();

        HLayout bottomPanel = new HLayout();
        bottomPanel.setHeight(30);
        bottomPanel.setBackgroundColor("#e6f1f6");

        viewPort.setAutoHeight();
        addMember(viewPort);
//        addMember(bottomPanel);
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
        
        int i = 0;
        for (V value : values) {
            try {
                viewPort.addMember(records[i]);
            updateRecord(i, records[i], value);
            records[i].show();
            i++;
            } catch (Exception e) {
                SC.say(e.getMessage() + ":::;" + Integer.toString(i));
            }
        }
    }
    
    protected abstract void updateRecord(int pos, R record, V value);
    
    protected class Strut extends HLayout {
        public Strut(int size) {
            super();
            setWidth(size);                    
        }
    }
    
}
