package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;
import org.springframework.mock.web.DelegatingServletInputStream;

@SuppressWarnings( "synthetic-access" )
public class DoMoveTest extends AbstractWebDAVTest {
    private static final String OVERWRITE_PATH = DEST_COLLECTION_PATH + "/sourceFolder";

    @Test
    public void testMovingOfFileOrFolderIfReadOnlyIsTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfDestinationNotPresent() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("serverName"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfDestinationIsPresentAndOverwriteFalse() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfDestinationIsPresentAndOverwriteTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaFileIfSourceNotPresent() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingIfSourcePathEqualsDestinationPath() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                oneOf(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaCollectionIfDestinationIsNotPresent() throws Exception {
        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = null;

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_CREATED);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).createFolder(mockTransaction, DEST_COLLECTION_PATH);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(null));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] {"sourceFile"}));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH + "/sourceFile");
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");

                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_COLLECTION_PATH + "/sourceFile");
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction,
                                                  DEST_COLLECTION_PATH + "/sourceFile",
                                                  resourceStream,
                                                  null,
                                                  null);
                will(returnValue(RESOURCE_LENGTH));

                StoredObject movedSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");
                will(returnValue(movedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] {"sourceFile"}));

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
                oneOf(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteFalse() throws Exception {

        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }

    @Test
    public void testMovingOfaCollectionIfDestinationIsPresentAndOverwriteTrue() throws Exception {
        mockery.checking(new Expectations() {
            {
                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(OVERWRITE_PATH));

                oneOf(mockReq).getServerName();
                will(returnValue("server_name"));

                oneOf(mockReq).getContextPath();
                will(returnValue(""));

                oneOf(mockReq).getPathInfo();
                will(returnValue(OVERWRITE_PATH));

                oneOf(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                oneOf(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                oneOf(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                oneOf(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                oneOf(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH);
                will(returnValue(destCollectionSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH);
                will(returnValue(destCollectionSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, OVERWRITE_PATH);
                will(returnValue(new String[] {"destFile"}));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH + "/destFile");
                will(returnValue(destFileSo));

                oneOf(mockStore).removeObject(mockTransaction, OVERWRITE_PATH + "/destFile");
                oneOf(mockStore).removeObject(mockTransaction, OVERWRITE_PATH);
                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).createFolder(mockTransaction, OVERWRITE_PATH);

                oneOf(mockReq).getHeader("Depth");
                will(returnValue(null));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                // make sure it's a new array reference :(
                will(returnValue(new String[] {"sourceFile"}));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).createResource(mockTransaction, OVERWRITE_PATH + "/sourceFile");

                DelegatingServletInputStream resourceStream = resourceRequestStream();
                oneOf(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(resourceStream));
                oneOf(mockStore).setResourceContent(mockTransaction, OVERWRITE_PATH + "/sourceFile", resourceStream, null, null);

                StoredObject movedSo = initFileStoredObject(RESOURCE_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH + "/sourceFile");
                will(returnValue(movedSo));

                oneOf(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                oneOf(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] {"sourceFile"}));

                oneOf(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                oneOf(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
                oneOf(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH);
            }
        });

        ResourceLocks resLocks = new ResourceLocks();
        DoDelete doDelete = new DoDelete(mockStore, resLocks, !READ_ONLY);
        DoCopy doCopy = new DoCopy(mockStore, resLocks, doDelete, !READ_ONLY);

        DoMove doMove = new DoMove(resLocks, doDelete, doCopy, !READ_ONLY);

        doMove.execute(mockTransaction, mockReq, mockRes);

        mockery.assertIsSatisfied();
    }
}
