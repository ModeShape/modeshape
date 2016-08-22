/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.value.binary;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.util.IOUtils;
import com.amazonaws.util.StringInputStream;
import org.easymock.Capture;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Unit test for the S3 Binary Store.
 * All calls to S3 are handled by a mock S3 client and verified.
 *
 * @author bbranan
 */
@RunWith(EasyMockRunner.class)
public class S3BinaryStoreTest extends EasyMockSupport {

    private static final String BUCKET = "MOCK_BUCKET";
    private static final String TEST_KEY = "test-key";
    private static final String TEST_MIME = "text/plain";
    private static final String TEST_CONTENT = "test-content";

    @Mock
    private AmazonS3Client s3Client;

    @Mock
    private ObjectListing objectListing;

    @TestSubject
    private S3BinaryStore s3BinaryStore = new S3BinaryStore(BUCKET, s3Client);

    @After
    public void tearDown() {
        verifyAll();
    }

    @Test
    public void testGetStoredMimeType() throws BinaryStoreException {
        ObjectMetadata objMeta = new ObjectMetadata();
        objMeta.setContentType(TEST_MIME);
        expect(s3Client.getObjectMetadata(BUCKET, TEST_KEY)).andReturn(objMeta);

        replayAll();

        BinaryValue binaryValue = createBinaryValue(TEST_KEY, TEST_CONTENT);
        String mimeType = s3BinaryStore.getStoredMimeType(binaryValue);
        assertEquals(TEST_MIME, mimeType);
    }

    private BinaryValue createBinaryValue(String key, String content) {
        return new StoredBinaryValue(s3BinaryStore, new BinaryKey(key), content.length());
    }

    @Test
    public void testStoreMimeType() throws BinaryStoreException {
        expect(s3Client.getObjectMetadata(BUCKET, TEST_KEY))
            .andReturn(new ObjectMetadata());
        Capture<CopyObjectRequest> copyRequestCapture = Capture.newInstance();
        expect(s3Client.copyObject(capture(copyRequestCapture))).andReturn(null);

        replayAll();

        BinaryValue binaryValue = createBinaryValue(TEST_KEY, TEST_CONTENT);
        s3BinaryStore.storeMimeType(binaryValue, TEST_MIME);

        CopyObjectRequest copyRequest = copyRequestCapture.getValue();
        assertEquals(BUCKET, copyRequest.getSourceBucketName());
        assertEquals(BUCKET, copyRequest.getDestinationBucketName());
        assertEquals(TEST_KEY, copyRequest.getSourceKey());
        assertEquals(TEST_KEY, copyRequest.getDestinationKey());
        assertEquals(TEST_MIME, copyRequest.getNewObjectMetadata().getContentType());
    }

    /**
     * Ensures that an execption is thrown if an attempt is made to store an
     * extracted text value which exceeds the capacity of S3
     */
    @Test
    public void testStoreExtractedTextTooLong() throws BinaryStoreException {
        StringBuilder textBuilder = new StringBuilder();
        for(int i=0; i<2001; i++) {
            textBuilder.append("a");
        }
        String extractedText = textBuilder.toString();

        replayAll();

        BinaryValue binaryValue = createBinaryValue(TEST_KEY, TEST_CONTENT);
        try {
            s3BinaryStore.storeExtractedText(binaryValue, extractedText);
            fail("Exception expected due to long extracted text value");
        } catch(BinaryStoreException e) {
            assertNotNull(e);
        }
    }

    @Test
    public void testStoreExtractedText() throws BinaryStoreException {
        String extractedText = "text-that-has-been-extracted";

        expect(s3Client.getObjectMetadata(BUCKET, TEST_KEY))
            .andReturn(new ObjectMetadata());
        Capture<CopyObjectRequest> copyRequestCapture = Capture.newInstance();
        expect(s3Client.copyObject(capture(copyRequestCapture))).andReturn(null);

        replayAll();

        BinaryValue binaryValue = createBinaryValue(TEST_KEY, TEST_CONTENT);
        s3BinaryStore.storeExtractedText(binaryValue, extractedText);

        CopyObjectRequest copyRequest = copyRequestCapture.getValue();
        assertEquals(BUCKET, copyRequest.getSourceBucketName());
        assertEquals(BUCKET, copyRequest.getDestinationBucketName());
        assertEquals(TEST_KEY, copyRequest.getSourceKey());
        assertEquals(TEST_KEY, copyRequest.getDestinationKey());
        assertEquals(extractedText, copyRequest.getNewObjectMetadata()
                                               .getUserMetadata()
                                               .get(s3BinaryStore.EXTRACTED_TEXT_KEY));
    }

    @Test
    public void testGetExtractedText() throws BinaryStoreException {
        String extractedText = "text-that-has-been-extracted";

        ObjectMetadata objMeta = new ObjectMetadata();
        Map<String, String> userMeta = new HashMap<>();
        userMeta.put(s3BinaryStore.EXTRACTED_TEXT_KEY, extractedText);
        objMeta.setUserMetadata(userMeta);
        expect(s3Client.getObjectMetadata(BUCKET, TEST_KEY)).andReturn(objMeta);

        replayAll();

        BinaryValue binaryValue = createBinaryValue(TEST_KEY, TEST_CONTENT);
        String extractValue = s3BinaryStore.getExtractedText(binaryValue);
        assertEquals(extractedText, extractValue);
    }

    /*
     * Tests storing new content
     */
    @Test
    public void testStoreValue() throws BinaryStoreException, UnsupportedEncodingException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(false);
        Capture<ObjectMetadata> objMetaCapture = Capture.newInstance();
        expect(s3Client.putObject(eq(BUCKET), isA(String.class),
                                  isA(InputStream.class), capture(objMetaCapture)))
            .andReturn(null);

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), false);
        ObjectMetadata objMeta = objMetaCapture.getValue();
        assertEquals(String.valueOf(false),
                     objMeta.getUserMetadata().get(s3BinaryStore.UNUSED_KEY));
    }

    /*
     * Tests storing content which already exists. Ensures that the call to set the
     * markAsUnread property is made.
     */
    @Test
    public void testStoreValueExisting() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(true);
        expect(s3Client.getObjectMetadata(eq(BUCKET), isA(String.class)))
            .andReturn(new ObjectMetadata());
        ObjectMetadata objMeta = new ObjectMetadata();
        Map<String, String> userMeta = new HashMap<>();
        userMeta.put(s3BinaryStore.UNUSED_KEY, String.valueOf(true));
        objMeta.setUserMetadata(userMeta);
        Capture<CopyObjectRequest> copyRequestCapture = Capture.newInstance();
        expect(s3Client.copyObject(capture(copyRequestCapture))).andReturn(null);

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), true);
        ObjectMetadata newObjMeta = copyRequestCapture.getValue().getNewObjectMetadata();
        assertEquals(String.valueOf(true),
                     newObjMeta.getUserMetadata().get(s3BinaryStore.UNUSED_KEY));
    }

    @Test
    public void testGetInputStream() throws BinaryStoreException, IOException {
        S3Object s3Object = new S3Object();
        s3Object.setObjectContent(new StringInputStream(TEST_CONTENT));
        expect(s3Client.getObject(BUCKET, TEST_KEY)).andReturn(s3Object);

        replayAll();

        InputStream resultStream = s3BinaryStore.getInputStream(new BinaryKey(TEST_KEY));
        assertEquals(TEST_CONTENT, IOUtils.toString(resultStream));
    }

    @Test
    public void testMarkAsUsed() throws BinaryStoreException {
        ObjectMetadata objMeta = new ObjectMetadata();
        Map<String, String> userMeta = new HashMap<>();
        // Existing value of unused property set to true (so file is considered not used)
        userMeta.put(s3BinaryStore.UNUSED_KEY, String.valueOf(true));
        objMeta.setUserMetadata(userMeta);
        expect(s3Client.getObjectMetadata(eq(BUCKET), isA(String.class)))
            .andReturn(objMeta);
        Capture<CopyObjectRequest> copyRequestCapture = Capture.newInstance();
        expect(s3Client.copyObject(capture(copyRequestCapture))).andReturn(null);

        replayAll();

        s3BinaryStore.markAsUsed(Collections.singleton(new BinaryKey(TEST_KEY)));
        ObjectMetadata newObjMeta = copyRequestCapture.getValue().getNewObjectMetadata();
        assertEquals(String.valueOf(false),
                     newObjMeta.getUserMetadata().get(s3BinaryStore.UNUSED_KEY));
    }

    @Test
    public void testMarkAsUnused() throws BinaryStoreException {
        ObjectMetadata objMeta = new ObjectMetadata();
        Map<String, String> userMeta = new HashMap<>();
        // Existing value of unused property set to false (so file is considered used)
        userMeta.put(s3BinaryStore.UNUSED_KEY, String.valueOf(false));
        objMeta.setUserMetadata(userMeta);
        expect(s3Client.getObjectMetadata(eq(BUCKET), isA(String.class)))
            .andReturn(objMeta);
        Capture<CopyObjectRequest> copyRequestCapture = Capture.newInstance();
        expect(s3Client.copyObject(capture(copyRequestCapture))).andReturn(null);

        replayAll();

        s3BinaryStore.markAsUnused(Collections.singleton(new BinaryKey(TEST_KEY)));
        ObjectMetadata newObjMeta = copyRequestCapture.getValue().getNewObjectMetadata();
        assertEquals(String.valueOf(true),
                     newObjMeta.getUserMetadata().get(s3BinaryStore.UNUSED_KEY));
    }

    /*
     * Tests setting the unused property given that the existing value is already set
     * to the preferred value. No update calls should occur.
     */
    @Test
    public void testMarkAsUnusedNoChangeNeeded() throws BinaryStoreException {
        ObjectMetadata objMeta = new ObjectMetadata();
        Map<String, String> userMeta = new HashMap<>();
        // Existing value of unused property set to true, so file is already considered
        // to be not used. No change should be needed.
        userMeta.put(s3BinaryStore.UNUSED_KEY, String.valueOf(true));
        objMeta.setUserMetadata(userMeta);
        expect(s3Client.getObjectMetadata(eq(BUCKET), isA(String.class)))
            .andReturn(objMeta);

        replayAll();

        s3BinaryStore.markAsUnused(Collections.singleton(new BinaryKey(TEST_KEY)));
    }

    @Test
    public void testRemoveValuesUnusedLongerThan() throws BinaryStoreException {
        String usedObjectKey = "used-object";
        String unusedNewKey = "unused-new";
        String unusedOldKey = "unused-old";

        // List of objects, one with unused=false,
        // one with unused=true but updated within the hour,
        // one with unused=true and last updated over a week ago (which should be removed)
        List<S3ObjectSummary> objectList = new ArrayList<>();
        S3ObjectSummary usedObject = new S3ObjectSummary();
        usedObject.setKey(usedObjectKey);
        objectList.add(usedObject);
        S3ObjectSummary unusedNewObject = new S3ObjectSummary();
        unusedNewObject.setKey(unusedNewKey);
        objectList.add(unusedNewObject);
        S3ObjectSummary unusedOldObject = new S3ObjectSummary();
        unusedOldObject.setKey(unusedOldKey);
        objectList.add(unusedOldObject);

        // Expect request to get object list
        expect(s3Client.listObjects(isA(ListObjectsRequest.class)))
            .andReturn(objectListing);
        expect(objectListing.getObjectSummaries()).andReturn(objectList);
        expect(objectListing.isTruncated()).andReturn(false);

        // Request for used object
        ObjectMetadata usedObjMeta = new ObjectMetadata();
        usedObjMeta.setUserMetadata(
            Collections.singletonMap(s3BinaryStore.UNUSED_KEY, String.valueOf(false)));
        usedObjMeta.setLastModified(new Date());
        expect(s3Client.getObjectMetadata(BUCKET, usedObjectKey)).andReturn(usedObjMeta);

        // Request for unused object with recent update
        ObjectMetadata unusedNewObjMeta = new ObjectMetadata();
        unusedNewObjMeta.setUserMetadata(
            Collections.singletonMap(s3BinaryStore.UNUSED_KEY, String.valueOf(true)));
        unusedNewObjMeta.setLastModified(new Date());
        expect(s3Client.getObjectMetadata(BUCKET, unusedNewKey)).andReturn(unusedNewObjMeta);

        // Request for unused object with old update
        ObjectMetadata unusedOldObjMeta = new ObjectMetadata();
        unusedOldObjMeta.setUserMetadata(
            Collections.singletonMap(s3BinaryStore.UNUSED_KEY, String.valueOf(true)));
        // Last modified 8 days ago
        unusedOldObjMeta.setLastModified(new Date(System.currentTimeMillis() - 691200000));
        expect(s3Client.getObjectMetadata(BUCKET, unusedOldKey)).andReturn(unusedOldObjMeta);

        // Expect one delete
        s3Client.deleteObject(BUCKET, unusedOldKey);
        expectLastCall();

        replayAll();

        s3BinaryStore.removeValuesUnusedLongerThan(7, TimeUnit.DAYS);
    }

    @Test
    public void testGetAllBinaryKeys() throws BinaryStoreException {
        List<S3ObjectSummary> objectList = new ArrayList<>();
        for(int i=0; i< 101; i++) {
            S3ObjectSummary object = new S3ObjectSummary();
            object.setKey(String.valueOf(i));
            objectList.add(object);
        }
        // First request to get objects (incomplete list returned)
        expect(s3Client.listObjects(isA(ListObjectsRequest.class)))
            .andReturn(objectListing);
        expect(objectListing.getObjectSummaries()).andReturn(objectList);
        expect(objectListing.isTruncated()).andReturn(true);
        // Second request to get more objects
        expect(s3Client.listNextBatchOfObjects(objectListing))
            .andReturn(objectListing);
        expect(objectListing.getObjectSummaries()).andReturn(objectList);
        expect(objectListing.isTruncated()).andReturn(false);

        replayAll();

        Iterable<BinaryKey> allKeys = s3BinaryStore.getAllBinaryKeys();
        int keyCount = 0;
        for(BinaryKey key : allKeys) {
            keyCount++;
        }
        assertEquals(202, keyCount); // Expecting two sets of 101 objects
    }

}
