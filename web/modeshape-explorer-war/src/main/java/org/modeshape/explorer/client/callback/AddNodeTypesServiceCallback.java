package org.modeshape.explorer.client.callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.util.SC;

/**
 *
 * @author James Pickup
 *
 */
public class AddNodeTypesServiceCallback implements AsyncCallback<Boolean> {

    @Override
    public void onSuccess(Boolean result) {
//		SC.say("Added CND node types successfully.");
        SC.warn("Custom node type registering not available over RMI yet.");
        Explorer.hideLoadingImg();
    }

    @Override
    public void onFailure(Throwable caught) {
        SC.warn(caught.toString(), new NewBooleanCallback());
        Explorer.hideLoadingImg();
    }
}
