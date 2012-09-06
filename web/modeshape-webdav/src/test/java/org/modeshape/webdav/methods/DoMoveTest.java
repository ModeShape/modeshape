package org.modeshape.webdav.methods;

import org.jmock.Expectations;
import org.junit.Test;
import org.modeshape.webdav.AbstractWebDAVTest;
import org.modeshape.webdav.StoredObject;
import org.modeshape.webdav.WebdavStatus;
import org.modeshape.webdav.locking.ResourceLocks;
import org.springframework.mock.web.DelegatingServletInputStream;

public class DoMoveTest extends AbstractWebDAVTest {
    private static final String OVERWRITE_PATH = DEST_COLLECTION_PATH + "/sourceFolder";

    @Test
    public void testMovingOfFileOrFolderIfReadOnlyIsTrue() throws Exception {

        mockery.checking(new Expectations() {
            {
                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("serverName"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("/servletPath"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = null;

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);

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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockStore).removeObject(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, DEST_FILE_PATH);

                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, DEST_FILE_PATH, resourceStream, null, null);
                will(returnValue(RESOURCE_LENGTH));

                one(mockStore).getStoredObject(mockTransaction, DEST_FILE_PATH);
                will(returnValue(destFileSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_FILE_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceFileSo = null;

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockRes).sendError(WebdavStatus.SC_NOT_FOUND);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_FILE_PATH));

                one(mockRes).sendError(WebdavStatus.SC_FORBIDDEN);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = null;

                one(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(destCollectionSo));

                one(mockRes).setStatus(WebdavStatus.SC_CREATED);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                one(mockStore).createFolder(mockTransaction, DEST_COLLECTION_PATH);

                one(mockReq).getHeader("Depth");
                will(returnValue(null));

                one(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] { "sourceFile" }));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH + "/sourceFile");
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");

                one(mockStore).getResourceContent(mockTransaction, SOURCE_COLLECTION_PATH + "/sourceFile");
                DelegatingServletInputStream resourceStream = resourceRequestStream();
                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile", resourceStream, null,
                                                  null);
                will(returnValue(RESOURCE_LENGTH));

                StoredObject movedSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH + "/sourceFile");
                will(returnValue(movedSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                one(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] { "sourceFile" }));

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
                one(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(DEST_COLLECTION_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("F"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, DEST_COLLECTION_PATH);
                will(returnValue(destCollectionSo));

                one(mockRes).sendError(WebdavStatus.SC_PRECONDITION_FAILED);
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
                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                exactly(2).of(mockReq).getHeader("Destination");
                will(returnValue(OVERWRITE_PATH));

                one(mockReq).getServerName();
                will(returnValue("server_name"));

                one(mockReq).getContextPath();
                will(returnValue(""));

                one(mockReq).getPathInfo();
                will(returnValue(OVERWRITE_PATH));

                one(mockReq).getServletPath();
                will(returnValue("servlet_path"));

                one(mockReq).getAttribute(INCLUDE_REQUEST_URI_ATTRIBUTE);
                will(returnValue(null));

                one(mockReq).getPathInfo();
                will(returnValue(SOURCE_COLLECTION_PATH));

                one(mockReq).getHeader("Overwrite");
                will(returnValue("T"));

                StoredObject sourceCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                StoredObject destCollectionSo = initFolderStoredObject();

                one(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH);
                will(returnValue(destCollectionSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH);
                will(returnValue(destCollectionSo));

                one(mockStore).getChildrenNames(mockTransaction, OVERWRITE_PATH);
                will(returnValue(new String[] { "destFile" }));

                StoredObject destFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH + "/destFile");
                will(returnValue(destFileSo));

                one(mockStore).removeObject(mockTransaction, OVERWRITE_PATH + "/destFile");
                one(mockStore).removeObject(mockTransaction, OVERWRITE_PATH);
                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                one(mockStore).createFolder(mockTransaction, OVERWRITE_PATH);

                one(mockReq).getHeader("Depth");
                will(returnValue(null));

                one(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                //make sure it's a new array reference :(
                will(returnValue(new String[] { "sourceFile" }));

                StoredObject sourceFileSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).createResource(mockTransaction, OVERWRITE_PATH + "/sourceFile");

                DelegatingServletInputStream resourceStream = resourceRequestStream();
                one(mockStore).getResourceContent(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(resourceStream));
                one(mockStore).setResourceContent(mockTransaction, OVERWRITE_PATH + "/sourceFile", resourceStream, null, null);

                StoredObject movedSo = initFileStoredObject(RESOURCE_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, OVERWRITE_PATH + "/sourceFile");
                will(returnValue(movedSo));

                one(mockRes).setStatus(WebdavStatus.SC_NO_CONTENT);

                one(mockStore).getStoredObject(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(sourceCollectionSo));

                one(mockStore).getChildrenNames(mockTransaction, SOURCE_COLLECTION_PATH);
                will(returnValue(new String[] { "sourceFile" }));

                one(mockStore).getStoredObject(mockTransaction, SOURCE_FILE_PATH);
                will(returnValue(sourceFileSo));

                one(mockStore).removeObject(mockTransaction, SOURCE_FILE_PATH);
                one(mockStore).removeObject(mockTransaction, SOURCE_COLLECTION_PATH);
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
