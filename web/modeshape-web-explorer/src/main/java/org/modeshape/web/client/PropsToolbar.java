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

import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;

/**
 *
 * @author kulikov
 */
public class PropsToolbar extends AbstractToolbar {
    
    public PropsToolbar(Console console) {
        super(console);
        
        //Add mixin
        button("", "icons/hcards_add.png", "Add mixin to the node", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console().addMixinDialog.showModal();
            }            
        });

        //Remove mixin
        button("", "icons/hcards_remove.png", "Remove mixin", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console().removeMixinDialog.showModal();
            }            
        });

        //Remove mixin
        button("", "icons/tag_add.png", "Add property", new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                console().addPropertyDialog.showModal();
            }            
        });
        
    }
}
