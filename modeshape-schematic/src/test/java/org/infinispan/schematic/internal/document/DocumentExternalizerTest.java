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
package org.infinispan.schematic.internal.document;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.InputStream;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.Json;
import org.junit.Test;

public class DocumentExternalizerTest extends AbstractExternalizerTest {

    @Test
    public void shouldRoundTripSampleRepositoryConfiguration() throws Exception {
        assertRoundTripJsonDocument("json/sample-repo-config.json");
    }

    @Test
    public void shouldRoundTripSimpleDocument() throws Exception {
        assertRoundTripJsonDocument("json/spec-example-doc.json");
    }

    @Test
    public void shouldRoundTripEmptyDocument() throws Exception {
        assertRoundTripJsonDocument("json/empty.json");
    }

    protected void assertRoundTripJsonDocument( String resourcePath ) throws Exception {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        assertThat(stream, is(notNullValue()));
        Document doc = Json.read(stream);
        byte[] bytes = marshall(doc);
        Document newDoc = (Document)unmarshall(bytes);
        assertThat(newDoc, is(doc));
    }

}
