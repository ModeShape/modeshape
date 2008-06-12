/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.repository.federation;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import javax.naming.Context;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.repository.RepositoryI18n;
import org.jboss.dna.spi.graph.connection.RepositoryConnection;
import org.jboss.dna.spi.graph.connection.RepositorySource;
import org.jboss.dna.spi.graph.connection.RepositorySourceException;

/**
 * @author Randall Hauch
 */
@ThreadSafe
public class FederatedRepositorySource implements RepositorySource {

    /**
     */
    private static final long serialVersionUID = 7587346948013486977L;

    public static final int DEFAULT_RETRY_LIMIT = 0;

    protected static final String USERNAME = "username";
    protected static final String CREDENTIALS = "credentials";
    protected static final String SOURCE_NAME = "sourceName";
    protected static final String REPOSITORY_NAME = "repositoryName";
    protected static final String RETRY_LIMIT = "retryLimit";
    protected static final String FEDERATION_SERVICE_JNDI_NAME = "fedServiceJndiName";

    private final String repositoryName;
    private final FederationService federationService;
    private String sourceName;
    private int retryLimit;
    private String username;
    private String credentials;

    protected FederatedRepositorySource( FederationService federationService,
                                         String repositoryName ) {
        ArgCheck.isNotNull(federationService, "federationService");
        ArgCheck.isNotNull(repositoryName, "repositoryName");
        this.federationService = federationService;
        this.repositoryName = repositoryName;
        this.retryLimit = DEFAULT_RETRY_LIMIT;
    }

    /**
     * @return federationService
     */
    public FederationService getFederationService() {
        return this.federationService;
    }

    /**
     * {@inheritDoc}
     */
    public int getRetryLimit() {
        return this.retryLimit;
    }

    /**
     * {@inheritDoc}
     */
    public void setRetryLimit( int limit ) {
        this.retryLimit = limit > 0 ? limit : 0;
    }

    /**
     * {@inheritDoc}
     */
    public RepositoryConnection getConnection() throws RepositorySourceException {
        // Find the repository ...
        FederatedRepository repository = federationService.getRepository(this.repositoryName);
        if (repository == null) {
            throw new RepositorySourceException(
                                                RepositoryI18n.unableToCreateConnectionToFederatedRepository.text(this.repositoryName));
        }
        // Authenticate the user ...
        String username = this.username;
        Object credentials = this.credentials;
        if (!repository.authenticate(username, credentials)) {
            throw new RepositorySourceException(
                                                RepositoryI18n.unableToAuthenticateConnectionToFederatedRepository.text(this.repositoryName,
                                                                                                                        username));
        }
        // Return the new connection ...
        return new FederatedRepositoryConnection(repository, this);
    }

    /**
     * {@inheritDoc}
     */
    public String getName() {
        return sourceName;
    }

    /**
     * @param sourceName the name of this repository source
     */
    public void setName( String sourceName ) {
        this.sourceName = sourceName;
    }

    /**
     * @return username
     */
    public String getUsername() {
        return this.username;
    }

    /**
     * @param username Sets username to the specified value.
     */
    public void setUsername( String username ) {
        this.username = username;
    }

    /**
     * @return credentials
     */
    public String getCredentials() {
        return this.credentials;
    }

    /**
     * @param credentials Sets credentials to the specified value.
     */
    public void setCredentials( String credentials ) {
        this.credentials = credentials;
    }

    /**
     * @return repositoryName
     */
    public String getRepositoryName() {
        return this.repositoryName;
    }

    /**
     * {@inheritDoc}
     */
    public Reference getReference() {
        String className = getClass().getName();
        String factoryClassName = NamingContextObjectFactory.class.getName();
        Reference ref = new Reference(className, factoryClassName, null);

        ref.add(new StringRefAddr(USERNAME, this.getUsername()));
        ref.add(new StringRefAddr(CREDENTIALS, this.getCredentials()));
        ref.add(new StringRefAddr(SOURCE_NAME, this.sourceName));
        ref.add(new StringRefAddr(REPOSITORY_NAME, this.repositoryName));
        ref.add(new StringRefAddr(FEDERATION_SERVICE_JNDI_NAME, this.federationService.getJndiName()));
        ref.add(new StringRefAddr(RETRY_LIMIT, Integer.toString(getRetryLimit())));
        return ref;
    }

    public static class NamingContextObjectFactory implements ObjectFactory {

        public NamingContextObjectFactory() {
        }

        /**
         * {@inheritDoc}
         */
        public Object getObjectInstance( Object obj,
                                         javax.naming.Name name,
                                         Context nameCtx,
                                         Hashtable<?, ?> environment ) throws Exception {
            if (obj instanceof Reference) {
                Map<String, String> values = new HashMap<String, String>();
                Reference ref = (Reference)obj;
                Enumeration<?> en = ref.getAll();
                while (en.hasMoreElements()) {
                    RefAddr subref = (RefAddr)en.nextElement();
                    String key = subref.getType();
                    Object value = subref.getContent();
                    values.put(key, (String)value);
                }
                String repositoryName = values.get(FederatedRepositorySource.REPOSITORY_NAME);
                String username = values.get(FederatedRepositorySource.USERNAME);
                String credentials = values.get(FederatedRepositorySource.CREDENTIALS);
                String retryLimit = values.get(FederatedRepositorySource.RETRY_LIMIT);
                String sourceName = values.get(FederatedRepositorySource.SOURCE_NAME);
                String federationSourceJndiName = values.get(FederatedRepositorySource.FEDERATION_SERVICE_JNDI_NAME);

                // Look for the federation service ...
                FederationService federationService = (FederationService)nameCtx.lookup(federationSourceJndiName);
                FederatedRepositorySource source = new FederatedRepositorySource(federationService, repositoryName);
                if (retryLimit != null) source.setRetryLimit(Integer.parseInt(retryLimit));
                if (sourceName != null) source.setName(sourceName);
                if (username != null) source.setUsername(username);
                if (credentials != null) source.setCredentials(credentials);
                return source;
            }
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return repositoryName.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FederatedRepositorySource) {
            FederatedRepositorySource that = (FederatedRepositorySource)obj;
            // The repository name, source name, and federation service must all match
            if (!this.getRepositoryName().equals(that.getRepositoryName())) return false;
            if (!this.getFederationService().equals(that.getFederationService())) return false;
            if (this.getName() == null) {
                if (that.getName() != null) return false;
            } else {
                if (!this.getName().equals(that.getName())) return false;
            }
            return true;
        }
        return false;
    }
}
