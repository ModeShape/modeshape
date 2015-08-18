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
package org.modeshape.web.shared;

import com.smartgwt.client.widgets.Window;
import com.smartgwt.client.widgets.events.CloseClickHandler;
import com.smartgwt.client.widgets.events.CloseClientEvent;
import com.smartgwt.client.widgets.layout.VStack;
import org.modeshape.web.client.Console;

/**
 *
 * @author kulikov
 */
public abstract class ModalForm {
    private final VStack layout = new VStack();
    private final Window window = new Window();
    private final Form form;
    
    public ModalForm(final Console console, 
            int width, int height, String title, Form form) {
        this.form = form;
        this.form.setTop(22);
        initWindow(title, width, height);
    }
    
    private void initWindow(String title, int width, int height) {
        window.addChild(layout);        
        window.setTitle(title);
        window.setCanDragReposition(true);
        window.setCanDragResize(false);
        window.setShowMinimizeButton(false);
        window.setShowCloseButton(true);
        window.setWidth(width);
        window.setHeight(height);
        window.setAutoCenter(true);
        
        window.addCloseClickHandler(new CloseClickHandler() {
            @Override
            public void onCloseClick(CloseClientEvent event) {
                hide();
            }            
        });
        
        window.addChild(form);
        form.setWidth100();
        form.setHeight100();
    }
    
    /**
     * Shows this dialog modal.
     */
    public void showModal() {
        form.init();
        window.show();
    }
    
    /**
     * Hides this dialog.
     */
    public void hide() {
        window.hide();
    }
    
}
