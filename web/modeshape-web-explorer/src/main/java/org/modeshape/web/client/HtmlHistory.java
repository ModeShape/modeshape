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

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.PlaceHistoryHandler.Historian;
import com.google.gwt.user.client.History;

/**
 *
 * @author kulikov
 */
public class HtmlHistory implements Historian,
        // allows the use of ValueChangeEvent.fire()
        HasValueChangeHandlers<String> {

    private final SimpleEventBus handlers = new SimpleEventBus();
    private String token;
    
    public HtmlHistory() {
        initEvent();
        this.token = History.getToken();
    }

    @Override
    public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> valueChangeHandler) {
        return this.handlers.addHandler(ValueChangeEvent.getType(), valueChangeHandler);
    }

    @Override
    public String getToken() {
        return token;
    }

    @Override
    public void newItem(String token, boolean issueEvent) {
        this.token = token;
        pushState(token);
    }

    @Override
    public void fireEvent(GwtEvent<?> event) {
        this.handlers.fireEvent(event);
    }

    private native void initEvent() /*-{
     var that = this;
     var oldHandler = $wnd.onpopstate;
     $wnd.onpopstate = $entry(function(e) {
     var uri = $wnd.location
     that.@org.modeshape.web.client.HtmlHistory::onPopState(Ljava/lang/String;)(uri);
     if (oldHandler) {
     oldHandler();
     }
     });
     }-*/;

    private void onPopState(String url) {
        StringBuilder b = new StringBuilder();
        b.append(url);
        ValueChangeEvent.fire(this, b.toString());
    }

    private native void pushState(String url) /*-{
     $wnd.history.pushState(null, $doc.title, url);
     }-*/;
}
