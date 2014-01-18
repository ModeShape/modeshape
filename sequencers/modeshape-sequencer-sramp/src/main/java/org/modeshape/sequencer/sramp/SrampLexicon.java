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
package org.modeshape.sequencer.sramp;

import static org.modeshape.sequencer.sramp.SrampLexicon.Namespace.PREFIX;
import org.modeshape.common.annotation.Immutable;

/**
 * A lexicon of S-RAMP names used within the XSD sequencer.
 */
@Immutable
public class SrampLexicon {
    private SrampLexicon() {
    }

    public static class Namespace {
        public static final String URI = "http://s-ramp.org/xmlns/2010/s-ramp";
        public static final String PREFIX = "sramp";
    }

    public static final String BASE_ARTIFACT_TYPE = PREFIX + ":baseArtifactType";
    public static final String CLASSIFIED_BY = PREFIX + ":classifiedBy";
    public static final String DESCRIPTION = PREFIX + ":description";

    public static final String DOCUMENT_ARTIFACT_TYPE = PREFIX + ":documentArtifactType";
    public static final String CONTENT_TYPE = PREFIX + ":contentType";
    public static final String CONTENT_SIZE = PREFIX + ":contentSize";

    public static final String XML_DOCUMENT = PREFIX + ":xmlDocument";
    public static final String CONTENT_ENCODING = PREFIX + ":contentEncoding";

    public static final String DOCUMENT = PREFIX + ":document";

    public static final String DERIVED_ARTIFACT_TYPE = PREFIX + ":derivedArtifactType";
    public static final String RELATED_DOCUMENTS = PREFIX + ":relatedDocuments";

    public static final String USER_DEFINED_ARTIFACT_TYPE = PREFIX + ":userDefinedArtifactType";
    public static final String USER_TYPE = PREFIX + ":userType";

    public static final String STORED_QUERY = PREFIX + ":storedQuery";
    public static final String PROPERTY_LIST = PREFIX + ":propertyList";

    public static final String RELATED_TO = PREFIX + ":relatedTo";

}
