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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.modeshape.common.logging.Logger;

/**
 * Infers table of contents from Word document by reading all paragraphs with style <code>Heading*</code>. This is analogous to
 * the default behavior of Word when generating a table of contents.
 */
public class WordMetadataReader {

    private static final Logger log = Logger.getLogger(WordMetadataReader.class);

    /** Prefix for styles that will be extracted and treated as outline information for the document */
    private static final String HEADER_PREFIX = "Heading";

    public static WordMetadata instance( InputStream stream ) throws IOException {
        WordMetadata metadata = new WordMetadata();
        List<WordMetadata.WordHeading> headings = new ArrayList<WordMetadata.WordHeading>();

        HWPFDocument document = new HWPFDocument(stream);
        Range range = document.getRange();

        StyleSheet stylesheet = document.getStyleSheet();

        for (int i = 0; i < range.numParagraphs(); i++) {
            Paragraph paragraph = range.getParagraph(i);

            String styleName = stylesheet.getStyleDescription(paragraph.getStyleIndex()).getName();

            if (styleName.startsWith(HEADER_PREFIX)) {
                String rawLevelNum = styleName.substring(HEADER_PREFIX.length() + 1).trim();
                int levelNum = 0;

                try {
                    levelNum = Integer.parseInt(rawLevelNum);
                } catch (NumberFormatException nfe) {
                    log.debug("Could not parse heading level from: " + styleName);
                }

                String text = Paragraph.stripFields(paragraph.text());

                if ('\r' == text.charAt(text.length() - 1)) {
                    text = text.substring(0, text.length() - 1);
                }

                headings.add(new WordMetadata.WordHeading(text, levelNum));
            }
        }

        metadata.setHeadings(headings);
        metadata.setMetadata(document.getSummaryInformation());
        return metadata;
    }
}
