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
