package org.modeshape.webdav.methods;

import java.io.IOException;
import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.exceptions.LockFailedException;
import org.modeshape.webdav.locking.ResourceLocks;

@SuppressWarnings( "synthetic-access" )
public class DoOptionsTest extends AbstractWebDAVTest {

    @Test
    public void testOptionsOnExistingNode() throws IOException, LockFailedException {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                one(mockRes).addHeader("DAV", "1, 2");

                StoredObject indexSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                one(mockRes).addHeader("Allow",
                                       "OPTIONS, GET, HEAD, POST, DELETE, TRACE, PROPPATCH, COPY, "
                                       + "MOVE, LOCK, UNLOCK, PROPFIND");
                one(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testOptionsOnNonExistingNode() throws IOException, LockFailedException {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue("/index.html"));

                one(mockRes).addHeader("DAV", "1, 2");

                StoredObject indexSo = null;

                one(mockStore).getStoredObject(mockTransaction, "/index.html");
                will(returnValue(indexSo));

                one(mockRes).addHeader("Allow", "OPTIONS, MKCOL, PUT");

                one(mockRes).addHeader("MS-Author-Via", "DAV");
            }
        });

        DoOptions doOptions = new DoOptions(mockStore, new ResourceLocks());
        doOptions.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

}
