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
package org.modeshape.web.client.contents;

import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.VLayout;

/**
 * Two columns layout.
 * 
 * Left column contains children nodes and binary editor, right column contains
 * details of the node such as properties and permissions.
 * 
 * 
 * @author kulikov
 */
public class ContentsLayout extends HLayout {
    private ChildrenEditor children;
    private DetailsLayout details;
    private boolean showDetails = false;
    
    /**
     * Creates layout.
     * 
     * @param children children nodes area.
     * @param binary binary content area.
     * @param details node details area.
     */
    public ContentsLayout(ChildrenEditor children, BinaryEditor binary, DetailsLayout details) {
        this.children = children;
        this.details = details;
        
        //left column
        VLayout panel = new VLayout();

        HLayout hstrut = new HLayout();
        hstrut.setHeight(30);
        
        //put children and binary editors separated by stut.
        panel.addMember(children);
        panel.addMember(hstrut);
        panel.addMember(binary);
        
        VLayout strut = new VLayout();
        strut.setWidth(10);
        
        addMember(panel);
        addMember(strut);
        addMember(details);
    }
    
    /**
     * Gets status of the details area.
     * 
     * @return true if details are visible on the screen.
     */
    public boolean showDetails() {
        return showDetails;
    }
    
    /**
     * Modifies status of the details area on the screen.
     * 
     * @param showDetails true enables details on the screen.
     */
    public void setShowDetails( boolean showDetails ) {
        this.showDetails = showDetails;
        details.setVisible(showDetails);
    }
    
    @Override
    public void setVisible(boolean visible) {
        children.setVisible(visible);
        if (showDetails) {
            details.setVisible(visible);
        } else {
            details.setVisible(false);
        }
    }
    
}
