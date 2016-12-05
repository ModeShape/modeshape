package org.modeshape.explorer.client.callback;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.util.SC;

/**
 * 
 * @author James Pickup
 *
 */
public class CRUDServiceCallback implements AsyncCallback<String> {
	private Explorer explorer;
        
	String newNodePath;
	String deletedNodePath;
        
	public CRUDServiceCallback(Explorer explorer, String newNodePath, String deletedNodePath) {
		this.explorer = explorer;
		this.newNodePath = newNodePath;
		this.deletedNodePath = deletedNodePath;
	}
	public void onSuccess(String result) {
		String returnMessage = result;
		//if returMessage says the operation failed. 
			//return
		
		if (null != newNodePath && !newNodePath.equals("")) {
			explorer.treeRecordClick(true, newNodePath);
		}
		if (null != deletedNodePath && !deletedNodePath.equals("")) {
			explorer.treeDeleteUpdate(getParentPath(deletedNodePath));
		}
		
		SC.say(returnMessage);
		Explorer.hideLoadingImg();
	}

	public void onFailure(Throwable caught) {
		SC.warn(caught.toString(), new NewBooleanCallback());
		Explorer.hideLoadingImg();
	}
	
	private static String getParentPath(String path) {
		String parentPath = path.substring(0, path.lastIndexOf('/'));
		if (null != parentPath && parentPath.equals("")) {
			return "/";
		}
		return parentPath;
	}

}

