/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.ListGridFieldType;
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
import com.smartgwt.client.widgets.tab.Tab;
import java.util.Collection;
import org.modeshape.web.shared.ResultSet;

/**
 *
 * @author kulikov
 */
public class QueryPanel extends Tab {
    private TextAreaItem queryEditor = new TextAreaItem();
    private SubmitItem execButton = new SubmitItem("Execute");
    private ListGrid grid = new ListGrid();
    
    private ComboBoxItem langBox = new ComboBoxItem();
    
    private Console console;
    
    public QueryPanel(Console console) {        
        super();
        this.console = console;
        
        setTitle("Query");
        setIcon("icons/data.png");
        
        langBox.setTitle("Query language");
        
        VLayout layout = new VLayout();
        
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
        
        layout.addMember(queryForm);
        layout.addMember(grid);

        queryForm.addSubmitValuesHandler(new ButtonClickHandler());
        this.setPane(layout);
    }
    
    public void init() {
        console.jcrService.supportedQueryLanguages(new SupportedLangsQueryHandler());
    }
    
    public class ButtonClickHandler implements SubmitValuesHandler {

        @Override
        public void onSubmitValues(SubmitValuesEvent event) {
            console.jcrService.query(
                    queryEditor.getEnteredValue(), 
                    langBox.getEnteredValue(), 
                    new QueryResultHandler());
        }

    }
    
    public class SupportedLangsQueryHandler implements AsyncCallback<String[]> {

        @Override
        public void onFailure(Throwable caught) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void onSuccess(String[] result) {
            langBox.setValueMap(result);
        }
    
    }
    
    public class QueryResultHandler implements AsyncCallback<ResultSet> {

        @Override
        public void onFailure(Throwable caught) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

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
        }
        
    }
}


