/*
 *
 */
package org.jboss.dna.services.rules.sequencer;

/**
 * @author John Verhaeg
 */
public final class ContentInfo {

    String mimeType;
    String header;
    String fileName;

    /**
     * @return fileName
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * @return header
     */
    public String getHeader() {
        return header;
    }

    /**
     * @return mimeType
     */
    public String getMimeType() {
        return mimeType;
    }

    /**
     * @param fileName Sets fileName to the specified value.
     */
    public void setFileName( String fileName ) {
        this.fileName = fileName;
    }

    /**
     * @param header Sets header to the specified value.
     */
    public void setHeader( String header ) {
        this.header = header;
    }

    /**
     * @param mimeType Sets mimeType to the specified value.
     */
    public void setMimeType( String mimeType ) {
        this.mimeType = mimeType;
    }
}
