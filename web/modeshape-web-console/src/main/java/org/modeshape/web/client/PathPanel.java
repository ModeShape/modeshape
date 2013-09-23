/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.events.SubmitValuesEvent;
import com.smartgwt.client.widgets.form.events.SubmitValuesHandler;
import com.smartgwt.client.widgets.form.fields.SubmitItem;
import com.smartgwt.client.widgets.form.fields.TextItem;

/**
 * Panel with control items which allows to switch path.
 * 
 * @author kulikov
 */
public class PathPanel extends DynamicForm {

    private TextItem pathEditor = new TextItem();
    private SubmitItem goButton = new SubmitItem();

    private Console console;
    
    public PathPanel(Console console) {
        super();
        this.console = console;
        
        setID("pathPanel");
        setMargin(0);

        pathEditor.setTitle("Path");
        pathEditor.setWidth(500);
        pathEditor.setStartRow(true);
        pathEditor.setEndRow(false);
        pathEditor.setRequired(true);

        goButton.setTitle("Go");
        goButton.setWidth(100);
        goButton.setEndRow(true);
        goButton.setStartRow(false);

        this.setNumCols(3);
        this.setWidth(700);
        setItems(pathEditor, goButton);
        addSubmitValuesHandler(new PathChangeHandler());
    }
    
    /**
     * Implements procedure of path selection.
     */
    private class PathChangeHandler implements SubmitValuesHandler {

        @Override
        public void onSubmitValues(SubmitValuesEvent event) {
            console.navigator.openFolder(pathEditor.getValueAsString());
        }
        
    }
}