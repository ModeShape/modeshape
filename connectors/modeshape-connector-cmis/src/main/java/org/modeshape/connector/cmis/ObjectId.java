/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.connector.cmis;

/**
 * Implements unique identifier of the object in the JCR domain. This identifier carries unique object identifier and how this
 * object should be reflected. The implementation of the connector suppose conversation between cmis folders and documents into
 * jcr folders and files. It means that some of the properties like binary content of the cmis document will be represented as
 * node rather then just property. Thus to perform such reflection we need to introduce key mappings which establishes relations
 * between cmis objects and jcr nodes. This class suppose to use the original unique identifier of the cmis object and adds suffix
 * corresponding to the object's type.
 * 
 * @author kulikov
 */
public class ObjectId {
    // this are object types we can outline
    public enum Type {
        REPOSITORY_INFO,
        CONTENT,
        OBJECT,
        ACL,
        PERMISSIONS
    }

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
    protected ObjectId( Type type,
                        String id ) {
        this.type = type;
        this.id = id;
    }

    /**
     * Constructs instance of this class from its textual representation.
     * 
     * @param uuid the textual representation of this object.
     * @return object instance.
     */
    public static ObjectId valueOf( String uuid ) {
        int p = uuid.indexOf("/");
        if (p < 0) {
            return new ObjectId(Type.OBJECT, uuid);
        }
        
        int p1 = p;
        while (p > 0) {
            p1 = p;
            p = uuid.indexOf("/", p + 1);
        }
        
        p = p1;
        String ident = uuid.substring(0, p);
        String type = uuid.substring(p + 1);

        return new ObjectId(Type.valueOf(type.toUpperCase()), ident);
    }

    /**
     * Provides textual representation for this object.
     * 
     * @param type the object type
     * @param id object identifier in cmis domain.
     * @return text view of this identifier.
     */
    public static String toString( Type type,
                                   String id ) {
        return type == Type.OBJECT ? id : id + "/" + type.toString();
    }

}
