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
package org.jboss.dna.maven.spi;

import java.util.Properties;
import org.jboss.dna.common.collection.UnmodifiableProperties;

/**
 * @author Randall Hauch
 */
public abstract class AbstractMavenUrlProvider implements MavenUrlProvider {

    private Properties properties = new Properties();

    public AbstractMavenUrlProvider() {
    }

    /**
     * {@inheritDoc}
     */
    public void configure( Properties properties ) {
        this.properties = new UnmodifiableProperties(properties);
    }

    /**
     * Get the properties for this provider.
     * @return the properties
     */
    public Properties getProperties() {
        return this.properties;
    }

    /**
     * @param properties Sets properties to the specified value.
     */
    public void setProperties( Properties properties ) {
        this.properties = properties != null ? properties : new Properties();
    }

}
