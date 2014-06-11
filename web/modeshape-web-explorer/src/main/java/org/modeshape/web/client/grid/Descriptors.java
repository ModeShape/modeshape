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
