package org.modeshape.explorer.client.domain;

import java.io.Serializable;

/**
 * @author chrisjennings
 *
 */
public class RemoteFile implements Serializable
{
	private static final long serialVersionUID = -1405748667932914839L;

	private static final String DEFAULT_DIR_ICON = "images/icons/folder_closed.png";
	private String path;
	private boolean isDirectory;
	
	public RemoteFile() {
		//
	}
	
	public RemoteFile(String path, boolean isDirectory){
		this.isDirectory = isDirectory;
		this.path = path;
	}

	public String getImagePath() {
		if(isDirectory()) {
			return DEFAULT_DIR_ICON;
		}
		return path.replaceAll("\\\\", "\\/");
	}

	public String getPath() {
		return path;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public boolean isDirectory() {
		return isDirectory;
	}
	public void setDirectory(boolean isDirectory) {
		this.isDirectory = isDirectory;
	}
}
