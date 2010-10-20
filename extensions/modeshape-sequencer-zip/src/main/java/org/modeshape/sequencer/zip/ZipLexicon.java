package org.modeshape.sequencer.zip;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the zip sequencer.
 */
@Immutable
public class ZipLexicon {
    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/zip/1.0";
        public static final String PREFIX = "zip";
    }

    public static final Name CONTENT = new BasicName(Namespace.URI, "content");
    public static final Name FILE = new BasicName(Namespace.URI, "file");

}
