package org.modeshape.explorer.client.callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;

/**
 * @author chrisjennings
 *
 */
public class ChangeNodeTypeIconAssociationCallback implements AsyncCallback<Boolean> {
	private Explorer jackrabbitExplorer;

	public ChangeNodeTypeIconAssociationCallback(Explorer jackrabbitExplorer) {
		this.jackrabbitExplorer = jackrabbitExplorer;
	}

	@Override
	public void onFailure(Throwable throwable) {
		//SC.warn("There was an error: " + throwable.toString(), new NewBooleanCallback());
		Explorer.hideLoadingImg();
	}

	@Override
	public void onSuccess(Boolean arg0) {
		jackrabbitExplorer.getNodeTypeIcons();
		jackrabbitExplorer.refreshFromRoot();
	}
}
