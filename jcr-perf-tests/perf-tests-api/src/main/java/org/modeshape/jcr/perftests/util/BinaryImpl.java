package org.modeshape.jcr.perftests.util;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

/**
 * @author Horia Chiorean
 */
public final class BinaryImpl implements Binary {

    private byte[] randomBytes;

    public BinaryImpl(int size) {
        this.randomBytes = new byte[size];
        new Random().nextBytes(randomBytes);
    }

    @Override
    public InputStream getStream() throws RepositoryException {
        return new ByteArrayInputStream(randomBytes);
    }

    @Override
    public int read( byte[] b, long position ) throws IOException, RepositoryException {
        System.arraycopy(randomBytes, (int) position, b, 0, b.length); //should never use data large enough for the cast to be a problem
        return b.length;
    }

    @Override
    public long getSize() throws RepositoryException {
        return randomBytes.length;
    }

    @Override
    public void dispose() {
        randomBytes = null;
    }
}
