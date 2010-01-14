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

package org.modeshape.sequencer.msoffice.word;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.model.StyleSheet;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.modeshape.common.util.Logger;

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
        return metadata;
    }
}
