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
package org.jboss.dna.jcr;

import java.util.HashSet;
import java.util.Set;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import org.jboss.dna.spi.ExecutionContext;
import org.jboss.dna.spi.connector.RepositoryConnection;
import org.jboss.dna.spi.graph.NameFactory;
import org.jboss.dna.spi.graph.Path;
import org.jboss.dna.spi.graph.PathFactory;
import org.jboss.dna.spi.graph.PropertyType;
import org.jboss.dna.spi.graph.ValueFactories;
import org.jboss.dna.spi.graph.commands.GraphCommand;
import org.jboss.dna.spi.graph.commands.impl.BasicGetNodeCommand;

/**
 * @author jverhaeg
 */
class GraphTools {

    private final ExecutionContext executionContext;
    private Session session;
    private final RepositoryConnection connection;

    GraphTools( ExecutionContext executionContext,
                RepositoryConnection connection ) {
        assert executionContext != null;
        assert connection != null;
        this.executionContext = executionContext;
        this.connection = connection;
    }

    private void execute( GraphCommand... commands ) throws RepositoryException {
        try {
            connection.execute(executionContext, commands);
        } catch (RuntimeException error) {
            throw error;
        } catch (Exception error) {
            throw new RepositoryException(error);
        }
    }

    /**
     * @return connection
     */
    public RepositoryConnection getConnection() {
        return connection;
    }

    NodeContent getNodeContent( Path path ) throws RepositoryException {
        assert session != null;
        assert path != null;
        // Get root node from source
        ValueFactories valueFactories = executionContext.getValueFactories();
        PathFactory pathFactory = valueFactories.getPathFactory();
        BasicGetNodeCommand getRootNodeCommand = new BasicGetNodeCommand(path);
        execute(getRootNodeCommand);
        // Get primary type
        NameFactory nameFactory = valueFactories.getNameFactory();
        org.jboss.dna.spi.graph.Property primaryTypeProp = getRootNodeCommand.getProperties().get(nameFactory.create("jcr:primaryType"));
        org.jboss.dna.spi.graph.ValueFactory<String> stringFactory = valueFactories.getStringFactory();
        String primaryType = stringFactory.create(primaryTypeProp.getValues()).next();
        // Process node's properties
        NodeContent content = new NodeContent();
        org.jboss.dna.spi.graph.ValueFactory<Boolean> booleanFactory = valueFactories.getBooleanFactory();
        for (org.jboss.dna.spi.graph.Property prop : getRootNodeCommand.getPropertyIterator()) {
            // Get property definition from node's primary type
            BasicGetNodeCommand getPropDefCommand = new BasicGetNodeCommand(pathFactory.create("/dna:system/dna:jcr/"
                                                                                               + primaryType + "/"
                                                                                               + prop.getName()));
            execute(getPropDefCommand);
            // Create either a single- or multiple-valued property, as defined by the property definition
            org.jboss.dna.spi.graph.Property isMultipleProp = getPropDefCommand.getProperties().get(nameFactory.create("jcr:multiple"));
            org.jboss.dna.spi.graph.Property requiredTypeProp = getPropDefCommand.getProperties().get(nameFactory.create("jcr:requiredType"));
            String type = stringFactory.create(requiredTypeProp.getValues()).next();
            if (booleanFactory.create(isMultipleProp.getValues()).next().booleanValue()) {
                // jcrProps.add(new JcrMultiValuedProperty(dnaProp.getName(), type, dnaProp.getValues()));
            } else {
                Value jcrValue;
                ValueFactory jcrValueFactory = session.getValueFactory();
                if (PropertyType.BINARY.equals(type)) {
                    jcrValue = jcrValueFactory.createValue(valueFactories.getBinaryFactory().create(prop.getValues()).next().getStream());
                } else {
                    assert PropertyType.UUID.equals(type);
                    jcrValue = jcrValueFactory.createValue(valueFactories.getStringFactory().create(prop.getValues()).next());
                }
                content.properties.add(new JcrProperty(session, prop.getName().toString(), jcrValue));
            }
        }
        return content;
    }

    void setSession( Session session ) {
        assert session != null;
        this.session = session;
    }

    class NodeContent {

        Set<Property> properties = new HashSet<Property>();
    }
}
