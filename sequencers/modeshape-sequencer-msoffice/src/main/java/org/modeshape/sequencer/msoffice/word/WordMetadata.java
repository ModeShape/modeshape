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

package org.modeshape.sequencer.msoffice.word;

import java.util.Collections;
import java.util.List;
import org.apache.poi.hpsf.SummaryInformation;
import org.modeshape.sequencer.msoffice.MSOfficeMetadata;

/**
 * Metadata for Microsoft Word documents.
 */
public class WordMetadata {

    private List<WordHeading> headings = Collections.emptyList();
    private MSOfficeMetadata metadata;

    public MSOfficeMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata( MSOfficeMetadata metadata ) {
        this.metadata = metadata;
    }

    public void setMetadata( SummaryInformation info ) {
        if (info != null) {
            metadata = new MSOfficeMetadata();
            metadata.setSummaryInformation(info);
        }
    }

    public List<WordHeading> getHeadings() {
        return headings;
    }

    public void setHeadings( List<WordHeading> headings ) {
        this.headings = headings;
    }

    public static class WordHeading {
        private String text;
        private int headingLevel;

        public WordHeading( String text,
                            int headerLevel ) {
            super();
            this.text = text;
            this.headingLevel = headerLevel;
        }

        public String getText() {
            return text;
        }

        public void setText( String text ) {
            this.text = text;
        }

        public int getHeaderLevel() {
            return headingLevel;
        }

        public void setHeaderLevel( int headerLevel ) {
            this.headingLevel = headerLevel;
        }

    }
}
