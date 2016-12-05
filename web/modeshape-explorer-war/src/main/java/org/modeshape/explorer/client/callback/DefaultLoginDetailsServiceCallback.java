package org.modeshape.explorer.client.callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.domain.LoginDetails;
import com.smartgwt.client.util.SC;

/**
 * 
 * @author James Pickup
 *
 */
public class DefaultLoginDetailsServiceCallback implements AsyncCallback<LoginDetails> {
	private Explorer jackrabbitExplorer;
	public DefaultLoginDetailsServiceCallback(Explorer jackrabbitExplorer) {
		this.jackrabbitExplorer = jackrabbitExplorer;
	}
	public void onSuccess(LoginDetails result) {
		jackrabbitExplorer.loginDetails = result;
		Explorer.hideLoadingImg();
		jackrabbitExplorer.showLoginBox();
	}

	public void onFailure(Throwable caught) {
		SC.warn("There was an error: " + caught.toString(), new NewBooleanCallback());
		Explorer.hideLoadingImg();
	}
}

