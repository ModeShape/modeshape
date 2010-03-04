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
package org.modeshape.sequencer.msoffice;

import org.apache.poi.hpsf.PropertySetFactory;
import org.apache.poi.hpsf.SummaryInformation;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderEvent;
import org.apache.poi.poifs.eventfilesystem.POIFSReaderListener;
import org.modeshape.common.util.Logger;

import java.util.Date;

/**
 * Metadata about an Microsoft Office file.
 */
public class MSOfficeMetadata implements POIFSReaderListener {

    private String title;
    private String subject;
    private String author;
    private String keywords;
    private String comment;
    private String template;
    private Date lastSavedBy;
    private String revision;
    private Long totalEditingTime;
    private Date lastPrinted;
    private Date created;
    private Date saved;
    private int pages;
    private int words;
    private int characters;
    private String creatingApplication;
    private byte[] thumbnail;
    private static final Logger LOGGER = Logger.getLogger(MSOfficeMetadata.class);

    public void processPOIFSReaderEvent( POIFSReaderEvent event ) {
        try {
            SummaryInformation si = (SummaryInformation)PropertySetFactory.create(event.getStream());
            title = si.getTitle();
            subject = si.getSubject();
            author = si.getAuthor();
            keywords = si.getKeywords();
            comment = si.getComments();
            template = si.getTemplate();
            lastSavedBy = si.getLastSaveDateTime();
            revision = si.getRevNumber();
            totalEditingTime = si.getEditTime();
            lastPrinted = si.getLastPrinted();
            created = si.getCreateDateTime();
            saved = si.getLastSaveDateTime();
            pages = si.getPageCount();
            words = si.getWordCount();
            characters = si.getCharCount();
            creatingApplication = si.getApplicationName();
            thumbnail = si.getThumbnail();
        } catch (Exception ex) {
            LOGGER.debug(ex, "Error processing the metadata for the MS Office document");
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

    public Date getLastSavedBy() {
        return lastSavedBy;
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

    public Date getSaved() {
        return saved;
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
