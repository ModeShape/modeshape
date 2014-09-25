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
import java.util.Collection;
import org.modeshape.web.client.grid.NodeTypes.TypeRecord;
import org.modeshape.web.shared.JcrNodeType;

/**
 *
 * @author kulikov
 */
public class NodeTypes extends Grid<TypeRecord, JcrNodeType> {
    
    private final static int RECORDS_PER_PAGE = 500;
    
    public NodeTypes() {
        super("Node types");
    }

    public void show(Collection<JcrNodeType> types) {
        setValues(types);
    }

    @Override
    protected TypeRecord[] records() {
        TypeRecord[] records = new TypeRecord[RECORDS_PER_PAGE];
        for (int i = 0; i < records.length; i++) {
            records[i] = new TypeRecord();
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

        Label type = new Label("<b>Primary</b>");
        type.setWidth(100);

        Label visibility = new Label("<b>Mixin</b>");
        visibility.setWidth(100);

        Label mvalue = new Label("<b>Abstract</b>");
        mvalue.setWidth(100);


        layout.addMember(name);
        layout.addMember(type);
        layout.addMember(visibility);
        layout.addMember(mvalue);

        return layout;
    }

    @Override
    protected HLayout toolBar() {
        return null;
    }

    @SuppressWarnings( "synthetic-access" )
    @Override
    protected void updateRecord(int pos, TypeRecord record, JcrNodeType value) {
        record.setValue(value);
    }

    public class TypeRecord extends HLayout {

        private Label name = new Label();
        private BooleanField isPrimary = new BooleanField();
        private BooleanField isMixin = new BooleanField();
        private BooleanField isAbstract = new BooleanField();

        public TypeRecord() {
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

            isPrimary.setWidth(100);
            isMixin.setWidth(100);
            isAbstract.setWidth(100);

            addMember(name);
            addMember(isPrimary);
            addMember(isMixin);
            addMember(isAbstract);
        }

        private void setValue(JcrNodeType nodeType) {
            this.name.setContents(nodeType.getName());
            this.isPrimary.setValue(nodeType.isPrimary());
            this.isAbstract.setValue(nodeType.isAbstract());
            this.isMixin.setValue(nodeType.isMixin());
        }
    }
}
