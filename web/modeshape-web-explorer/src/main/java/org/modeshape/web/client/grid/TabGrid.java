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
        try {
        for (V value : values) {
            viewPort.addMember(records[i]);
            updateRecord(i, records[i], value);
            records[i].show();
            i++;
        }
        } catch (Exception e) {
            SC.say(Integer.toString(i));
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
