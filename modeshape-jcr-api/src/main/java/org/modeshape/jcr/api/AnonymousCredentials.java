/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.api;

import java.util.HashMap;
import java.util.Map;
import javax.jcr.Credentials;

/**
 * A {@link Credentials} implementation that can be used to represent anonymous users when ModeShape is configured to allow
 * anonymous users.
 * <p>
 * Note that this implementation supports attributes.
 */
public class AnonymousCredentials implements Credentials {

    private static final long serialVersionUID = 1L;

    private final Map<String, Object> attributes = new HashMap<String, Object>();

    /**
     * Create a new instance of the anonymous credentials.
     */
    public AnonymousCredentials() {
    }

    /**
     * Create a new instance of the anonymous credentials, with the supplied attributes.
     * 
     * @param attributeName the name of the attribute; may not be null
     * @param attributeValue the value of the attribute
     */
    public AnonymousCredentials( String attributeName,
                                 Object attributeValue ) {
        setAttribute(attributeName, attributeValue);
    }

    /**
     * Create a new instance of the anonymous credentials, with the supplied attributes.
     * 
     * @param attribute1Name the name of the attribute; may not be null
     * @param attribute1Value the value of the attribute
     * @param attribute2Name the name of the attribute; may not be null
     * @param attribute2Value the value of the attribute
     */
    public AnonymousCredentials( String attribute1Name,
                                 Object attribute1Value,
                                 String attribute2Name,
                                 Object attribute2Value ) {
        setAttribute(attribute1Name, attribute1Value);
        setAttribute(attribute2Name, attribute2Value);
    }

    /**
     * Create a new instance of the anonymous credentials, with the supplied attributes.
     * 
     * @param attribute1Name the name of the attribute; may not be null
     * @param attribute1Value the value of the attribute
     * @param attribute2Name the name of the attribute; may not be null
     * @param attribute2Value the value of the attribute
     * @param attribute3Name the name of the attribute; may not be null
     * @param attribute3Value the value of the attribute
     */
    public AnonymousCredentials( String attribute1Name,
                                 Object attribute1Value,
                                 String attribute2Name,
                                 Object attribute2Value,
                                 String attribute3Name,
                                 Object attribute3Value ) {
        setAttribute(attribute1Name, attribute1Value);
        setAttribute(attribute2Name, attribute2Value);
        setAttribute(attribute3Name, attribute3Value);
    }

    /**
     * Stores an attribute in this credentials instance.
     * 
     * @param name a <code>String</code> specifying the name of the attribute
     * @param value the <code>Object</code> to be stored
     */
    public void setAttribute( String name,
                              Object value ) {
        // name cannot be null
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }

        // null value is the same as removeAttribute()
        if (value == null) {
            removeAttribute(name);
            return;
        }

        synchronized (attributes) {
            attributes.put(name, value);
        }
    }

    /**
     * Returns the value of the named attribute as an <code>Object</code>, or <code>null</code> if no attribute of the given name
     * exists.
     * 
     * @param name a <code>String</code> specifying the name of the attribute
     * @return an <code>Object</code> containing the value of the attribute, or <code>null</code> if the attribute does not exist
     */
    public Object getAttribute( String name ) {
        synchronized (attributes) {
            return (attributes.get(name));
        }
    }

    /**
     * Removes an attribute from this credentials instance.
     * 
     * @param name a <code>String</code> specifying the name of the attribute to remove
     */
    public void removeAttribute( String name ) {
        synchronized (attributes) {
            attributes.remove(name);
        }
    }

    /**
     * Returns the names of the attributes available to this credentials instance. This method returns an empty array if the
     * credentials instance has no attributes available to it.
     * 
     * @return a string array containing the names of the stored attributes
     */
    public String[] getAttributeNames() {
        synchronized (attributes) {
            return attributes.keySet().toArray(new String[attributes.keySet().size()]);
        }
    }

    /**
     * @return attributes
     */
    public Map<String, Object> getAttributes() {
        synchronized (attributes) {
            return new HashMap<String, Object>(attributes);
        }
    }

}
