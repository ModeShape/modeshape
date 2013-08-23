package org.modeshape.explorer.client;

/**
 * @author chrisjennings
 * Values to be read from properties file for connection details
 * 
 */
public enum ConnectionProperties {
	DEFAULT_WORKSPACE("defaultWorkspace"),
	DEFAULT_USERNAME("defaultUsername"),
	DEFAULT_PASSWORD("defaultPassword"),
	SUPPORT_LOCAL("supportLocal"),
	DEFAULT_LOCAL_FILE("configFilePathTxtDefault"),
	DEFAULT_LOCAL_DIR("homeDirPathTxtDefault"),
	SUPPORT_JNDI("supportJndi"),
	DEFAULT_JNDI_NAME("jndiNameTxtDefault"),
	DEFAULT_JNDI_CONTEXT("jndiContextTxtDefault"),
	SUPPORT_RMI("supportRmi"),
	DEFAULT_RMI_URL("rmiUrlTxtDefault");

	public final String key;

	ConnectionProperties(String key) {
		this.key = key;
	}
}
