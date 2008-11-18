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
package org.jboss.dna.common.jdbc.model.spi;

import org.jboss.dna.common.jdbc.model.api.Catalog;
import org.jboss.dna.common.jdbc.model.api.Schema;

/**
 * Provides database schema specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class SchemaBean extends DatabaseNamedObjectBean implements Schema {
    private static final long serialVersionUID = -3277162949797951267L;
    private Catalog catalog;

    /**
     * Default constructor
     */
    public SchemaBean() {
    }

    /**
     * Gets database catalog
     * 
     * @return database catalog
     */
    public Catalog getCatalog() {
        return catalog;
    }

    /**
     * Sets database catalog
     * 
     * @param catalog the database catalog
     */
    public void setCatalog( Catalog catalog ) {
        this.catalog = catalog;
    }
}
