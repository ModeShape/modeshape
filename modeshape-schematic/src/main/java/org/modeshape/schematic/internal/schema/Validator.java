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
package org.modeshape.schematic.internal.schema;

import java.io.Serializable;
import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.Path;

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
