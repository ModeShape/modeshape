/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
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
     that.@org.modeshape.web.client.HtmlHistory::onPopState()();
     if (oldHandler) {
     oldHandler();
     }
     });
     }-*/;

    private void onPopState() {
        ValueChangeEvent.fire(this, getToken());
    }

    private native void pushState(String url) /*-{
     $wnd.history.pushState(null, $doc.title, url);
     }-*/;
}
