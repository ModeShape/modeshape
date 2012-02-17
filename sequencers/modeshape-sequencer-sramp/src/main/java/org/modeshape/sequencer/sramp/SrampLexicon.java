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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.sramp;

import org.modeshape.common.annotation.Immutable;
import static org.modeshape.sequencer.sramp.SrampLexicon.Namespace.PREFIX;

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
