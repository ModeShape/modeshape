package org.modeshape.webdav;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import junit.framework.Assert;
import org.jmock.Mockery;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.util.FileUtil;
import org.modeshape.webdav.locking.IResourceLocks;
import org.modeshape.webdav.locking.LockedObject;
import org.modeshape.webdav.locking.ResourceLocks;
import org.springframework.mock.web.DelegatingServletInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;

public abstract class AbstractWebDAVTest extends Assert {

    protected static final boolean READ_ONLY = true;

    protected static final int TEMP_TIMEOUT = 10;
    protected static final boolean TEMPORARY = true;

    protected static final byte[] RESOURCE_CONTENT = new byte[] { '<', 'h', 'e', 'l', 'l', 'o', '/', '>' };
    protected static final long RESOURCE_LENGTH = RESOURCE_CONTENT.length;

    private static final String EXCLUSIVE_LOCK_REQUEST = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:lockinfo xmlns:D='DAV:'>" + "<D:lockscope><D:exclusive/></D:lockscope>" + "<D:locktype><D:write/></D:locktype>" + "<D:owner><D:href>I'am the Lock Owner</D:href></D:owner>" + "</D:lockinfo>";

    private static final String SHARED_LOCK_REQUEST = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>" + "<D:lockinfo xmlns:D='DAV:'>" + "<D:lockscope><D:shared/></D:lockscope>" + "<D:locktype><D:write/></D:locktype>" + "<D:owner><D:href>I'am the Lock Owner</D:href></D:owner>" + "</D:lockinfo>";

    protected static final String TMP_FOLDER = "/tmp/tests";
    protected static final String SOURCE_COLLECTION_PATH = TMP_FOLDER + "/sourceFolder";
    protected static final String DEST_COLLECTION_PATH = TMP_FOLDER + "/destFolder";
    protected static final String SOURCE_FILE_PATH = SOURCE_COLLECTION_PATH + "/sourceFile";
    protected static final String DEST_FILE_PATH = DEST_COLLECTION_PATH + "/destFile";

    protected static final String OWNER = "owner";

    protected static final String INCLUDE_REQUEST_URI_ATTRIBUTE = "javax.servlet.include.request_uri";

    private static final String OUTPUT_ROOT_FOLDER = "target/modeshape-webdav";

    protected Mockery mockery;
    protected IWebdavStore mockStore;
    protected HttpServletRequest mockReq;
    protected HttpServletResponse mockRes;
    protected ITransaction mockTransaction;
    protected IMimeTyper mockMimeTyper;
    protected IResourceLocks mockResourceLocks;

    protected PrintWriter getPrintWriter() throws IOException {
        File outputRootFolder = new File(OUTPUT_ROOT_FOLDER);
        if (!outputRootFolder.exists() || !outputRootFolder.isDirectory()) {
            outputRootFolder.mkdir();
        }
        File outputFile = new File(OUTPUT_ROOT_FOLDER + "/webdav_output_" + System.currentTimeMillis() + ".xml");
        outputFile.createNewFile();
        return new PrintWriter(outputFile);
    }

    protected DelegatingServletInputStream exclusiveLockRequestStream() {
        return new DelegatingServletInputStream(new ByteArrayInputStream(EXCLUSIVE_LOCK_REQUEST.getBytes()));
    }

    protected DelegatingServletInputStream sharedLockRequestStream() {
        return new DelegatingServletInputStream(new ByteArrayInputStream(SHARED_LOCK_REQUEST.getBytes()));
    }

    protected DelegatingServletInputStream resourceRequestStream() {
        return new DelegatingServletInputStream(new ByteArrayInputStream(RESOURCE_CONTENT));
    }

    @Before
    public void setupMocks() {
        mockery = new Mockery();
        mockStore = mockery.mock(IWebdavStore.class);
        mockReq = mockery.mock(HttpServletRequest.class);
        mockRes = mockery.mock(HttpServletResponse.class);
        mockTransaction = mockery.mock(ITransaction.class);
        mockMimeTyper = mockery.mock(IMimeTyper.class);
        mockResourceLocks = mockery.mock(IResourceLocks.class);
    }

    @After
    public void tearDownAfterClass() {
        FileUtil.delete(OUTPUT_ROOT_FOLDER);
    }

    public StoredObject initFolderStoredObject() {
        return initStoredObject(true, null);
    }

    public StoredObject initFileStoredObject( byte[] resourceContent ) {
        return initStoredObject(false, resourceContent);
    }

    private StoredObject initStoredObject( boolean isFolder,
                                           byte[] resourceContent ) {
        StoredObject so = new StoredObject();
        so.setFolder(isFolder);
        so.setCreationDate(new Date());
        so.setLastModified(new Date());
        if (!isFolder) {
            // so.setResourceContent(resourceContent);
            so.setResourceLength(resourceContent.length);
        } else {
            so.setResourceLength(0L);
        }

        return so;
    }

    public StoredObject initLockNullStoredObject() {
        StoredObject so = new StoredObject();
        so.setNullResource(true);
        so.setFolder(false);
        so.setCreationDate(null);
        so.setLastModified(null);
        // so.setResourceContent(null);
        so.setResourceLength(0);

        return so;
    }

    public LockedObject initLockNullLockedObject( ResourceLocks resLocks,
                                                  String path ) {
        LockedObject lo = new LockedObject(resLocks, path, false);
        lo.setExclusive(true);
        return lo;
    }
}
