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
package org.jboss.dna.connector.store.jpa.models.basic;

import java.util.UUID;
import javax.persistence.EntityManager;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.connector.store.jpa.JpaConnectorI18n;
import org.jboss.dna.connector.store.jpa.Model;
import org.jboss.dna.connector.store.jpa.models.common.NamespaceEntity;
import org.jboss.dna.connector.store.jpa.models.common.NodeId;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

/**
 * Database model that stores node properties as opaque records and children as transparent records. Large property values are
 * stored separately.
 * 
 * @author Randall Hauch
 */
public class BasicModel extends Model {

    public BasicModel() {
        super("Basic", JpaConnectorI18n.basicModelDescription);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.connector.store.jpa.Model#createRequestProcessor(java.lang.String, org.jboss.dna.graph.ExecutionContext,
     *      javax.persistence.EntityManager, java.util.UUID, long, boolean)
     */
    @Override
    public RequestProcessor createRequestProcessor( String sourceName,
                                                    ExecutionContext context,
                                                    EntityManager entityManager,
                                                    UUID rootNodeUuid,
                                                    long largeValueMinimumSizeInBytes,
                                                    boolean compressData ) {
        return new BasicRequestProcessor(sourceName, context, entityManager, rootNodeUuid, largeValueMinimumSizeInBytes,
                                         compressData);
    }

    /**
     * Configure the entity class that will be used by JPA to store information in the database.
     * 
     * @param configurator the Hibernate {@link Ejb3Configuration} component; never null
     */
    @Override
    public void configure( Ejb3Configuration configurator ) {
        // Add the annotated classes ...
        configurator.addAnnotatedClass(NamespaceEntity.class);
        configurator.addAnnotatedClass(NodeId.class);
        configurator.addAnnotatedClass(PropertiesEntity.class);
        configurator.addAnnotatedClass(LargeValueEntity.class);
        configurator.addAnnotatedClass(ChildEntity.class);
        configurator.addAnnotatedClass(ChildId.class);

        // Set the cache information for each persistent class ...
        // configurator.setProperty("hibernate.ejb.classcache." + KidpackNode.class.getName(), "read-write");
        // configurator.setProperty("hibernate.ejb.collectioncache" + KidpackNode.class.getName() + ".distributors",
        // "read-write, RegionName");
    }

}
