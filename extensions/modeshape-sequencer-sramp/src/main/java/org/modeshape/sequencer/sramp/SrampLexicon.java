package org.modeshape.sequencer.sramp;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of S-RAMP names used within the XSD sequencer.
 */
@Immutable
public class SrampLexicon {
    public static class Namespace {
        public static final String URI = "http://s-ramp.org/xmlns/2010/s-ramp";
        public static final String PREFIX = "sramp";
    }

    public static final Name BASE_ARTIFACT_TYPE = new BasicName(Namespace.URI, "baseArtifactType");
    public static final Name CLASSIFIED_BY = new BasicName(Namespace.URI, "classifiedBy");
    public static final Name DESCRIPTION = new BasicName(Namespace.URI, "description");

    public static final Name DOCUMENT_ARTIFACT_TYPE = new BasicName(Namespace.URI, "documentArtifactType");
    public static final Name CONTENT_TYPE = new BasicName(Namespace.URI, "contentType");
    public static final Name CONTENT_SIZE = new BasicName(Namespace.URI, "contentSize");

    public static final Name XML_DOCUMENT = new BasicName(Namespace.URI, "xmlDocument");
    public static final Name CONTENT_ENCODING = new BasicName(Namespace.URI, "contentEncoding");

    public static final Name DOCUMENT = new BasicName(Namespace.URI, "document");

    public static final Name DERIVED_ARTIFACT_TYPE = new BasicName(Namespace.URI, "derivedArtifactType");
    public static final Name RELATED_DOCUMENTS = new BasicName(Namespace.URI, "relatedDocuments");

    public static final Name USER_DEFINED_ARTIFACT_TYPE = new BasicName(Namespace.URI, "userDefinedArtifactType");
    public static final Name USER_TYPE = new BasicName(Namespace.URI, "userType");

    public static final Name STORED_QUERY = new BasicName(Namespace.URI, "storedQuery");
    public static final Name PROPERTY_LIST = new BasicName(Namespace.URI, "propertyList");

    public static final Name RELATED_TO = new BasicName(Namespace.URI, "relatedTo");

}
