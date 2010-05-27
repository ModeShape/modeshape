package org.modeshape.sequencer.msoffice;

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.basic.BasicName;

/**
 * A lexicon of names used within the MS Office sequencer.
 */
@Immutable
public class MSOfficeMetadataLexicon {

    public static class Namespace {
        public static final String URI = "http://www.modeshape.org/msoffice/1.0";
        public static final String PREFIX = "msoffice";
    }

    public static final Name METADATA_NODE = new BasicName(Namespace.URI, "metadata");
    public static final Name TITLE = new BasicName(Namespace.URI, "title");
    public static final Name SUBJECT = new BasicName(Namespace.URI, "subject");
    public static final Name AUTHOR = new BasicName(Namespace.URI, "author");
    public static final Name KEYWORDS = new BasicName(Namespace.URI, "keywords");
    public static final Name COMMENT = new BasicName(Namespace.URI, "comment");
    public static final Name TEMPLATE = new BasicName(Namespace.URI, "template");
    public static final Name LAST_SAVED_BY = new BasicName(Namespace.URI, "last_saved_by");
    public static final Name REVISION = new BasicName(Namespace.URI, "revision");
    public static final Name TOTAL_EDITING_TIME = new BasicName(Namespace.URI, "total_editing_time");
    public static final Name LAST_PRINTED = new BasicName(Namespace.URI, "last_printed");
    public static final Name CREATED = new BasicName(Namespace.URI, "created");
    public static final Name SAVED = new BasicName(Namespace.URI, "saved");
    public static final Name PAGES = new BasicName(Namespace.URI, "pages");
    public static final Name WORDS = new BasicName(Namespace.URI, "words");
    public static final Name CHARACTERS = new BasicName(Namespace.URI, "characters");
    public static final Name CREATING_APPLICATION = new BasicName(Namespace.URI, "creating_application");
    public static final Name THUMBNAIL = new BasicName(Namespace.URI, "thumbnail");
    public static final Name SLIDE = new BasicName(Namespace.URI, "slide");
    public static final Name TEXT = new BasicName(Namespace.URI, "text");
    public static final Name NOTES = new BasicName(Namespace.URI, "notes");
    public static final Name FULL_CONTENT = new BasicName(Namespace.URI, "full_contents");
    public static final Name SHEET_NAME = new BasicName(Namespace.URI, "sheet_name");
    public static final Name HEADING_NODE = new BasicName(Namespace.URI, "heading");
    public static final Name HEADING_NAME = new BasicName(Namespace.URI, "heading_name");
    public static final Name HEADING_LEVEL = new BasicName(Namespace.URI, "heading_level");

}
