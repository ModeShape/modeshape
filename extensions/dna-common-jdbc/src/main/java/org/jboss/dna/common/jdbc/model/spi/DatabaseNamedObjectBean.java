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
package org.jboss.dna.common.jdbc.model.spi;

import java.util.Map;
import java.util.HashMap;
import org.jboss.dna.common.jdbc.model.api.DatabaseNamedObject;

/**
 * Provides database named object specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class DatabaseNamedObjectBean extends CoreMetaDataBean implements DatabaseNamedObject {
    private static final long serialVersionUID = 5784316298846262968L;
    private String name;
    private String remarks;
    private Map<String, Object> extraProperties = new HashMap<String, Object>();

    /**
     * Default constructor
     */
    public DatabaseNamedObjectBean() {
    }

    /**
     * Gets database named object name
     * 
     * @return database named object name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets database named object name
     * 
     * @param name the database named object name
     */
    public void setName( String name ) {
        this.name = name;
    }

    /**
     * Gets explanatory comment on the database named object
     * 
     * @return explanatory comment on the database named object
     */
    public String getRemarks() {
        return remarks;
    }

    /**
     * Sets explanatory comment on the database named object
     * 
     * @param remarks the explanatory comment on the database named object
     */
    public void setRemarks( String remarks ) {
        this.remarks = remarks;
    }

    /**
     * Gets extra (non standard) properties if provided by database.
     * 
     * @return extra properties if provided by database
     */
    public Map<String, Object> getExtraProperties() {
        return extraProperties;
    }

    /**
     * Gets extra (non standard) property if provided by database.
     * 
     * @param key the key
     * @return extra property if provided by database
     */
    public Object getExtraProperty( String key ) {
        return extraProperties.get(key);
    }

    /**
     * Adds extra property
     * 
     * @param key the key
     * @param value the value
     */
    public void addExtraProperty( String key,
                                  Object value ) {
        extraProperties.put(key, value);
    }

    /**
     * deletes extra property
     * 
     * @param key the key
     * 
     */
    public void deleteExtraProperty( String key ) {
        extraProperties.remove(key);
    }
}
