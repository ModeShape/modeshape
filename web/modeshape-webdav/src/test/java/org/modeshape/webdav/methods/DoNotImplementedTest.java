package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.WebdavStatus;

@SuppressWarnings( "synthetic-access" )
public class DoNotImplementedTest extends AbstractWebDAVTest {

    @Test
    public void testDoNotImplementedIfReadOnlyTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(READ_ONLY);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testDoNotImplementedIfReadOnlyFalse() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockReq).getMethod();
                will(returnValue("notImplementedMethod"));
                one(mockRes).sendError(WebdavStatus.SC_NOT_IMPLEMENTED);
            }
        });

        DoNotImplemented doNotImplemented = new DoNotImplemented(!READ_ONLY);
        doNotImplemented.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
