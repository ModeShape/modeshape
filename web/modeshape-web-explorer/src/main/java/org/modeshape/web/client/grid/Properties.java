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
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;
import org.modeshape.web.client.Contents;
import org.modeshape.web.client.grid.Properties.PropertyRecord;
import org.modeshape.web.shared.JcrNode;
import org.modeshape.web.shared.JcrProperty;

/**
 *
 * @author kulikov
 */
@SuppressWarnings( "synthetic-access" )
public class Properties extends TabGrid<PropertyRecord, JcrProperty> {

    private final Contents contents;

    public Properties(Contents contents) {
        super("Properties");
        this.contents = contents;
    }

    public void show(JcrNode node) {
        setValues(node.getProperties());
    }

    @Override
    protected PropertyRecord[] records() {
        PropertyRecord[] records = new PropertyRecord[100];
        for (int i = 0; i < records.length; i++) {
            records[i] = new PropertyRecord();
        }
        return records;
    }

    @Override
    protected HLayout tableHeader() {
        HLayout header = new HLayout();
        header.setHeight(30);
        header.setBackgroundColor("#e6f1f6");

        Label name = new Label("<b>Name</b>");
        name.setWidth(150);

        Label type = new Label("<b>Type</b>");
        type.setWidth(150);

        Label visibility = new Label("<b>Protected</b>");
        visibility.setWidth(100);

        Label mvalue = new Label("<b>Multivalue</b>");
        mvalue.setWidth(100);

        Label value = new Label("<b>Value</b>");
        value.setWidth100();

        header.addMember(name);
        header.addMember(type);
        header.addMember(visibility);
        header.addMember(mvalue);
        header.addMember(value);
        return header;
    }

    @Override
    protected HLayout toolBar() {
        HLayout header = new HLayout();
        header.setBackgroundColor("#ffffff");
//        header.setMargin(5);
        header.setAlign(Alignment.LEFT);
        header.setDefaultLayoutAlign(Alignment.LEFT);
        header.setLayoutAlign(Alignment.LEFT);
        header.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        header.setLayoutAlign(VerticalAlignment.CENTER);
        header.setHeight(30);

        HLayout panel = new HLayout();

        panel.setAlign(Alignment.RIGHT);
        panel.setWidth100();
        panel.setDefaultLayoutAlign(Alignment.RIGHT);
        panel.setLayoutAlign(Alignment.RIGHT);
        panel.setDefaultLayoutAlign(VerticalAlignment.CENTER);
        panel.setLayoutAlign(VerticalAlignment.CENTER);

//            Label hint = new Label();
//            hint.setWidth(200);
//            hint.setAlign(Alignment.RIGHT);
//            hint.setLayoutAlign(VerticalAlignment.BOTTOM);

//            hint.setContents("Click button to switch view");

        Button addMixinButton = new Button();
        addMixinButton.setTitle("Add mixin");
        addMixinButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.addMixin();
            }
        });

        Button removeMixinButton = new Button();
        removeMixinButton.setTitle("Remove mixin");
        removeMixinButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.removeMixin();
            }
        });

        Button editPropertyButton = new Button();
        editPropertyButton.setTitle("Edit property");
        editPropertyButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                contents.setProperty();
            }
        });


        header.addMember(panel);

//            panel.addMember(hint);
        panel.addMember(new Strut(10));
        panel.addMember(addMixinButton);
        panel.addMember(new Strut(5));
        panel.addMember(removeMixinButton);
        panel.addMember(new Strut(5));
        panel.addMember(editPropertyButton);

        return header;
    }

    @Override
    protected void updateRecord(int pos, PropertyRecord record, JcrProperty value) {
        record.setValue(value);
    }

    protected class PropertyRecord extends HLayout {

        private Label name = new Label();
        private Label type = new Label();
        private BooleanField isProtected = new BooleanField();
        private BooleanField isMultiple = new BooleanField();
        private Label value = new Label();

        public PropertyRecord() {
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
            name.setWidth(150);

            type.setWidth(150);
            type.setStyleName("text");

            isProtected.setWidth(100);
            isMultiple.setWidth(100);
            value.setWidth100();
            value.setStyleName("text");

            addMember(name);
            addMember(type);
            addMember(isProtected);
            addMember(isMultiple);
            addMember(value);
        }

        private void setValue(JcrProperty property) {
            this.name.setContents(property.getName());
            this.type.setContents(property.getType());
            this.isProtected.setValue(property.isProtected());
            this.isMultiple.setValue(property.isMultiValue());
            this.value.setContents(property.getValue());
        }
    }
}
