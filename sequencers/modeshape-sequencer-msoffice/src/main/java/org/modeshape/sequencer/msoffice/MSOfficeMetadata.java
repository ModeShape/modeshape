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
package org.modeshape.sequencer.msoffice;

import java.util.Date;
import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.modeshape.common.logging.Logger;

/**
 * Metadata about an Microsoft Office file.
 */
public class MSOfficeMetadata implements POIFSReaderListener {

    private static final Logger LOGGER = Logger.getLogger(MSOfficeMetadata.class);

    private String title;
    private String subject;
    private String author;
    private String keywords;
    private String comment;
    private String template;
    private Date lastSaved;
    private String revision;
    private Long totalEditingTime;
    private Date lastPrinted;
    private Date created;
    private int pages;
    private int words;
    private int characters;
    private String creatingApplication;
    private byte[] thumbnail;

    public void setSummaryInformation( SummaryInformation si ) {
        title = si.getTitle();
        subject = si.getSubject();
        author = si.getAuthor();
        keywords = si.getKeywords();
        comment = si.getComments();
        template = si.getTemplate();
        lastSaved = si.getLastSaveDateTime();
        revision = si.getRevNumber();
        totalEditingTime = si.getEditTime();
        lastPrinted = si.getLastPrinted();
        created = si.getCreateDateTime();
        pages = si.getPageCount();
        words = si.getWordCount();
        characters = si.getCharCount();
        creatingApplication = si.getApplicationName();
        thumbnail = si.getThumbnail();
    }

    @Override
    public void processPOIFSReaderEvent( POIFSReaderEvent event ) {
        try {
            SummaryInformation si = (SummaryInformation)PropertySetFactory.create(event.getStream());
            setSummaryInformation(si);
        } catch (Exception ex) {
            LOGGER.debug("Error processing the metadata for the MS Office document", ex);
        }
    }

    public String getTitle() {
        return title;
    }

    public String getSubject() {
        return subject;
    }

    public String getAuthor() {
        return author;
    }

    public String getKeywords() {
        return keywords;
    }

    public String getComment() {
        return comment;
    }

    public String getTemplate() {
        return template;
    }

    public Date getLastSaved() {
        return lastSaved;
    }

    public String getRevision() {
        return revision;
    }

    public Long getTotalEditingTime() {
        return totalEditingTime;
    }

    public Date getLastPrinted() {
        return lastPrinted;
    }

    public Date getCreated() {
        return created;
    }

    public int getPages() {
        return pages;
    }

    public int getWords() {
        return words;
    }

    public int getCharacters() {
        return characters;
    }

    public String getCreatingApplication() {
        return creatingApplication;
    }

    public byte[] getThumbnail() {
        return thumbnail;
    }
}
