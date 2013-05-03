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
package org.modeshape.connector.cmis;

/**
 * Implements unique identifier of the object in the JCR domain.
 *
 * This identifier carries unique object identifier and how this object should be
 * reflected. 
 * 
 * The implementation of the connector suppose conversation between cmis folders 
 * and documents into jcr folders and files. It means that some of the properties 
 * like binary content of the cmis document will be represented as node rather 
 * then just property. Thus to perform such reflection we need to introduce key 
 * mappings which establishes relations between cmis objects and jcr nodes.
 * 
 * This class suppose to use the original unique identifier of the cmis object and 
 * adds suffix corresponding to the object's type.
 *
 * @author kulikov
 */
public class ObjectId {
    //this are object types we can outline
    public enum Type {REPOSITORY_INFO, CONTENT, OBJECT}

    private Type type;
    private String id;

    /**
     * Gets type of the object to indicated by identifier part.
     * 
     * @return object type.
     */
    public Type getType() {
        return type;
    }

    /**
     * Gets unique identifier of the object in CMIS domain.
     * 
     * @return object identifier.
     */
    public String getIdentifier() {
        return id;
    }
    
    /**
     * Constructs new unique object identifier in the JCR domain.
     * 
     * @param type type of the object 
     * @param id unique identifier of the object in cmis domain.
     */
    protected ObjectId(Type type, String id) {
        this.type = type;
        this.id = id;
    }

    /**
     * Constructs instance of this class from its textual representation.
     * 
     * @param uuid the textual representation of this object.
     * @return object instance.
     */
    public static ObjectId valueOf(String uuid) {
        int p = uuid.indexOf("/");
        if (p < 0) {
            return new ObjectId(Type.OBJECT, uuid);
        }
        String ident = uuid.substring(0, p);
        String type = uuid.substring(p + 1);

        return new ObjectId(Type.valueOf(type.toUpperCase()), ident);
    }

    /**
     * Provides textual representation for this object.
     * 
     * @param type the object type
     * @param id object identifier in cmis domain.
     * 
     * @return text view of this identifier.
     */
    public static String toString(Type type, String id) {
        return type == Type.OBJECT? id : id + "/" + type.toString();
    }
    
}
