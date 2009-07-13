/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.dna.jcr.gen;

import java.io.InputStream;
import java.util.Calendar;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.databene.model.consumer.AbstractConsumer;
import org.databene.model.data.Entity;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.jcr.SecurityContextCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Utility that writes the generated content as nodes to a DNA JCR repository.<br/>
 * 
 * @author Serge Pagop
 */

public class JCRDataMassGen extends AbstractConsumer<Entity> {

    private static final Logger log = LoggerFactory.getLogger(JCRDataMassGen.class);
    // attributes ------------------------------------------------------------------------------------------------------

    private Session session;
    private Node currentNode;
    private boolean printContent;

    private JcrConfiguration configuration;
    private JcrEngine engine;
    private Repository repository;

    // Constructor -----------------------------------------------------------------------------------------------------

    /**
     * Construct a Data Mass Generator, that may be used to generate test data.
     * @param sourceName - the name of the source.
     * @param description - the configuration description.
     * @param beanPropertyName - the name of the bean property.
     * @param workspaceName - the workspace name.
     * @param repositoryName - the repository name.
     * @param repositorySource - 
     * @param username
     * @param printContent
     * @throws Exception
     */
    @SuppressWarnings( "unchecked" )
    public JCRDataMassGen( String sourceName,
                           String description,
                           String beanPropertyName,
                           String workspaceName,
                           String repositoryName,
                           String repositorySource,
                           String username,
                           boolean printContent ) throws Exception {
        this.printContent = printContent;

        // setting up the JcrConfiguration
        configuration = new JcrConfiguration();
        Class<? extends RepositorySource> clazz = (Class<? extends RepositorySource>)Class.forName(repositorySource);
        configuration.repositorySource(sourceName).usingClass(clazz).setDescription(description).setProperty(beanPropertyName,
                                                                                                             workspaceName);
        // repository
        configuration.repository(repositoryName).setSource(sourceName);
        // Start the engine
        engine = configuration.build();
        engine.start();
        // Obtain a JCR Repository instance by name
        repository = engine.getRepository(repositoryName);
        SecurityContext securityContext = new MyCustomSecurityContext(username);
        SecurityContextCredentials credentials = new SecurityContextCredentials(securityContext);
        this.session = repository.login(credentials, workspaceName);
        this.currentNode = session.getRootNode();
    }

    // consumer interface implementation -------------------------------------------------------------------------------

    public void startConsuming( Entity entity ) {
        Node newNode;
        try {
            String n = entity.get("nodeName").toString();
            newNode = currentNode.addNode(n, "nt:unstructured");
            for (Map.Entry<String, Object> component : entity.getComponents().entrySet()) {
                String key = component.getKey();
                Object value = component.getValue();
                if (!"nodeName".equals(key)) setNodeProperty(newNode, key, value);
            }
            currentNode = newNode;
        } catch (Exception e) {
            throw new RuntimeException("Exception whe trying to store " + entity, e);
        }
    }

    @Override
    public void finishConsuming( Entity object ) {
        try {
            session.save();
            currentNode = currentNode.getParent();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {
        try {
            if (printContent) printNode(session.getRootNode(), "");
        } catch (RepositoryException e) {
            throw new RuntimeException(e);

        } finally {
            session.logout();

            // Shutdown the engine
            if (engine != null) {
                engine.shutdown();
                try {
                    engine.awaitTermination(3, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

        }
    }

    public void setPrintContent( boolean printContent ) {
        this.printContent = printContent;
    }

    // private helpers -------------------------------------------------------------------------------------------------

    private void setNodeProperty( Node node,
                                  String propertyName,
                                  Object propertyValue )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (propertyValue instanceof Long) node.setProperty(propertyName, (Long)propertyValue);
        else if (propertyValue instanceof Double) node.setProperty(propertyName, (Double)propertyValue);
        else if (propertyValue instanceof Boolean) node.setProperty(propertyName, (Boolean)propertyValue);
        else if (propertyValue instanceof Calendar) node.setProperty(propertyName, (Calendar)propertyValue);
        else if (propertyValue instanceof String[]) node.setProperty(propertyName, (String[])propertyValue);
        else if (propertyValue instanceof InputStream) node.setProperty(propertyName, (InputStream)propertyValue);
        else if (propertyValue instanceof Node) node.setProperty(propertyName, (Node)propertyValue);
        else if (propertyValue instanceof Value) node.setProperty(propertyName, (Value)propertyValue);
        else if (propertyValue instanceof Value[]) node.setProperty(propertyName, (Value[])propertyValue);
        else node.setProperty(propertyName, propertyValue.toString());
    }

    private void printNode( Node node,
                            String indent ) throws RepositoryException {
        log.info(indent + "Node: " + node.getName() + " " + node.getPrimaryNodeType().getName());
        PropertyIterator propIt = node.getProperties();
        while (propIt.hasNext()) {
            Property property = (Property)propIt.next();
            log.info(indent + '\t' + '\t' + property.getName());
            Value value = property.getValue();
            log.info(indent + '\t' + '\t' + '\t' + value.getString());

        }
        NodeIterator iterator = node.getNodes();
        while (iterator.hasNext())
            printNode(iterator.nextNode(), indent + '\t');
    }

    protected class MyCustomSecurityContext implements SecurityContext {
        private String username;

        public MyCustomSecurityContext( String username ) {
            this.username = username;
        }

        /**
         * @see org.jboss.dna.graph.SecurityContext#getUserName()
         */
        public String getUserName() {
            return this.username;
        }

        /**
         * @see org.jboss.dna.graph.SecurityContext#hasRole(java.lang.String)
         */

        public boolean hasRole( String roleName ) {
            return true;
        }

        /**
         * @see org.jboss.dna.graph.SecurityContext#logout()
         */
        public void logout() {
            // do something
        }
    }

}
