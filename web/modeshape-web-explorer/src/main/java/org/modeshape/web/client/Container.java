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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author kulikov
 */
public class Container extends View {
    
    private VLayout[] pages;
    
    public Container(JcrServiceAsync jcrService, ViewPort viewPort){
        super(viewPort, null);   
//        pages = new VLayout[] {new Contents(jcrService, viewPort),  new RepositoryInfo(viewPort, null), new QueryPanel(null)};
        
        setAlign(Alignment.CENTER);        
        setDefaultLayoutAlign(Alignment.CENTER);
        setLayoutAlign(Alignment.CENTER);        
//        setWidth("70%");
        
        VLayout layout = new VLayout();
        addMember(layout);
        
        
        layout.setAlign(Alignment.CENTER);
        layout.setDefaultLayoutAlign(Alignment.CENTER);
        
        HLayout vstrut = new HLayout();
        vstrut.setHeight(10);
        
        layout.addMember(pages[0]);
        layout.addMember(pages[1]);
        layout.addMember(pages[2]);
        layout.addMember(vstrut);
        
        show(0);
    }
    
    public void show(int k) {
        for (int i = 0; i < pages.length; i++) {
            pages[i].setVisible(false);
        }
        
        if (k < pages.length) {
            pages[k].setVisible(true);
            pages[k].show();
        }
    }
}
