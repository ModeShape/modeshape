/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
package org.infinispan.schematic.internal.schema;

import java.io.Serializable;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Immutable;
import org.infinispan.schematic.document.Path;

@Immutable
public interface Validator extends Serializable {

    /**
     * Validate a portion of the supplied document.
     * 
     * @param fieldValue the field value to be validated; may be null
     * @param fieldName the field name to be validated; may be null if the field value is not known or the validator is to
     *        validate the document
     * @param document the document; never null
     * @param pathToDocument the path to the supplied document; never null but may be a zero-length path if the document is the
     *        top-level document
     * @param problems the problems where any errors or warnings should be recorded; never null
     * @param resolver the component that can be used to resolve references to other schema documents
     */
    void validate( Object fieldValue,
                   String fieldName,
                   Document document,
                   Path pathToDocument,
                   Problems problems,
                   SchemaDocumentResolver resolver );

    static interface Factory {
        public Validator create( Document schemaDocument,
                                 Path pathToDoc );
    }

    static interface SchemaDocumentResolver {
        SchemaDocument get( String uri,
                            Problems problems );
    }

}
