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

import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 * Loading indicator.
 * 
 * @author kulikov
 */
public class LoadingIcon {
    private final Img loadingImg = new Img("loading.gif");    
    private final HLayout disabledHLayout = new HLayout();
    
    /**
     * Shows loading indicator at the given place of screen.
     * 
     * @param x horizontal coordinate (from left corner).
     * @param y vertical coordinate (from right corner).
     */
    public void show(int x, int y) {
        disabledHLayout.setSize("100%", "100%");
        disabledHLayout.setStyleName("disabledBackgroundStyle");
        disabledHLayout.show();

        loadingImg.setSize("100px", "100px");
        loadingImg.setTop(y); //loading image height is 50px
        loadingImg.setLeft(x); //loading image width is 50px
        loadingImg.show();
        loadingImg.bringToFront();
    }
    
    /**
     * Hides this indicator.
     */
    public void hide() {
        loadingImg.hide();
        disabledHLayout.hide();
    }
}
