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

import org.modeshape.schematic.annotation.Immutable;
import org.modeshape.schematic.document.Document;

/**
 * A Schema is a Document containing a description of the structure of other JSON/BSON documents.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 */
@Immutable
public class SchemaDocument {

    private final String uri;
    private final Document document;
    private final Validator validator;

    public SchemaDocument( String uri,
                           Document document,
                           Validator validator ) {
        this.uri = uri;
        this.validator = validator;
        this.document = document;
    }

    public String getUri() {
        return uri;
    }

    public Document getDocument() {
        return document;
    }

    public Validator getValidator() {
        return validator;
    }
}
