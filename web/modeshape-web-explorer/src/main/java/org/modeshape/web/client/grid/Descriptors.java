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
import com.smartgwt.client.widgets.layout.HLayout;
import org.modeshape.web.client.Console;
import org.modeshape.web.client.grid.Descriptors.DescriptorRecord;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.Param;

/**
 *
 * @author kulikov
 */
public class Descriptors extends Grid<DescriptorRecord, Param> {

    public Descriptors(Console console) {
        super("Repository descriptor");
    }

    public void show(JcrRepositoryDescriptor desc) {
        setValues(desc.info());
    }

    @Override
    protected DescriptorRecord[] records() {
        DescriptorRecord[] records = new DescriptorRecord[100];
        for (int i = 0; i < records.length; i++) {
            records[i] = new DescriptorRecord();
        }
        return records;
    }

    @Override
    protected HLayout tableHeader() {
        HLayout layout = new HLayout();
        layout.setHeight(30);
        layout.setBackgroundColor("#e6f1f6");

        Label name = new Label("<b>Name</b>");
        name.setWidth(450);

        Label value = new Label("<b>Value</b>");
        value.setWidth100();

        layout.addMember(name);
        layout.addMember(value);
        return layout;
    }

    @Override
    protected HLayout toolBar() {
        return null;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    protected void updateRecord(int pos, DescriptorRecord record, Param value) {
        record.name.setContents(value.getName());
        record.value.setContents(value.getValue());
    }

    public class DescriptorRecord extends HLayout {

        private Label name = new Label();
        private Label value = new Label();

        public DescriptorRecord() {
            super();
            setStyleName("grid");
            setHeight(30);

            setDefaultLayoutAlign(VerticalAlignment.CENTER);
            setDefaultLayoutAlign(Alignment.LEFT);

            setLayoutAlign(VerticalAlignment.CENTER);
            setLayoutAlign(Alignment.CENTER);

            setAlign(VerticalAlignment.CENTER);
            setAlign(Alignment.LEFT);

            name.setIcon("icons/sprocket.png");
            name.setStyleName("text");
            name.setWidth(450);

            value.setWidth100();
            value.setStyleName("text");

            addMember(name);
            addMember(value);
        }

    }
}
