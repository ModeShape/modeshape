package org.modeshape.explorer.client.domain;

import java.io.Serializable;

public class LoginDetails implements Serializable {
	private static final long serialVersionUID = 9709731178901836L;
	
	private boolean supportsLocalRepository;
	private boolean supportsJndiRepository;
	private boolean supportsRmiRepository;
	private String configFilePath;
	private String homeDirPath;
	private String jndiName;
	private String jndiContext;
	private String rmiUrl;
	private String workSpace;
	private String userName;
	private String password;

	public String getConfigFilePath() {
		return configFilePath;
	}
	public void setConfigFilePath(String configFilePath) {
		this.configFilePath = configFilePath;
	}
	public String getHomeDirPath() {
		return homeDirPath;
	}
	public void setHomeDirPath(String homeDirPath) {
		this.homeDirPath = homeDirPath;
	}
	public String getJndiName() {
		return jndiName;
	}
	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}
	public String getJndiContext() {
		return jndiContext;
	}
	public void setJndiContext(String jndiContext) {
		this.jndiContext = jndiContext;
	}
	public String getRmiUrl() {
		return rmiUrl;
	}
	public void setRmiUrl(String rmiUrl) {
		this.rmiUrl = rmiUrl;
	}
	public String getWorkSpace() {
		return workSpace;
	}
	public void setWorkSpace(String workSpace) {
		this.workSpace = workSpace;
	}
	public String getUserName() {
		return userName;
	}
	public void setUserName(String userName) {
		this.userName = userName;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public boolean isSupportsLocalRepository() {
		return supportsLocalRepository;
	}
	public void setSupportsLocalRepository(boolean supportsLocalRepository) {
		this.supportsLocalRepository = supportsLocalRepository;
	}
	public boolean isSupportsJndiRepository() {
		return supportsJndiRepository;
	}
	public void setSupportsJndiRepository(boolean supportsJndiRepository) {
		this.supportsJndiRepository = supportsJndiRepository;
	}
	public boolean isSupportsRmiRepository() {
		return supportsRmiRepository;
	}
	public void setSupportsRmiRepository(boolean supportsRmiRepository) {
		this.supportsRmiRepository = supportsRmiRepository;
	}
}
