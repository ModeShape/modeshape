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

import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Pager control element.
 * 
 * @author kulikov
 */
public abstract class Pager extends HLayout {
    
    private final Label[] pageNumber = new Label[10];
    private final TextItem itemsPerPageEditor = new TextItem("Nodes per page");
    
    private int pageTotal;

    public Pager() {
        super();
        setHeight(30);
        
        final Label pageLabel = new Label();
        pageLabel.setContents("<b>Page:</b>");
        pageLabel.setMargin(3);
        pageLabel.setAutoWidth();
        addMember(pageLabel);
        
        for (int i = 0; i < pageNumber.length; i++) {
            pageNumber[i] = new Label();
            pageNumber[i].setAutoWidth();
            pageNumber[i].addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    Label label = (Label) event.getSource();
                    String indexStr = label.getContents();
                    if (indexStr.equals("..")) {
                        return;
                    }
                    int index = Integer.parseInt(indexStr) - 1;
                    fetch(index);
                    draw(index);
                }
            });
            pageNumber[i].setMargin(3);
            addMember(pageNumber[i]);
        }
        
        itemsPerPageEditor.setWidth(50);
        itemsPerPageEditor.setValue("10");
        
        DynamicForm form = new DynamicForm();
        form.setHeight100();
        form.setWidth(200);
        form.setLayoutAlign(VerticalAlignment.CENTER);
        form.setItems(itemsPerPageEditor);
        
        Label strut = new Label();
        strut.setHeight100();
        strut.setWidth100();
        
        addMember(strut);
        addMember(form);
    }

    /**
     * Assigns total number of records.
     * 
     * @param amount 
     */
    public void setRecordsAmount(int amount) {
        int ipp = Integer.parseInt(itemsPerPageEditor.getValueAsString());
        pageTotal = amount % ipp == 0? amount / ipp : amount / ipp + 1;
        draw(0);
    }
    
    /**
     * Gets amount of records displayed by one page.
     * 
     * @return 
     */
    public int getRecordsPerPage() {
        return Integer.parseInt(itemsPerPageEditor.getValueAsString());
    }
    /**
     * Fetches records from source.
     * 
     * @param index page index
     */
    public abstract void fetch(int index);

    private void draw(int index) {
        for (int i = 0; i < pageNumber.length; i++) {
            pageNumber[i].setVisible(false);
        }

        if (pageTotal <= pageNumber.length) {
            for (int i = 0; i < pageTotal; i++) {
                pageNumber[i].setContents(Integer.toString(i + 1));
                pageNumber[i].setVisible(true);
            }
        } else if (index < pageTotal / 2) {
            pageNumber[0].setContents("1");
            pageNumber[0].setVisible(true);

            for (int i = 1; i < pageNumber.length - 1; i++) {
                pageNumber[i].setContents(Integer.toString(i + 1));
                pageNumber[i].setVisible(true);
            }

            pageNumber[pageNumber.length - 2].setContents("..");
            pageNumber[pageNumber.length - 2].setVisible(true);

            pageNumber[pageNumber.length - 1].setContents(Integer.toString(pageTotal));
            pageNumber[pageNumber.length - 1].setVisible(true);

        } else {

            pageNumber[0].setContents("1");
            pageNumber[0].setVisible(true);

            pageNumber[1].setContents("..");
            pageNumber[1].setVisible(true);

            for (int i = 2; i < pageNumber.length - 1; i++) {
                pageNumber[i].setContents(Integer.toString(i + 1));
                pageNumber[i].setVisible(true);
            }

        }
        
        String idx = Integer.toString(index + 1);
        for (int i = 0; i < pageNumber.length; i++) {
            if (pageNumber[i].getContents().equals(idx)) {
                pageNumber[i].setStyleName("page-index-selected");
            } else {
                pageNumber[i].setStyleName("page-index");
            }
        }
    }
}
