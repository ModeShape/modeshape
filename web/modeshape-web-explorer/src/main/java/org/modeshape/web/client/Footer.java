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
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author kulikov
 */
public class Footer extends VLayout {
    
    public Footer() {
        super();
        HLayout band = new HLayout();
        band.setHeight(50);
        band.setBackgroundColor("#424242");
        
        addMember(band);
        
        Img logo = new Img();
        logo.setSrc("icons/rht.png");

        logo.setHeight(50);
        logo.setWidth(80);
        logo.setValign(VerticalAlignment.CENTER);        
        
        band.addMember(logo);
        
        HLayout band2 = new HLayout();
        band2.setHeight(10);
        
        addMember(band2);
        
        Label wsLabel = new Label("");
        wsLabel.setWidth100();
        wsLabel.setContents("<a href=\"http://www.jboss.org\"><b>Report a problem</b></a>");
        wsLabel.setHeight100();
        wsLabel.setStyleName("white-ref");
        wsLabel.setAlign(Alignment.RIGHT);
        
        band.addMember(wsLabel);
    }
    
}
