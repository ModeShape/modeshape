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
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Tool bar for the repository management.
 * 
 * @author kulikov
 */
public class AbstractToolbar extends HLayout {
    private Console console;
    
    public AbstractToolbar(Console console) {
        super();
        this.console = console;
        this.setHeight(30);
    }

    /**
     * Gets access to the main form.
     * @return 
     */
    protected Console console() {
        return console;
    }
    
    /**
     * Adds new button to the toolbar.
     * 
     * @param title the title of the button
     * @param icon the icon of the button
     * @param handler handler class
     */
    protected void button(String title, String icon, String toolTip, ClickHandler handler) {
        Button button = new Button();
        button.setWidth(30);
        button.setHeight(30);
        button.setTitle(title);
        button.setIcon(icon);
        button.setTooltip(toolTip);
        button.setMargin(1);
        button.addClickHandler(handler);        
        addMember(button);
    }

    protected void spacer() {
        HLayout spacer = new HLayout();
        spacer.setWidth(5);
        addMember(spacer);
    }
}
