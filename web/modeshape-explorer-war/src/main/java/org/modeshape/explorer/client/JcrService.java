package org.modeshape.explorer.client;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.modeshape.explorer.client.domain.JcrNode;
import org.modeshape.explorer.client.domain.LoginDetails;
import org.modeshape.explorer.client.domain.RemoteFile;

@RemoteServiceRelativePath("JcrService")
public interface JcrService extends RemoteService {
	LoginDetails getDefaultLoginDetails() throws SerializedException;
	Boolean loginLocal(String configFilePath, String homeDirPath, String workSpace, String userName, String password) throws Exception;
	Boolean loginViaJndi(String jndiName, String jndiContext, String workSpace, String userName, String password) throws Exception;
	Boolean loginViaRmi(String rmiUrl, String workSpace, String userName, String password) throws Exception;
	List<Map<String, List<JcrNode>>> getNodeTree(String path) throws SerializedException;
	List<JcrNode> getNode(String path) throws SerializedException;
	List<String> getAvailableNodeTypes() throws SerializedException;
	String addNewNode(String path, String newNodeName, String primaryNodeType, String jcrContentFileName, boolean cancel) throws SerializedException;
	String moveNode(String sourcePath, String destinationPath) throws SerializedException;
	String renameNode(String sourcePath, String newName) throws SerializedException;
	String copyNode(String sourcePath, String destinationPath) throws SerializedException;
	String cutAndPasteNode(String sourcePath, String destinationPath) throws SerializedException;
	String moveNodes(Map<String, String> nodeMap) throws SerializedException;
	String copyNodes(Map<String, String> nodeMap) throws SerializedException;
	String deleteNode(String sourcePath) throws SerializedException;
	String saveNodeDetails(String sourcePath, JcrNode jcrNode) throws SerializedException;
	String addNewProperty(String sourcePath, String name, String value) throws SerializedException;
	String deleteProperty(String sourcePath, String name) throws SerializedException;
	String saveProperties(String sourcePath, JcrNode jcrNode) throws SerializedException;
	String savePropertyStringValue(String sourcePath, String property, String value) throws SerializedException;
	//String savePropertyBinaryValue(String sourcePath, String property, InputStream value) throws SerializedException;
	List<String> fullTextSearch(String query) throws SerializedException;
	List<String> xpathSearch(String query) throws SerializedException;
	List<String> sqlSearch(String query) throws SerializedException;
	Boolean addNodeTypes(String cnd) throws SerializedException;
	List<Map<String, String>> getNodeTypeIcons() throws SerializedException;
	String getBrowsableContentFilterRegex() throws SerializedException;
	List<RemoteFile> getPossibleIconPaths(String path) throws SerializedException;
	Boolean changeNodeTypeIconAssociation(String nodeType, String iconPath) throws SerializedException;
}
