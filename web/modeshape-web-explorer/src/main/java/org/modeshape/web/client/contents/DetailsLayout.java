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

import com.smartgwt.client.widgets.layout.VLayout;
import org.modeshape.web.client.grid.TabGrid;
import org.modeshape.web.client.grid.TabsetGrid;
import org.modeshape.web.shared.JcrNode;

/**
 *
 * @author kulikov
 */
public class DetailsLayout extends VLayout {
    private PropertiesEditor props;
    private TabsetGrid grid;
    
    public DetailsLayout(PropertiesEditor props, PermissionsEditor acl) {
        setWidth(300);
        grid = new TabsetGrid(
                new String[]{"Properties", "Permissions"},
                new TabGrid[]{props, acl} );
        addMember(grid);
    }
    
    public void show(JcrNode node) {
        props.show(node);
    }
}
