package org.modeshape.jcr.value.binary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.SecureHash;
import org.modeshape.jcr.mimetype.MimeTypeDetector;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * A subclass of {@link UrlBinaryValue} to lazily compute a contentBased Hash when the key is a URI based Hash
 */
public class FileUrlBinaryValue extends UrlBinaryValue {
    private static final long serialVersionUID = 1L;

    private File file;
    private BinaryKey hash;

    public FileUrlBinaryValue( String sha1,
                           String sourceName,
                           URL content,
                           long size,
                           String nameHint,
                           MimeTypeDetector mimeTypeDetector,
                           File file) {
        super(sha1, sourceName, content, size, nameHint, mimeTypeDetector);
        this.file = file;
    }
    
    @Override
    public byte[] getHash() {
        generateHash();
        return hash.toBytes();
    }

    @Override
    public String getHexHash() {
        generateHash();
        return hash.toString();
    }
    
    private void generateHash() {
        if (hash==null) {
            try { 
                byte[] hashBytes = SecureHash.getHash(SecureHash.Algorithm.SHA_1, file);
                this.hash = new BinaryKey(hashBytes);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
