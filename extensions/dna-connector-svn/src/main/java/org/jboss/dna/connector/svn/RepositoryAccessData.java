/**
 * 
 */
package org.jboss.dna.connector.svn;

import net.jcip.annotations.ThreadSafe;

@ThreadSafe
public class RepositoryAccessData {

    
    private String repositoryRootUrl;
    private String username;
    private String password;

    /**
     * @param password
     * @param username
     * @param repositoryRootUrl
     */
    public RepositoryAccessData( String repositoryRootUrl,
                           String username,
                           String password ) {
        this.repositoryRootUrl = repositoryRootUrl;
        this.username = username;
        this.password = password;
    }

    /**
     * @return the repositoryRootUrl
     */
    public String getRepositoryRootUrl() {
        return repositoryRootUrl;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }
}
