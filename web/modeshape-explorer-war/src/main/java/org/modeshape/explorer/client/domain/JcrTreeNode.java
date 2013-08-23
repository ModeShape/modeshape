package org.modeshape.explorer.client.domain;

import java.io.Serializable;
import java.util.Map;

import org.modeshape.explorer.client.Explorer;
import com.smartgwt.client.widgets.tree.TreeNode;

public class JcrTreeNode extends TreeNode implements Serializable {
	private static final long serialVersionUID = 2162736740069966873L;
	private Map<String, String> properties;
	
	public JcrTreeNode() {
		super();
	}

	public JcrTreeNode(String name, String path, String primaryNodeType,
			Map<String, String> properties, JcrTreeNode... children) {
		setTitle(name);
		setAttribute("path", path);
		setAttribute("primaryNodeType", primaryNodeType);
		this.properties = properties;
		setAttribute("treeGridIcon", Explorer.defaultIcon);
		setChildren(children);
	}

	public JcrTreeNode(String name, String path, String primaryNodeType, 
			Map<String, String> properties, String treeGridIcon, JcrTreeNode... children) {
		setTitle(name);
		setAttribute("path", path);
		setAttribute("primaryNodeType", primaryNodeType);
		this.properties = properties;
		//setAttribute("treeGridIcon", JackrabbitExplorer.defaultIcon);
		setAttribute("treeGridIcon", treeGridIcon);
		setChildren(children);
	}

	public Map<String, String> getProperties() {
		return properties;
	}

	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}
	
}
