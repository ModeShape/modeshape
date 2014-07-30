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
