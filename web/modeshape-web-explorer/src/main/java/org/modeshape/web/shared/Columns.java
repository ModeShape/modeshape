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

import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.VerticalAlignment;
import com.smartgwt.client.widgets.layout.HLayout;

/**
 *
 * @author kulikov
 */
public class Columns extends HLayout {
    public Columns(int hAlign, int vAlign) {
        switch (hAlign) {
            case Align.LEFT :
                this.setDefaultLayoutAlign(Alignment.LEFT);
                this.setLayoutAlign(Alignment.LEFT);
                this.setAlign(Alignment.LEFT);
                break;
            case Align.CENTER :
                this.setDefaultLayoutAlign(Alignment.CENTER);
                this.setLayoutAlign(Alignment.CENTER);
                this.setAlign(Alignment.CENTER);
                break;
            case Align.RIGHT :
                this.setDefaultLayoutAlign(Alignment.RIGHT);
                this.setLayoutAlign(Alignment.RIGHT);
                this.setAlign(Alignment.RIGHT);
                break;
        }
        

        switch (vAlign) {
            case Align.TOP :
                this.setDefaultLayoutAlign(VerticalAlignment.TOP);
                this.setLayoutAlign(VerticalAlignment.TOP);
                this.setAlign(VerticalAlignment.TOP);
                break;
            case Align.CENTER :
                this.setDefaultLayoutAlign(VerticalAlignment.CENTER);
                this.setLayoutAlign(VerticalAlignment.CENTER);
                this.setAlign(VerticalAlignment.CENTER);
                break;
            case Align.BOTTOM :
                this.setDefaultLayoutAlign(VerticalAlignment.BOTTOM);
                this.setLayoutAlign(VerticalAlignment.BOTTOM);
                this.setAlign(VerticalAlignment.BOTTOM);
                break;
        }
        
    }
    
    public void addStrut( int width ) {
        HLayout strut = new HLayout();
        strut.setWidth(width);
        strut.setHeight100();
        addMember(strut);
    }
}
