/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.web.client;

import com.smartgwt.client.widgets.Button;
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
//        addNodeButton.setHeight(30);
        
        addMember(addNodeButton);
        this.setHeight(30);
    }
    
    
}
