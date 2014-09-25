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
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 *
 * @author kulikov
 */
public class ViewPort extends VLayout {
    private VLayout panel;
    private VLayout frame;
    private Label caption = new Label();
    
    private final JcrServiceAsync jcrService;
    
    
    public ViewPort(JcrServiceAsync jcrService) {
        super();
        this.jcrService = jcrService;
        
        caption.setMargin(5);
        caption.setStyleName("caption");
        
        setWidth("70%");
        setLayoutAlign(Alignment.CENTER);
        setAlign(Alignment.CENTER);
        setDefaultLayoutAlign(Alignment.CENTER);
        
        Canvas text = new Canvas();
        text.setContents("ModeShape is a distributed, hierarchical, transactional, and consistent data store with support for queries, full-text search, events, versioning, references, and flexible and dynamic schemas. It is very fast, highly available, extremely scalable, and it is 100% open source and written in Java. Clients use the JSR-283 standard Java API for content repositories (aka, JCR) or ModeShape's REST API, and can query content through JDBC and SQL.");
        text.setWidth100();
        text.setAutoHeight();
        
        addMember(new Strut(30));
//        addMember(text);
//        addMember(new Strut(10));
//        addMember(pathLabel);
//        addMember(new Strut(5));
        
        panel = new VLayout();
        panel.setWidth100();
        panel.setHeight100();
        
//        panel.setStyleName("viewport");
        
//        HLayout p = new HLayout();
//        p.setHeight(30);
//        p.setBackgroundColor("green");
//        p.addMember(caption);
//        panel.addMember(p);        
        addMember(panel);   
        
        frame = new VLayout();
        panel.addMember(frame);
        
        setCaption("Repositories");
    }
    
    public JcrServiceAsync jcrService() {
        return jcrService;
    }
    
    public final void setCaption(String title) {
        this.caption.setContents(title);
    }
    
    
    public void display(View view) {
        Canvas[] members = frame.getMembers();
        for (Canvas canvas : members) {
            canvas.setVisible(false);
        }
        
        boolean found = false;
        for (Canvas canvas : members) {
            if (canvas == view) {
                found = true;
                break;
            }
        }

        if (!found) {
            frame.addMember(view);
        }
        view.setVisible(true);
        view.show();
    }
    
    
    private class Strut extends VLayout {
        public Strut(int size) {
            super();
            setHeight(size);
        }
    }
    
}
