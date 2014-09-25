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

import com.smartgwt.client.widgets.layout.VLayout;
import java.util.HashMap;

/**
 *
 * @author kulikov
 */
public class View extends VLayout {
    private ViewPort viewPort;
    private View parent;
    private HashMap<String, View> children = new HashMap<String,View>();
    
    public View(ViewPort viewPort, View parent) {
        super();
        this.viewPort = viewPort;
        this.parent = parent;
    }
    
    public View parent() {
        return parent;
    }
    
    public ViewPort viewPort() {
        return viewPort;
    }
    
    public void addChild(String name, View child) {
        children.put(name, child);
    }
    
    public void showChild(String name) {
        
    }
    
}
