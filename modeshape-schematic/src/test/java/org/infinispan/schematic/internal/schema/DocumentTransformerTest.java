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
package org.infinispan.schematic.internal.schema;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.infinispan.schematic.SchemaLibrary;
import org.infinispan.schematic.SchemaLibrary.Results;
import org.infinispan.schematic.SchemaValidationTest;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.infinispan.schematic.document.ParsingException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DocumentTransformerTest {

    protected static final String PARTS_SCHEMA_URI = "json/schema/spec-example-doc.json";

    private static SchemaLibrary schemas;
    private static boolean print;

    @BeforeClass
    public static void beforeAll() throws Exception {
        schemas = Schematic.createSchemaLibrary();
        schemas.put("http://json-schema.org/draft-03/schema#", Json.read(resource("json/schema/draft-03/schema.json")));
        schemas.put(PARTS_SCHEMA_URI, Json.read(resource("json/schema/spec-example.json")));
        // schemas.put("json/schema/repository-config-schema.json",
        // Json.read(resource("json/schema/repository-config-schema.json")));
    }

    @Before
    public void beforeEach() {
        print = false;
    }

    @Test
    public void shouldNotTransformDocumentWithNoMismatchedValues() throws Exception {
        Document doc = doc("{ 'name' : 'Acme Bottle Opener', 'id' : 123 , 'price' : 2.99, 'tags' : [ 'easy', 'under-10-dollars' ] }");
        transform(doc, PARTS_SCHEMA_URI, 0);
    }

    @Test
    public void shouldTransformDocumentWithStringValueWhereIntegerExpected() throws Exception {
        // print = true;
        Document doc = doc("{ 'name' : 'Acme Bottle Opener', 'id' : '123' , 'price' : 2.99, 'tags' : [ 'easy', 'under-10-dollars' ] }");
        transform(doc, PARTS_SCHEMA_URI, 1);
    }

    protected static InputStream resource( String resourcePath ) {
        InputStream result = SchemaValidationTest.class.getClassLoader().getResourceAsStream(resourcePath);
        assert result != null : "Could not find resource \"" + resourcePath + "\"";
        return result;
    }

    protected static Document doc( String content ) throws ParsingException {
        Document doc = Json.read(content);
        if (print) System.out.println(doc);
        return doc;
    }

    protected static Document transform( Document doc,
                                         String schemaUri,
                                         int numExpectedMismatchedValues ) {
        Results results = schemas.validate(doc, schemaUri);
        if (print) System.out.println(results);
        if (numExpectedMismatchedValues > 0) {
            assertThat("expected mismatch errors, but found none", results.hasOnlyTypeMismatchErrors(), is(true));
            assertThat("expected different number of mismatches", results.errorCount(), is(numExpectedMismatchedValues));
        } else {
            assertThat("expected no mismatch errors", results.hasOnlyTypeMismatchErrors(), is(false));
            assertThat("expected to find problems", results.hasProblems(), is(false));
        }
        Document output = schemas.convertValues(doc, results);
        if (numExpectedMismatchedValues > 0) {
            assertThat(output, is(not(sameInstance(doc))));
            // Now double check that the output is valid ...
            Results newResults = schemas.validate(output, schemaUri);
            assertThat(newResults.hasErrors(), is(false));
            if (print) {
                System.out.println("After converting: " + output);
                System.out.println(newResults);
            }
        } else {
            assertThat(output, is(sameInstance(doc)));
        }
        return output;
    }

}
