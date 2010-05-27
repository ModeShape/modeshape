package org.modeshape.sequencer.mp3;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the mp3 sequencer.
 */
@Immutable
public class Mp3MetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/mp3/1.0";
        public static final String PREFIX = "mp3";
    }

    public static final Name METADATA_NODE = new BasicName(Namespace.URI, "metadata");
    public static final Name TITLE = new BasicName(Namespace.URI, "title");
    public static final Name AUTHOR = new BasicName(Namespace.URI, "author");
    public static final Name ALBUM = new BasicName(Namespace.URI, "album");
    public static final Name YEAR = new BasicName(Namespace.URI, "year");
    public static final Name COMMENT = new BasicName(Namespace.URI, "comment");

}
