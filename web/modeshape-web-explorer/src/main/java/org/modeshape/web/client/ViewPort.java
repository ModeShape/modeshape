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
