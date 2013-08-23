package org.modeshape.explorer.client.callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.util.BooleanCallback;
import com.smartgwt.client.util.SC;

/**
 *
 * @author James Pickup
 *
 */
public class LoginServiceCallback implements AsyncCallback<Boolean> {

    private Explorer explorer;

    public LoginServiceCallback(Explorer explorer) {
        this.explorer = explorer;
    }

    @Override
    public void onSuccess(Boolean result) {
        Explorer.hideLoadingImg();
        Explorer.service.getAvailableNodeTypes(new AvailableNodeTypesServiceCallback());
        explorer.showMainForm();
    }

    @Override
    public void onFailure(Throwable caught) {
        SC.warn("There was an error logging in: " + caught.toString(), new LoginErrorCallback());
        Explorer.hideLoadingImg();
    }

    public class LoginErrorCallback implements BooleanCallback {

        @Override
        public void execute(Boolean value) {
            Explorer.loginWindow.show();
        }
    }
}
