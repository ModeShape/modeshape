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

    protected TextItem pathEditor = new TextItem();
    private SubmitItem goButton = new SubmitItem();

    protected Console console;

    public PathPanel( Console console ) {
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
    protected class PathChangeHandler implements SubmitValuesHandler {

        @Override
        public void onSubmitValues( SubmitValuesEvent event ) {
            console.navigator.openFolder(pathEditor.getValueAsString());
        }

    }
}
