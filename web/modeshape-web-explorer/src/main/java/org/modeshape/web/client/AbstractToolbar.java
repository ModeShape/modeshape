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

import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Tool bar for the repository management.
 * 
 * @author kulikov
 */
public class AbstractToolbar extends HLayout {
    private Console console;

    public AbstractToolbar( Console console ) {
        super();
        this.console = console;
        this.setHeight(30);
    }

    protected Console console() {
        return console;
    }

    protected void button( String title,
                           String icon,
                           String toolTip,
                           ClickHandler handler ) {
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
