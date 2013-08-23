package org.modeshape.explorer.client.handler;

import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.widgets.grid.events.CellClickEvent;
import com.smartgwt.client.widgets.grid.events.CellClickHandler;

/**
 * @author chrisjennings
 * 
 */
public class IconGridHandler implements CellClickHandler {

	private Explorer jackrabbitExplorer;

	public IconGridHandler(Explorer jackrabbitExplorer) {
		this.jackrabbitExplorer = jackrabbitExplorer;
	}

	@Override
	public void onCellClick(CellClickEvent event) {
		jackrabbitExplorer.changeCurrentNodeTypeAssociation(event.getRecord()
				.getAttribute("path"));
		jackrabbitExplorer.hidePossibleIconsWindow();
	}
}
