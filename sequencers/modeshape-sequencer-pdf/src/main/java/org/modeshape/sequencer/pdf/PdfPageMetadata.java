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
package org.modeshape.sequencer.pdf;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility for extracting metadata from PDF files' pages.
 * 
 * @since 5.1
 */
public class PdfPageMetadata {

    private Integer pageNumber;
    private List<PdfAttachmentMetadata> attachments = new ArrayList<PdfAttachmentMetadata>();


    public Integer getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber( Integer pageNumber ) {
        this.pageNumber = pageNumber;
    }

    public List<PdfAttachmentMetadata> getAttachments() {
        return attachments;
    }

    public void setAttachments( List<PdfAttachmentMetadata> attachments ) {
        this.attachments = attachments;
    }

    public void addAttachment( PdfAttachmentMetadata attachment ) {
        this.attachments.add(attachment);
    }

}
