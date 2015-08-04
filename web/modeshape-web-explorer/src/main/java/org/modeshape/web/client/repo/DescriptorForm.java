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
package org.modeshape.web.client.repo;

import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.util.Collection;
import org.modeshape.web.client.Console;
import org.modeshape.web.shared.BaseCallback;
import org.modeshape.web.shared.Form;
import org.modeshape.web.shared.JcrRepositoryDescriptor;
import org.modeshape.web.shared.Param;

/**
 *
 * @author kulikov
 */
public class DescriptorForm extends Form {

    private final Console console;
    private final ListGrid grid = new ListGrid();

    public DescriptorForm(Console console) {
        this.console = console;
        this.grid.setWidth100();
        this.grid.setHeight100();

        ListGridField[] fields = new ListGridField[3];

        fields[0] = new ListGridField("icon", " ");
        fields[0].setCanEdit(false);
        fields[0].setImageURLPrefix("icons/bullet_");
        fields[0].setImageURLSuffix(".png");
        fields[0].setWidth(30);
        fields[0].setType(ListGridFieldType.IMAGE);

        fields[1] = new ListGridField("name", "Parameter name");
        fields[1].setCanEdit(false);
        fields[1].setType(ListGridFieldType.TEXT);

        fields[2] = new ListGridField("value", "Value");
        fields[2].setCanEdit(false);
        fields[2].setType(ListGridFieldType.TEXT);

        grid.setFields(fields);
        addMember(grid);
    }

    @Override
    public void init() {
        console.jcrService().repositoryInfo(console.repository(), new BaseCallback<JcrRepositoryDescriptor>() {
            @Override
            public void onSuccess(JcrRepositoryDescriptor descriptor) {
                Collection<Param> params = descriptor.info();
                ListGridRecord[] data = new ListGridRecord[params.size()];
                int i = 0;
                for (Param p : params) {
                    ListGridRecord record = new ListGridRecord();
                    record.setAttribute("icon", "blue");
                    record.setAttribute("name", p.getName());
                    record.setAttribute("value", p.getValue());
                    data[i++] = record;
                }
                
                grid.setData(data);
            }
        });
    }
}
