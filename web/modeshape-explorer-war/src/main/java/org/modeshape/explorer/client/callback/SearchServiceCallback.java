package org.modeshape.explorer.client.callback;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.util.SC;
import com.smartgwt.client.widgets.grid.ListGridRecord;

/**
 * 
 * @author James Pickup
 *
 */
public class SearchServiceCallback implements AsyncCallback<List<String>> {
	private Explorer jackrabbitExplorer;
	public SearchServiceCallback(Explorer jackrabbitExplorer) {
		this.jackrabbitExplorer = jackrabbitExplorer;
	}
	public void onSuccess(List<String> result) {
		List<String> returnList = result;

		if (null == returnList || returnList.size() < 1) {
			SC.say("No results for search.");
			Explorer.hideLoadingImg();
			return;
		}
		ListGridRecord[] searchResultsListGridRecords = new ListGridRecord[]{};
		searchResultsListGridRecords = new ListGridRecord[returnList.size()];
		int i = 0;
		for (String resultString : returnList) {
			ListGridRecord listGridRecord = new ListGridRecord();
	        listGridRecord.setAttribute("path", resultString);
			searchResultsListGridRecords[i] = listGridRecord;
			i++;
		}
		jackrabbitExplorer.searchResultsListGrid.setData(searchResultsListGridRecords);
		
		jackrabbitExplorer.bottomRightTabSet.selectTab(1);
		Explorer.hideLoadingImg();
	}

	public void onFailure(Throwable caught) {
		SC.warn("There was an error: " + caught.toString(), new NewBooleanCallback());
		Explorer.hideLoadingImg();
	}
}

