/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.connector.store.jpa;

import java.util.Locale;
import java.util.UUID;
import javax.persistence.EntityManager;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * A descriptor of a schema used by this connector.
 * 
 * @see JpaSource.Models
 * @see JpaSource.Models#addModel(Model)
 * @see JpaSource#setModel(String)
 * @see JpaSource#getModel()
 * @author Randall Hauch
 */
public abstract class Model {
    private final String name;
    private final I18n description;

    protected Model( String name,
                     I18n description ) {
        CheckArg.isNotEmpty(name, "name");
        CheckArg.isNotNull(description, "description");
        this.name = name;
        this.description = description;
    }

    public final String getName() {
        return this.name;
    }

    /**
     * Get the description of this model in the default locale.
     * 
     * @return the description of this model; never null
     */
    public String getDescription() {
        return description.text();
    }

    /**
     * Get the description of this model in the supplied locale.
     * 
     * @param locale the locale in which the description is to be returned
     * @return the description of this model; never null
     */
    public String getDescription( Locale locale ) {
        return description.text(locale);
    }

    public abstract RequestProcessor createRequestProcessor( String sourceName,
                                                             ExecutionContext context,
                                                             EntityManager entityManager,
                                                             UUID rootNodeUuid,
                                                             long largeValueMinimumSizeInBytes,
                                                             boolean comparessData,
                                                             boolean enforceReferentialIntegrity );

    /**
     * Configure the entity class that will be used by JPA to store information in the database.
     * 
     * @param configurator the Hibernate {@link Ejb3Configuration} component; never null
     */
    public abstract void configure( Ejb3Configuration configurator );

    @Override
    public final int hashCode() {
        return this.name.hashCode();
    }

    @Override
    public final boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Model) {
            Model that = (Model)obj;
            if (this.getName().equals(that.getName())) return true;
        }
        return false;
    }

    @Override
    public final String toString() {
        return name;
    }
}
