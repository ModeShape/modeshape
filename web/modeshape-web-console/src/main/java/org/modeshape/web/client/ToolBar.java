/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 *
 * @author kulikov
 */
public class ToolBar extends HLayout {
    private Console console;
    
    public ToolBar(Console console) {
        super();
        this.console = console;
        
        Button addNodeButton = new Button();
        addNodeButton.setTitle("New");
        addNodeButton.setWidth(30);
        addNodeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (event.isLeftButtonDown()) {
                    ToolBar.this.console.newNodeDialog.showDialog();
                }
            }            
        });
//        addNodeButton.setHeight(30);
        
        addMember(addNodeButton);
        this.setHeight(30);
    }
    
    
}
