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
package org.modeshape.web.client.nt;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.util.Collection;
import org.modeshape.web.client.Console;
import org.modeshape.web.shared.Form;
import org.modeshape.web.shared.JcrNodeType;

/**
 *
 * @author kulikov
 */
public class NodeTypesForm extends Form {
    private final Console console;
    private final ListGrid grid = new ListGrid();

    public NodeTypesForm(Console console) {
        this.console = console;
        this.grid.setWidth100();
        this.grid.setHeight100();
        
        ListGridField[] fields = new ListGridField[5];
        
        fields[0] = new ListGridField("icon", " ");
        fields[0].setCanEdit(false);
        fields[0].setImageURLPrefix("icons/bullet_");
        fields[0].setImageURLSuffix(".png");
        fields[0].setWidth(30);
        fields[0].setType(ListGridFieldType.IMAGE);
        
        fields[1] = new ListGridField("name", "Type Name");
        fields[1].setCanEdit(false);
        fields[1].setType(ListGridFieldType.TEXT);

        fields[2] = new ListGridField("is_primary", "Primary type");
        fields[2].setCanEdit(false);
        fields[2].setType(ListGridFieldType.BOOLEAN);
        fields[2].setWidth(30);

        fields[3] = new ListGridField("is_mixin", "Mixin type");
        fields[3].setCanEdit(false);
        fields[3].setType(ListGridFieldType.BOOLEAN);
        fields[3].setWidth(30);
        
        fields[4] = new ListGridField("is_abstract", "Abstract");
        fields[4].setCanEdit(false);
        fields[4].setType(ListGridFieldType.BOOLEAN);
        fields[4].setWidth(30);
        
        grid.setFields(fields);
        addMember(grid);
    }
    
    @Override
    public void init() {
        console.jcrService().nodeTypes(console.contents().repository(), 
                console.contents().workspace(), 
                new AsyncCallback<Collection<JcrNodeType>> () {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void onSuccess(Collection<JcrNodeType> result) {
                displayNodeTypes(result);
            }
        });
    }
    
    private void displayNodeTypes(Collection<JcrNodeType> nodeTypes) {
        ListGridRecord[] data = new ListGridRecord[nodeTypes.size()];
        int i = 0;
        for (JcrNodeType t : nodeTypes) {
            data[i] = new ListGridRecord();
            data[i].setAttribute("name", t.getName());
            data[i].setAttribute("is_primary", t.isPrimary());
            data[i].setAttribute("is_mixin", t.isMixin());
            data[i].setAttribute("is_abstract", t.isAbstract());
            i++;
        }
        grid.setData(data);
    }
}
