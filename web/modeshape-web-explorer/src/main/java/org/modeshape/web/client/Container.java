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
