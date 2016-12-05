package org.modeshape.explorer.client.callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.util.SC;

/**
 * 
 * @author James Pickup
 *
 */
public class GetBrowsableContentFilterRegexsServiceCallback implements AsyncCallback<String> {
	public void onSuccess(String result) {
		Explorer.browsableContentFilterRegex = result;
		Explorer.hideLoadingImg();
	}

	public void onFailure(Throwable caught) {
		SC.warn("There was an error: " + caught.toString(), new NewBooleanCallback());
		Explorer.hideLoadingImg();
	}
}

