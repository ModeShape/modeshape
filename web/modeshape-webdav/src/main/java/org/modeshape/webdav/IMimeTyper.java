package org.modeshape.webdav;

public interface IMimeTyper {

    /**
     * Detect the mime type of this object
     * 
     * @param transaction
     * @param path
     * @return the MIME type
     */
    String getMimeType( ITransaction transaction,
                        String path );
}
