/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.shared;

import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

/**
 *
 * @author kulikov
 */
public class ResultSet implements Serializable {
    private String[] columnNames;
    private ArrayList<String[]> result = new ArrayList();
    
    public ResultSet() {
    }
    
    public String[] getColumnNames() {
        return columnNames;
    }
    
    public void setColumnNames(String[] columnNames) {
        this.columnNames = columnNames;
    }
    
    public void setRows(Collection<String[]> rows) {
        this.result.clear();
        this.result.addAll(rows);
    }
    
    public Collection<String[]> getRows() {
        return result;
    }
    
}
