package org.modeshape.jcr.perftests.util;

import javax.jcr.Binary;
import javax.jcr.RepositoryException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class used by tests when working with {@link javax.jcr.Binary} implementations
 *
 * @author Horia Chiorean
 */
public final class BinaryHelper {

    private BinaryHelper() {
    }

    /**
     * Asserts that the given binary source has the expected size (in bytes). The operation reads the data from the binary
     * instance into a byte array and checks the size of the byte array
     * @param source a <code>Binary</code> instance.
     * @param expectedSize the expected size of the binary
     */
    public static void assertExpectedSize( Binary source, int expectedSize ) throws RepositoryException, IOException {
        ByteArrayOutputStream byteArrayOutput = new ByteArrayOutputStream();
        InputStream inputStream = source.getStream();
        try {
            byte[] buff = new byte[1000];
            int available;
            while ((available = inputStream.read(buff)) != -1) {
                byteArrayOutput.write(buff, 0, available);
            }
            assert byteArrayOutput.size() == expectedSize;
        } finally {
            inputStream.close();
            byteArrayOutput.close();
        }
    }
}
