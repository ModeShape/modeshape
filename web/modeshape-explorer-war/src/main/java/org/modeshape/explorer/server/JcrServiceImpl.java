package org.modeshape.explorer.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.activation.MimetypesFileTypeMap;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
//import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeIterator;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.naming.Context;
import javax.naming.InitialContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
//import org.apache.jackrabbit.core.TransientRepository;
//import org.apache.jackrabbit.rmi.repository.URLRemoteRepository;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.modeshape.explorer.client.ConnectionProperties;
import org.modeshape.explorer.client.JcrService;
import org.modeshape.explorer.client.SerializedException;
import org.modeshape.explorer.client.domain.JcrNode;
import org.modeshape.explorer.client.domain.LoginDetails;
import org.modeshape.explorer.client.domain.RemoteFile;
import org.modeshape.jcr.JcrRepository;

/**
 *
 * @author James Pickup
 *
 */
public class JcrServiceImpl extends RemoteServiceServlet implements JcrService {

    private static final long serialVersionUID = 8840001785942628602L;
    private Log log = LogFactory.getLog(this.getClass());
    private final static String FULL_TEXT_SEARCH = "fullTextSearch";
    private final static String XPATH_SEARCH = "xpathSearch";
    private final static String SQL_SEARCH = "sqlSearch";
    private final static String TEMP_FILES = "temp_files/";
    private static String REAL_ABSOLUTE_PATH = "";
    private static final String DEFAULT_ICON_PATH_PREFIX = "images";
    private static final String DEFAULT_ICON_PATH = "customicons";
    private static final String NODETYPE_ICONS_PROPERTIES_FILE = "nodeTypeIcons.properties";
    private static final String CONNECTIONS_PROPERTIES_FILE = "connectionTypes.properties";

    protected Session getJcrSession() throws Exception {
        if (null == getThreadLocalRequest().getSession().getAttribute("session")) {
            throw new Exception("Session has timed out. Please refresh your browser.");
        }
        return (Session) getThreadLocalRequest().getSession().getAttribute("session");
    }

    public JcrServiceImpl() {
        super();
    }

    private void cleanAllTempFiles() {
        REAL_ABSOLUTE_PATH = getServletContext().getRealPath("/");
        deleteDirectory(new File(REAL_ABSOLUTE_PATH + TEMP_FILES));
        new File(REAL_ABSOLUTE_PATH + TEMP_FILES).mkdir();
    }

    /**
     * Delete all files and sub directories
     *
     * @param path
     * @return boolean. True for success
     */
    protected boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i]);
                } else {
                    files[i].delete();
                }
            }
        }
        return (path.delete());
    }

    /**
     * Get the default login details from the parameters in the web.xml
     */
    public LoginDetails getDefaultLoginDetails() throws SerializedException {
        LoginDetails loginDetails;
        try {
            Map<String, String> connectionProperties = getConnectionProperties();
            loginDetails = new LoginDetails();
            loginDetails.setSupportsLocalRepository(Boolean.parseBoolean(connectionProperties.get(ConnectionProperties.SUPPORT_LOCAL.key)));
            loginDetails.setHomeDirPath(connectionProperties.get(ConnectionProperties.DEFAULT_LOCAL_DIR.key));
            loginDetails.setConfigFilePath(connectionProperties.get(ConnectionProperties.DEFAULT_LOCAL_FILE.key));
            loginDetails.setSupportsJndiRepository(Boolean.parseBoolean(connectionProperties.get(ConnectionProperties.SUPPORT_JNDI.key)));
            loginDetails.setJndiContext(connectionProperties.get(ConnectionProperties.DEFAULT_JNDI_CONTEXT.key));
            loginDetails.setJndiName(connectionProperties.get(ConnectionProperties.DEFAULT_JNDI_NAME.key));
            loginDetails.setSupportsRmiRepository(Boolean.parseBoolean(connectionProperties.get(ConnectionProperties.SUPPORT_RMI.key)));
            loginDetails.setRmiUrl(connectionProperties.get(ConnectionProperties.DEFAULT_RMI_URL.key));
            loginDetails.setWorkSpace(connectionProperties.get(ConnectionProperties.DEFAULT_WORKSPACE.key));
            loginDetails.setUserName(connectionProperties.get(ConnectionProperties.DEFAULT_USERNAME.key));
            loginDetails.setPassword(connectionProperties.get(ConnectionProperties.DEFAULT_PASSWORD.key));
            getNodeTypeIcons();
        } catch (Exception e) {
            log.info("Failed fetching default login details fron web descriptor. " + e.getMessage());
            throw new SerializedException(e.getMessage());
        }
        return loginDetails;
    }

    public String getBrowsableContentFilterRegex() throws SerializedException {
        return getServletContext().getInitParameter("browsableContentFilterRegex");
    }

    /**
     *
     * @param configFilePath
     * @param homeDirPath
     * @param workSpace
     * @param userName
     * @param password
     * @return JCR Session
     * @throws SerializedException
     */
    public Session getNewLocalSession(String configFilePath, String homeDirPath, String workSpace, String userName, String password) throws Exception {
        /*		try
         {
         Repository repository = new TransientRepository(configFilePath, homeDirPath);
         SimpleCredentials creds = new SimpleCredentials(userName, password
         .toCharArray());
         return repository.login(creds, workSpace);
         }
         catch (Exception e)
         {
         log.info("Failed Login. " + e.getMessage());
         throw new SerializedException(e.getMessage());
         }
         */
        return null;
    }

    /**
     *
     * @param jndiName
     * @param jndiContext
     * @param workSpace
     * @param userName
     * @param password
     * @return JCR Session
     * @throws SerializedException
     */
    public Session getNewSessionViaJndi(String jndiName, String jndiContext, String workSpace, String userName, String password) throws SerializedException {
        try {
            InitialContext context = new InitialContext();
            log.info("Gettinf repository: " + jndiName);
            JcrRepository repository = (JcrRepository) context.lookup(jndiName);
            
            log.info("Got repository: " + repository);
            SimpleCredentials creds = new SimpleCredentials(userName, password.toCharArray());
//            return repository.login(creds, workSpace);
            return repository.login(workSpace);
        } catch (Exception e) {
            log.info("Failed Login. " + e.getMessage());
            throw new SerializedException(e.getMessage());
        }
    }

    /**
     *
     * @param rmiUrl
     * @param workSpace
     * @param userName
     * @param password
     * @return JCR Session
     * @throws SerializedException
     */
    public Session getNewSessionViaRmi(String rmiUrl, String workSpace, String userName, String password) throws SerializedException {
        /*			try {
         Repository repository = new URLRemoteRepository(rmiUrl);
         SimpleCredentials creds = new SimpleCredentials(userName, password.toCharArray());
         return repository.login(creds, workSpace);
         } catch (Exception e) {
         log.info("Failed Login. " + e.getMessage());
         throw new SerializedException(e.getMessage());
         }
         */
        return null;
    }

//	/**
//	 *  Login to repository and store session as HTTP session attribute
//	 */
//	public Boolean login(String rmiUrl, String workSpace, String userName, String password) throws SerializedException {
//		cleanAllTempFiles();
//		try {
//			getThreadLocalRequest().getSession().setAttribute("session", getNewSession(rmiUrl, workSpace, userName, password));
//			
//		} catch (Exception e) {
//			log.info("Failed Login. " + e.getMessage());
//			throw new SerializedException(e.getMessage());
//		}
//		return true;
//	}
    public Boolean loginLocal(String configFilePath, String homeDirPath, String workSpace, String userName,
            String password) throws Exception {
        try {
            getThreadLocalRequest().getSession().setAttribute("session",
                    getNewLocalSession(configFilePath, homeDirPath, workSpace, userName, password));
        } catch (Exception e) {
            log.info("Failed Login. " + e.getMessage());
            throw new Exception(e.getMessage());
        }
        return true;
    }

    public Boolean loginViaJndi(String jndiName, String jndiContext, String workSpace, String userName,
            String password) throws Exception {
        try {
            getThreadLocalRequest().getSession().setAttribute("session",
                    getNewSessionViaJndi(jndiName, jndiContext, workSpace, userName, password));
        } catch (Exception e) {
            log.info("Failed Login. " + e.getMessage());
            throw new Exception(e.getMessage());
        }
        return true;
    }

    public Boolean loginViaRmi(String rmiUrl, String workSpace, String userName,
            String password) throws Exception {
        try {
            getThreadLocalRequest().getSession().setAttribute("session",
                    getNewSessionViaRmi(rmiUrl, workSpace, userName, password));
        } catch (Exception e) {
            log.info("Failed Login. " + e.getMessage());
            throw new Exception(e.getMessage());
        }
        return true;
    }

    /**
     * Retrieve the node type icon mappings from properties file
     */
    public List<Map<String, String>> getNodeTypeIcons() throws SerializedException {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(getServletContext().getRealPath("/WEB-INF") + File.separator + NODETYPE_ICONS_PROPERTIES_FILE));
        } catch (FileNotFoundException e) {
            log.error("Unable to find " + NODETYPE_ICONS_PROPERTIES_FILE, e);
            throw new SerializedException(e.getMessage());
        } catch (IOException e) {
            log.error("Error reading " + NODETYPE_ICONS_PROPERTIES_FILE, e);
            throw new SerializedException(e.getMessage());
        }
        List<Map<String, String>> returnList = new ArrayList<Map<String, String>>();
        Map<String, String> tempMap;
        for (Iterator<Map.Entry<Object, Object>> iterator = properties.entrySet().iterator(); iterator.hasNext();) {
            Map.Entry<Object, Object> propertyPair = iterator.next();
            tempMap = new HashMap<String, String>();
            tempMap.put(propertyPair.getKey().toString(), propertyPair.getValue().toString());
            returnList.add(tempMap);
        }

        return returnList;
    }

    /**
     * Retrieve the supported connection types and default values from
     * properties file
     */
    private Map<String, String> getConnectionProperties() throws Exception {
        Properties properties = new Properties();
        try {
            properties.load(new FileInputStream(getServletContext().getRealPath("/WEB-INF") + File.separator + CONNECTIONS_PROPERTIES_FILE));
        } catch (FileNotFoundException e) {
            log.error("Unable to find " + CONNECTIONS_PROPERTIES_FILE, e);
            throw new SerializedException(e.getMessage());
        } catch (IOException e) {
            log.error("Error reading " + CONNECTIONS_PROPERTIES_FILE, e);
            throw new SerializedException(e.getMessage());
        }
        Map<String, String> propertiesMap = new HashMap<String, String>();
        for (ConnectionProperties property : ConnectionProperties.values()) {
            log.debug(property.key + " = " + properties.getProperty(property.key, ""));
            propertiesMap.put(property.key, properties.getProperty(property.key, ""));
        }
        return propertiesMap;
    }

    /*
     * Method used to navigate to a specific path, used for the 'go' browse button and the
     * search when clicking on a search result
     * @see com.priocept.client.JcrService#getNodeTree(java.lang.String)
     */
    public List<Map<String, List<JcrNode>>> getNodeTree(String path) throws SerializedException {
        List<Map<String, List<JcrNode>>> returnList;
        try {
            if (null == path || path.equals("")) {
                path = "/";
            }
            String[] pathSplit = path.split("/");
            returnList = new ArrayList<Map<String, List<JcrNode>>>();
            Map<String, List<JcrNode>> treeAssociationMap = null;
            //iterate over each path element to gather and return the nodes at each level to be mapped to the tree on the front end
            StringBuffer pathBuilder = new StringBuffer();
            for (int i = 0; i < pathSplit.length; i++) {
                treeAssociationMap = new HashMap<String, List<JcrNode>>();
                pathBuilder.append(pathSplit[i].trim() + "/");
                treeAssociationMap.put(pathBuilder.toString(), getNode(pathBuilder.toString()));
                returnList.add(treeAssociationMap);
            }
        } catch (Exception e) {
            log.info("Failed fetching Node Tree. " + e.getMessage());
            throw new SerializedException(e.getMessage());
        }

        return returnList;
    }

    /*
     * Used to gather a list of the children of the node at the given path (parent node)
     * @see com.priocept.client.JcrService#getNode(java.lang.String)
     */
    public List<JcrNode> getNode(String path) throws SerializedException {
        List<JcrNode> jcrNodeList;
        try {
            if (null == path || path.equals("")) {
                path = "/";
            }
            Item item = null;
            JcrNode newJcrNode = null;
            NodeIterator nodeIterator = null;
            PropertyIterator propertyIterator = null;
            Map<String, String> properties;
            jcrNodeList = new ArrayList<JcrNode>();
            item = getJcrSession().getItem(path);
            if (null == item && !(item instanceof Node)) {
                return null;
            }
            Node pathNode = (Node) item;
            nodeIterator = pathNode.getNodes();
            for (int i = 0; i < nodeIterator.getSize(); i++) {
                Node node = nodeIterator.nextNode();
                propertyIterator = node.getProperties();
                properties = new HashMap<String, String>();
                for (int j = 0; j < propertyIterator.getSize(); j++) {
                    Property property = propertyIterator.nextProperty();
                    //added a Binary Value text to the jcr:data property, as displaying raw binary in the value cell isn't desirable
                    if (null != property.getName() && property.getName().equalsIgnoreCase("jcr:data")) {
                        properties.put(property.getName() + " (Click to open)", "Binary Value");

                        //Any property of value which starts with http:// will be openable in the frontend 
                    } else if (null != property.getValue() && property.getValue().getString().startsWith("http://")) {
                        properties.put(property.getName() + " (Click to open)", property.getValue().getString());
                    } else {
                        properties.put(property.getName(), property.getValue().getString());
                    }
                }
                if (node.getPath().toString().contains("[") && node.getPath().toString().contains("]")) {
                    String nameWithBrackets = node.getPath().toString().substring(node.getPath().toString().lastIndexOf('/') + 1, node.getPath().toString().length());
                    newJcrNode = new JcrNode(nameWithBrackets, node.getPath().toString(), node.getPrimaryNodeType()
                            .getName(), properties);
                } else {
                    newJcrNode = new JcrNode(node.getName().toString(), node.getPath().toString(), node.getPrimaryNodeType()
                            .getName(), properties);
                }

                jcrNodeList.add(newJcrNode);
            }
        } catch (Exception e) {
            log.info("Failed fetching Node. " + e.getMessage());
            throw new SerializedException("Failed fetching Node. " + e.getMessage());
        }

        return jcrNodeList;
    }

    public List<String> getAvailableNodeTypes() throws SerializedException {
        List<String> availableNodeTypeList;
        try {
            availableNodeTypeList = new ArrayList<String>();
            NodeTypeIterator nodeTypeIterator = getJcrSession().getWorkspace().getNodeTypeManager().getAllNodeTypes();
            for (int j = 0; j < nodeTypeIterator.getSize(); j++) {
                NodeType nodeType = nodeTypeIterator.nextNodeType();
                availableNodeTypeList.add(nodeType.getName());
            }
        } catch (Exception e) {
            log.info("Failed fetching available node types. " + e.getMessage());
            throw new SerializedException(e.getMessage());
        }
        return availableNodeTypeList;
    }

    /**
     * Add new node and add mandatory jcr:content child node if the node type is
     * a file type
     */
    public String addNewNode(String path, String newNodeName, String primaryNodeType, String jcrContentFileName, boolean cancel) throws SerializedException {
        if (cancel) {
            deleteDirectory(new File(REAL_ABSOLUTE_PATH + TEMP_FILES + getThreadLocalRequest().getSession().getId()));
            return "Removed files";
        }
        if (null == path || path.equals("") || null == primaryNodeType || primaryNodeType.equals("")) {
            throw new SerializedException("New node not added.");
            //return "New node not added.";
        }
        Node pathNode;
        try {
            Item item = null;
            item = getJcrSession().getItem(path);

            if (null == item && !(item instanceof Node)) {
                return null;
            }
            pathNode = (Node) item;
            Node newNode = pathNode.addNode(newNodeName, primaryNodeType);
            if (primaryNodeType.contains("file") || primaryNodeType.contains("File")) {
                Node resNode = newNode.addNode("jcr:content", "nt:resource");

                //Fixes: http://code.google.com/p/jackrabbitexplorer/issues/detail?id=19
                //Must ascertain MIME type.
                MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
                String mimeType = mimeTypesMap.getContentType(jcrContentFileName);

                resNode.setProperty("jcr:mimeType", mimeType);
                resNode.setProperty("jcr:encoding", "");
                resNode.setProperty("jcr:data", new FileInputStream(REAL_ABSOLUTE_PATH + TEMP_FILES
                        + getThreadLocalRequest().getSession().getId() + "/" + jcrContentFileName));
            }

            getJcrSession().save();
        } catch (Exception e) {
            log.info("Node not added. " + e.getMessage());
            throw new SerializedException("Node not added. " + e.getMessage());
        } finally {
            if (null != getThreadLocalRequest()) {
                deleteDirectory(new File(REAL_ABSOLUTE_PATH + TEMP_FILES + getThreadLocalRequest().getSession().getId()));
            }
        }

        return "New node successfully created.";
    }

    public String moveNode(String sourcePath, String destinationPath) throws SerializedException {
        String sourceName;
        try {
            if (null == sourcePath || sourcePath.equals("") || null == destinationPath || destinationPath.equals("")) {
                throw new Exception("Node not moved.");
            }
            int lastIndexOfSlash = sourcePath.lastIndexOf('/');
            sourceName = sourcePath.substring(lastIndexOfSlash + 1, sourcePath.length());
            if (sourceName.indexOf('[') >= 0) {
                sourceName = sourceName.substring(0, sourceName.indexOf('['));
            }
            if (destinationPath.equals("/")) {
                getJcrSession().move(sourcePath, destinationPath + sourceName);
            } else {
                getJcrSession().move(sourcePath, destinationPath + "/" + sourceName);
            }
            getJcrSession().save();
        } catch (Exception e) {
            log.info("Node Not Moved. " + e.getMessage());
            throw new SerializedException("Node Not Moved. " + e.getMessage());
        }

        return "Successfully moved. " + sourcePath + " to " + destinationPath;
    }

    public String renameNode(String sourcePath, String newName) throws SerializedException {
        String oldName;
        String newPath;
        try {
            if (null == sourcePath || sourcePath.equals("") || null == newName || newName.equals("")) {
                throw new Exception("Node not renamed.");
            }
            int lastIndexOfSlash = sourcePath.lastIndexOf('/');
            oldName = sourcePath.substring(lastIndexOfSlash + 1, sourcePath.length());
            newPath = sourcePath.substring(0, lastIndexOfSlash + 1) + newName;
            getJcrSession().move(sourcePath, newPath);
            getJcrSession().save();
        } catch (Exception e) {
            log.info("Node Not Renamed. " + e.getMessage());
            throw new SerializedException("Node Not Renamed. " + e.getMessage());
        }

        return "Successfully renamed from " + oldName + " to " + newName;
    }

    public String moveNodes(Map<String, String> nodeMap) throws SerializedException {
        // if (null == sourcePath || sourcePath.equals("") ||
        // null == destinationPath || destinationPath.equals("")) {
        // return null;
        // }
        // int lastIndexOfSlash = sourcePath.lastIndexOf('/');
        // String sourceName = sourcePath.substring(lastIndexOfSlash + 1,
        // sourcePath.length());
        // try {
        // getJcrSession().move(sourcePath, destinationPath + "/" + sourceName);
        // getJcrSession().save();
        // } catch (Exception e) {
        // return "Node Not Moved. " + e.getMessage();
        // }
        //		
        // return "Successfully Moved. " + sourcePath + " to " +
        // destinationPath;
        return null;
    }

    public String cutAndPasteNode(String sourcePath, String destinationPath) throws SerializedException {
        try {
            if (null == sourcePath || sourcePath.equals("") || null == destinationPath || destinationPath.equals("")) {
                throw new Exception("Node not cut.");
            }
            copyNode(sourcePath, destinationPath);
            deleteNode(sourcePath);
        } catch (Exception e) {
            log.info("Node not cut. " + e.getMessage());
            throw new SerializedException("Node not cut. " + e.getMessage());
        }

        return "Successfully cut and pasted " + sourcePath + " to " + destinationPath;
    }

    public String copyNode(String sourcePath, String destinationPath) throws SerializedException {
        String sourceName;
        try {
            if (null == sourcePath || sourcePath.equals("") || null == destinationPath || destinationPath.equals("")) {
                throw new Exception("Node not copied.");
            }
            int lastIndexOfSlash = sourcePath.lastIndexOf('/');
            sourceName = sourcePath.substring(lastIndexOfSlash + 1, sourcePath.length());
            if (sourceName.indexOf('[') >= 0) {
                sourceName = sourceName.substring(0, sourceName.indexOf('['));
            }
            if (destinationPath.equals("/")) {
                getJcrSession().getWorkspace().copy(sourcePath, destinationPath + sourceName);
            } else {
                getJcrSession().getWorkspace().copy(sourcePath, destinationPath + "/" + sourceName);
            }
            getJcrSession().save();
        } catch (Exception e) {
            log.info("Node not copied. " + e.getMessage());
            throw new SerializedException("Node not copied. " + e.getMessage());
        }

        return "Successfully copied " + sourcePath + " to " + destinationPath;
    }

    public String copyNodes(Map<String, String> nodeMap) throws SerializedException {
        // if (null == sourcePath || sourcePath.equals("") ||
        // null == destinationPath || destinationPath.equals("")) {
        // return null;
        // }
        // int lastIndexOfSlash = sourcePath.lastIndexOf('/');
        // String sourceName = sourcePath.substring(lastIndexOfSlash + 1,
        // sourcePath.length());
        // try {
        // getJcrSession().getWorkspace().copy(sourcePath, destinationPath + "/"
        // +
        // sourceName);
        // getJcrSession().save();
        // } catch (Exception e) {
        // return "Node Not Copied. " + e.getMessage();
        // }
        //		
        // return "Successfully Copied. " + sourcePath + " to " +
        // destinationPath;
        return null;
    }

    public String deleteNode(String sourcePath) throws SerializedException {
        if (null == sourcePath || sourcePath.equals("")) {
            throw new SerializedException("Node source missing");
        }
        try {
            Item item = getJcrSession().getItem(sourcePath);
            item.remove();
            getJcrSession().save();
        } catch (Exception e) {
            log.info("Node not deleted. " + e.getMessage());
            throw new SerializedException("Node not deleted. " + e.getMessage());
        }

        return "Successfully deleted. " + sourcePath;
    }

    //not used
    public String saveNodeDetails(String sourcePath, JcrNode jcrNode) throws SerializedException {
//		if (null == jcrNode) {
//			throw new SerializedException("Details not saved.");
//		}
//		try {
//			Item item = getJcrSession().getItem(sourcePath);
//			if (null == item && !(item instanceof Node)) {
//				return null;
//			}
//			Node pathNode = (Node) item;
//			pathNode.setProperty("name", jcrNode.getName());
//			pathNode.setProperty("path", jcrNode.getPath());
//			pathNode.setProperty("primaryNodeType", jcrNode.getPrimaryNodeType());
//			getJcrSession().save();
//		} catch (Exception e) {
//			log.info("Node details not saved. " + e.getMessage());
//			throw new SerializedException("Node details not saved. " + e.getMessage());
//		}
//
//		return "Successfully saved. " + sourcePath;
        return "";
    }

    // Properties
    public String addNewProperty(String sourcePath, String name, String value) throws SerializedException {
        if (null == sourcePath || sourcePath.equals("")) {
            throw new SerializedException("Property not added.");
        }
        try {
            Item item = getJcrSession().getItem(sourcePath);
            if (null == item && !(item instanceof Node)) {
                return null;
            }
            Node pathNode = (Node) item;
            pathNode.setProperty(name, value);

            getJcrSession().save();
        } catch (Exception e) {
            log.info("Property not added. " + e.getMessage());
            throw new SerializedException("Property not added. " + e.getMessage());
        }

        return "Successfully added new property at " + sourcePath;
    }

    public String deleteProperty(String sourcePath, String name) throws SerializedException {
        if (null == sourcePath || sourcePath.equals("") || null == name || name.equals("")) {
            throw new SerializedException("Property not deleted.");
        }
        try {
            Item item = getJcrSession().getItem(sourcePath);
            if (null == item && !(item instanceof Node)) {
                return null;
            }
            Node pathNode = (Node) item;
            pathNode.getProperty(name).remove();

            getJcrSession().save();
        } catch (Exception e) {
            log.info("Property not deleted. " + e.getMessage());
            throw new SerializedException("Property not deleted. " + e.getMessage());
        }

        return "Successfully deleted " + name + " property at " + sourcePath;
    }

    public String saveProperties(String sourcePath, JcrNode jcrNode) throws SerializedException {
        if (null == jcrNode) {
            throw new SerializedException("Properties not saved.");
        }
        try {
            Item item = getJcrSession().getItem(sourcePath);
            if (null == item && !(item instanceof Node)) {
                return null;
            }
            Node pathNode = (Node) item;
            for (Iterator<Map.Entry<String, String>> iterator = jcrNode.getProperties().entrySet().iterator(); iterator.hasNext();) {
                Map.Entry<String, String> propertyPair = iterator.next();
                pathNode.setProperty(propertyPair.getKey(), propertyPair.getValue());
            }

            getJcrSession().save();
        } catch (Exception e) {
            log.info("Properties not saved. " + e.getMessage());
            throw new SerializedException("Properties not saved. " + e.getMessage());
        }

        return "Successfully saved. " + sourcePath;
    }

    public String savePropertyStringValue(String sourcePath, String property, String value) throws SerializedException {
        if (null == sourcePath || null == property || null == value) {
            throw new SerializedException("Property not saved.");
        }
        try {
            Item item = getJcrSession().getItem(sourcePath);
            if (null == item && !(item instanceof Node)) {
                return null;
            }
            Node pathNode = (Node) item;
            pathNode.setProperty(property, (String) value);

            getJcrSession().save();
        } catch (Exception e) {
            log.info("Property value not saved. " + e.getMessage());
            throw new SerializedException("Property not saved. " + e.getMessage());
        }

        return "Successfully saved property " + sourcePath;
    }

    /**
     *
     * @param sourcePath
     * @param property
     * @param value
     * @return String success message
     * @throws SerializedException
     */
    public String savePropertyBinaryValue(String sourcePath, String property, InputStream inputStream) throws SerializedException {
        if (null == sourcePath || null == property || null == inputStream) {
            throw new SerializedException("Property not saved.");
        }
        try {
            Item item = getJcrSession().getItem(sourcePath);
            if (null == item && !(item instanceof Node)) {
                return null;
            }
            Node pathNode = (Node) item;
            pathNode.setProperty(property, (InputStream) inputStream);

            getJcrSession().save();
        } catch (Exception e) {
            log.info("Binary Property not saved. " + e.getMessage());
            throw new SerializedException("Property not saved. " + e.getMessage());
        }

        return "Successfully saved. " + sourcePath;
    }

    /*
     * Search(non-Javadoc)
     * @see com.priocept.jcr.client.JcrService#fullTextSearch(java.lang.String)
     */
    public List<String> fullTextSearch(String queryString) throws SerializedException {
        return searchHelper(queryString, FULL_TEXT_SEARCH);
    }

    public List<String> xpathSearch(String queryString) throws SerializedException {
        return searchHelper(queryString, XPATH_SEARCH);
    }

    public List<String> sqlSearch(String queryString) throws SerializedException {
        return searchHelper(queryString, SQL_SEARCH);
    }

    /**
     *
     * @param queryString
     * @param searchType
     * @return Search Results as String in a List
     * @throws SerializedException
     */
    protected List<String> searchHelper(String queryString, String searchType) throws SerializedException {
        if (null == queryString || null == searchType) {
            return null;
        }
        List<String> results = new ArrayList<String>();
        try {
            QueryManager qm = getJcrSession().getWorkspace().getQueryManager();
            Query query = null;
            if (searchType.equals(FULL_TEXT_SEARCH)) {
                queryString = "//*[jcr:contains(., '" + queryString + "')]";
                query = qm.createQuery(queryString, Query.XPATH);
            } else if (searchType.equals(XPATH_SEARCH)) {
                query = qm.createQuery(queryString, Query.XPATH);
            } else if (searchType.equals(SQL_SEARCH)) {
                query = qm.createQuery(queryString, Query.SQL);
            }
            QueryResult queryResult = query.execute();
            NodeIterator resultIterator = queryResult.getNodes();
            while (resultIterator.hasNext()) {
                Node node = resultIterator.nextNode();
                results.add(node.getPath());
            }
        } catch (Exception e) {
            log.info("Search Failed. queryString='" + queryString + "' searchType='" + searchType + "' " + e.getMessage());
            throw new SerializedException(e.getMessage());
        }
        return results;
    }

    /*
     * Management of node types(non-Javadoc)
     * @see com.priocept.jcr.client.JcrService#addNodeTypes(java.lang.String)
     */
//	@SuppressWarnings("deprecation")
    public Boolean addNodeTypes(String cnd) throws SerializedException {
        return false;

        //TODO Fix registering custom node types
		/*
         * Can't be done over RMI at this stage
         * Cannot cast ClientNodeTypeManager to JackrabbitNodeTypeManager or NodeTypeManagerImpl
         * 
         * Also, JCR2.0 javax.jcr.nodetype.NodeTypeManager.registerNodeType is unimplemented 
         * i.e. throws javax.jcr.UnsupportedRepositoryOperationException: TODO: JCRRMI-26
         * 
         * Is possible to add custom node types using a TransientRepository or through JNDI
         */
        /*
         JackrabbitNodeTypeManager manager = null;
         try {
         manager =  (JackrabbitNodeTypeManager) getJcrSession().getWorkspace().getNodeTypeManager();
         } catch (RepositoryException e) {
         log.info("Failed to register node types" + e.getMessage());
         throw new SerializedException(e.getMessage());
         } catch (Exception e) {
         e.printStackTrace();
         log.info("Failed to register node types" + e.getMessage());
         throw new SerializedException(e.getMessage());
         }
		
         //Convert cnd String to InputStream
         InputStream is = null;
         try {
         is = new ByteArrayInputStream(cnd.getBytes("UTF-8"));
         } catch (UnsupportedEncodingException e) {
         log.info("Failed to register node types" + e.getMessage());
         throw new SerializedException(e.getMessage());
         }
		
         try {
         manager.registerNodeTypes(is, JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
         } catch (IOException e) {
         log.info("Failed to register node types" + e.getMessage());
         throw new SerializedException(e.getMessage());
         } catch (RepositoryException e) {
         log.info("Failed to register node types" + e.getMessage());
         throw new SerializedException(e.getMessage());
         }
		
         return true;
         */
    }

    @Override
    public List<RemoteFile> getPossibleIconPaths(String path) throws SerializedException {
        if (path == null) {
            path = DEFAULT_ICON_PATH;
        }
        String rootOfPath = getServletContext().getRealPath("/");
        File file = new File(rootOfPath + DEFAULT_ICON_PATH_PREFIX + File.separator + DEFAULT_ICON_PATH);
        List<RemoteFile> children = new ArrayList<RemoteFile>();

        //SmartGWT prefixes all generated image paths with "images/" so we must remove this part of the path.
        int charactersToRemove = new String(rootOfPath + DEFAULT_ICON_PATH_PREFIX + File.separator).length();

        for (File child : file.listFiles()) {
            if (!child.isDirectory()) {
                String fullPath = child.getPath();
                String relativePath = fullPath.substring(charactersToRemove);
                RemoteFile remoteFile = new RemoteFile(relativePath, child.isDirectory());
                children.add(remoteFile);
            }
        }
        return children;
    }

    @Override
    public Boolean changeNodeTypeIconAssociation(String nodeType, String iconPath) throws SerializedException {
        Properties properties = new Properties();
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            inputStream = new FileInputStream(getServletContext().getRealPath(File.separator + "WEB-INF") + File.separator + NODETYPE_ICONS_PROPERTIES_FILE);
            properties.load(inputStream);
            outputStream = new FileOutputStream(getServletContext().getRealPath(File.separator + "WEB-INF") + File.separator + NODETYPE_ICONS_PROPERTIES_FILE);

            //Correct iconpath to be a URL path
            iconPath = iconPath.replaceAll("\\\\", "\\/");

            properties.setProperty(nodeType, iconPath);
            properties.store(outputStream, null);
        } catch (FileNotFoundException e) {
            log.error("Unable to find " + NODETYPE_ICONS_PROPERTIES_FILE + " : ", e);
            throw new SerializedException(e.getMessage());
        } catch (IOException e) {
            log.error("Exception reading/writing " + NODETYPE_ICONS_PROPERTIES_FILE + " : ", e);
            throw new SerializedException(e.getMessage());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    log.error("Exception closing input stream from " + NODETYPE_ICONS_PROPERTIES_FILE, e);
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    log.error("Exception closing output stream to " + NODETYPE_ICONS_PROPERTIES_FILE, e);
                }
            }
        }
        return true;
    }
}
