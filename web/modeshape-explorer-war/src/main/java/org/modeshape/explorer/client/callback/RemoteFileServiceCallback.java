package org.modeshape.explorer.client.callback;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import org.modeshape.explorer.client.domain.RemoteFile;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * @author chrisjennings
 *
 */
public class RemoteFileServiceCallback implements AsyncCallback<List<RemoteFile>> {

	private Explorer jackrabbitExplorer;
	public RemoteFileServiceCallback(Explorer jackrabbitExplorer) {
		this.jackrabbitExplorer = jackrabbitExplorer;
	}

	@Override
	public void onSuccess(List<RemoteFile> results) {
		if (null == results || results.size() < 1) {
			SC.say("No possible icon files found.");
			Explorer.hideLoadingImg();
			return;
		}
		ListGridRecord[] iconFilesListGridRecords = new ListGridRecord[results.size()];
		int i = 0;
		for (RemoteFile remoteFile : results) {
			ListGridRecord listGridRecord = new ListGridRecord();
			listGridRecord.setAttribute("imagePath", remoteFile.getImagePath());
			listGridRecord.setAttribute("path", remoteFile.getPath());
			listGridRecord.setAttribute("isDir", remoteFile.isDirectory());
			iconFilesListGridRecords[i] = listGridRecord;
			i++;
		}
		jackrabbitExplorer.getRemoteIconFilesListGrid().setData(iconFilesListGridRecords);
		jackrabbitExplorer.showPossibleIconsWindow();
		Explorer.hideLoadingImg();		
	}

	@Override
	public void onFailure(Throwable caught) {
		SC.warn("There was an error: " + caught.toString(), new NewBooleanCallback());
		Explorer.hideLoadingImg();
	}
}
