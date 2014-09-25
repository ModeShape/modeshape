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
package org.modeshape.web.client;

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
import com.smartgwt.client.widgets.layout.VLayout;
import java.util.Collection;
import org.modeshape.web.shared.ResultSet;

/**
 *
 * @author kulikov
 */
public class QueryPanel extends View {
    private final static String DEFAULT_LANG = "JCR-SQL2";
    
    private TextAreaItem queryEditor = new TextAreaItem();
    private SubmitItem execButton = new SubmitItem("Execute");
    private ListGrid grid = new ListGrid();
    
    private ComboBoxItem langBox = new ComboBoxItem();
    
    private Console console;
    public QueryPanel(Console console, JcrServiceAsync jcrService, ViewPort viewPort) {        
        super(viewPort, null);
        this.setHeight(500);
        this.console = console;
        
        this.setStyleName("grid-bg");
        this.setLayoutMargin(1);

        VLayout background = new VLayout();
        background.setWidth100();
        background.setHeight100();
        background.setStyleName("grid-panel");
        addMember(background);
        
        langBox.setTitle("Query language");
                
        DynamicForm queryForm = new DynamicForm(); 
        queryForm.setBackgroundColor("#d3d3d3");
        queryForm.setID("query-form");
        queryForm.setNumCols(3);

        queryEditor.setName("query");
        queryEditor.setTitle("Query");
        queryEditor.setStartRow(true);
        queryEditor.setEndRow(false);
        queryEditor.setWidth(500);
        
        execButton.setStartRow(false);
        execButton.setEndRow(true);
        queryForm.setItems(queryEditor, execButton, langBox);
        
        background.addMember(queryForm);
        background.addMember(grid);
        
        queryForm.addSubmitValuesHandler(new ButtonClickHandler());
    }
    
    public void init() {
        console.jcrService.supportedQueryLanguages(console.contents().repository(),
                console.contents().workspace(),
                new SupportedLangsQueryHandler());
    }
    
    public class ButtonClickHandler implements SubmitValuesHandler {

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void onSubmitValues(SubmitValuesEvent event) {
            console.jcrService.query(console.contents().repository(),
                    console.contents().workspace(),
                    queryEditor.getEnteredValue(), 
                    langBox.getEnteredValue(), 
                    new QueryResultHandler());
        }

    }
    
    public class SupportedLangsQueryHandler implements AsyncCallback<String[]> {

        @Override
        public void onFailure(Throwable caught) {
            SC.say(caught.getMessage());
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void onSuccess(String[] result) {
            langBox.setValueMap(result);
            langBox.setValue(defaultLang(result));
            console.display(QueryPanel.this);
        }
    
    }
    
    private String defaultLang(String[] options) {
        for (String option : options) {
            if (option.toUpperCase().equals(DEFAULT_LANG)) {
                return option;
            }
        }
        return "";
    }
    
    public class QueryResultHandler implements AsyncCallback<ResultSet> {

        @Override
        public void onFailure(Throwable caught) {
            SC.say(caught.getMessage());
        }

        @SuppressWarnings( "synthetic-access" )
        @Override
        public void onSuccess(ResultSet rs) {
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
            
            grid.setFields(fields);
            grid.setData(tbl);
            grid.show();
        }
        
    }
}


