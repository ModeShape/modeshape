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
package org.modeshape.sequencer.odf;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.xml.datatype.Duration;

import org.odftoolkit.simple.Document;
import org.odftoolkit.simple.PresentationDocument;
import org.odftoolkit.simple.SpreadsheetDocument;
import org.odftoolkit.simple.meta.Meta;

/**
 * Utility for extracting metadata from OpenDocument formats.
 * 
 * @since 5.1
 */
public class OdfMetadata {

    static final String[] MIME_TYPE_STRINGS = { "application/vnd.oasis.opendocument.text",
                                                "application/vnd.oasis.opendocument.spreadsheet",
                                                "application/vnd.oasis.opendocument.presentation",
                                                "application/vnd.oasis.opendocument.graphics",
                                                "application/vnd.oasis.opendocument.chart",
                                                "application/vnd.oasis.opendocument.text-template",
                                                "application/vnd.oasis.opendocument.spreadsheet-template",
                                                "application/vnd.oasis.opendocument.presentation-template",
                                                "application/vnd.oasis.opendocument.graphics-template",
                                                "application/vnd.oasis.opendocument.chart-template",
                                                // not supported yet in odftoolkit:
                                                // "application/vnd.oasis.opendocument.base",
                                                // "application/vnd.oasis.opendocument.formula",
                                                // "application/vnd.oasis.opendocument.formula-template",
                                                // "application/vnd.oasis.opendocument.image",
                                                // "application/vnd.oasis.opendocument.image-template"
                                                };

    private Integer pages;
    private Integer sheets;
    private Calendar creationDate;
    private String creator;
    private String description;
    private Integer editingCycles;
    private Long editingTime;
    private String generator;
    private String initialCreator;
    private List<String> keywords = new ArrayList<>();
    private String language;
    private Calendar modificationDate;
    private String printedBy;
    private Calendar printDate;
    private String title;
    private String subject;

    private InputStream in;

    public OdfMetadata( InputStream inputStream ) {
        this.in = inputStream;
    }

    /*
     * Check that given file is supported by this sequencer and parse the metadata in the process.
     */
    public boolean check() throws Exception {
        Document doc = Document.loadDocument(in);
        Meta metadata = doc.getOfficeMetadata();
        if (metadata != null) {
            title = metadata.getTitle();
            subject = metadata.getSubject();
            description = metadata.getDescription();
            initialCreator = metadata.getInitialCreator();
            creator = metadata.getCreator();
            language = metadata.getLanguage();
            editingCycles = metadata.getEditingCycles();
            creationDate = metadata.getCreationDate();
            modificationDate = metadata.getDcdate();
            if (metadata.getEditingDuration() != null) {
                Duration duration = metadata.getEditingDuration().getValue();
                editingTime = duration.getTimeInMillis(Calendar.getInstance()) / 1000;
            }
            printDate = metadata.getPrintDate();
            printedBy = metadata.getPrintedBy();
            if (metadata.getKeywords() != null) {
                keywords.addAll(metadata.getKeywords());
            }
            generator = metadata.getGenerator();
            if (metadata.getDocumentStatistic() != null) {
                pages = metadata.getDocumentStatistic().getPageCount();
            }
        }

        // document specific meta
        if (doc instanceof PresentationDocument) {
            PresentationDocument presentation = (PresentationDocument) doc;
            pages = presentation.getSlideCount();
        }

        if (doc instanceof SpreadsheetDocument) {
            SpreadsheetDocument spreadsheet = (SpreadsheetDocument) doc;
            sheets = spreadsheet.getSheetCount();
        }

        return true;
    }

    public Integer getPages() {
        return pages;
    }

    public Integer getSheets() {
        return sheets;
    }

    public Calendar getCreationDate() {
        return creationDate;
    }

    public String getCreator() {
        return creator;
    }

    public String getDescription() {
        return description;
    }

    public Integer getEditingCycles() {
        return editingCycles;
    }

    public Long getEditingTime() {
        return editingTime;
    }

    public String getGenerator() {
        return generator;
    }

    public String getInitialCreator() {
        return initialCreator;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public String getLanguage() {
        return language;
    }

    public Calendar getModificationDate() {
        return modificationDate;
    }

    public String getPrintedBy() {
        return printedBy;
    }

    public Calendar getPrintDate() {
        return printDate;
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

}
