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
package org.modeshape.web.client.query;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridFieldType;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextAreaItem;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.util.Collection;
import org.modeshape.web.client.Console;
import org.modeshape.web.shared.Form;
import org.modeshape.web.shared.ResultSet;

/**
 *
 * @author kulikov
 */
public class QueryForm extends Form {
    private final static String DEFAULT_LANG = "JCR-SQL2";
    
    private final TextAreaItem queryEditor = new TextAreaItem();
    private final SubmitItem execButton = new SubmitItem("Execute");
    private final ComboBoxItem langBox = new ComboBoxItem("Query language");
    private final ListGrid grid = new ListGrid();
    
    private final Console console;
    
    public QueryForm(final Console console) {
        this.console = console;
        setWidth100();
        
        grid.setWidth100();
        grid.setHeight(380);
        
        DynamicForm queryForm = new DynamicForm(); 
        queryForm.setBackgroundColor("#e6f1f6");
        queryForm.setID("query-form-1");
        queryForm.setNumCols(3);

        queryEditor.setName("query");
        queryEditor.setTitle("Query");
        queryEditor.setStartRow(true);
        queryEditor.setEndRow(false);
        queryEditor.setWidth(500);
        
        execButton.setStartRow(false);
        execButton.setEndRow(true);
        queryForm.setItems(queryEditor, execButton, langBox);
        queryForm.addSubmitValuesHandler(new SubmitValuesHandler() {
            @Override
            public void onSubmitValues(SubmitValuesEvent event) {
                console.jcrService().query(console.contents().repository(),
                        console.contents().workspace(),
                        queryEditor.getEnteredValue(),
                        langBox.getEnteredValue(),
                        new AsyncCallback<ResultSet>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        SC.say(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(ResultSet data) {
                        displayResultSet(data);
                    }
                });
            }
        });
        
        addMember(queryForm);
        addMember(grid);
    }
    
    private void displayResultSet(ResultSet rs) {
        String[] columnNames = rs.getColumnNames();
        ListGridField[] fields = new ListGridField[columnNames.length + 1];

        fields[0] = new ListGridField("icon", " ");
        fields[0].setCanEdit(false);
        fields[0].setImageURLPrefix("icons/bullet_");
        fields[0].setImageURLSuffix(".png");
        fields[0].setWidth(30);
        fields[0].setType(ListGridFieldType.IMAGE);

        for (int i = 1; i < fields.length; i++) {
            fields[i] = new ListGridField(columnNames[i - 1], columnNames[i - 1]);
            fields[i].setCanEdit(false);
            fields[i].setShowHover(true);
        }

        grid.setFields(fields);
        
        Collection<String[]> rows = rs.getRows();
        ListGridRecord[] tbl = new ListGridRecord[rows.size()];
        int j = 0;
        for (String[] columns : rows) {
            ListGridRecord rec = new ListGridRecord();
            rec.setAttribute("icon", "blue");
            for (int i = 0; i < columns.length; i++) {
                rec.setAttribute(columnNames[i], columns[i]);
            }
            tbl[j++] = rec;
        }

        grid.setData(tbl);
        grid.show();
    }
    
    @Override
    public void init() {
        console.jcrService().supportedQueryLanguages(console.contents().repository(),
                console.contents().workspace(),new AsyncCallback<String[]>() {

            @Override
            public void onFailure(Throwable caught) {
                SC.say(caught.getMessage());
            }

            @Override
            public void onSuccess(String[] result) {
                langBox.setValueMap(result);
                langBox.setValue(defaultLang(result));
            }
        });
    }
    
    private String defaultLang(String[] options) {
        for (String option : options) {
            if (option.toUpperCase().equals(DEFAULT_LANG)) {
                return option;
            }
        }
        return "";
    }
    
    
}
