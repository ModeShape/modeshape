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

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.SetObjectTaggingResult;
import com.amazonaws.services.s3.model.Tag;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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
        objMeta.addUserMetadata(S3BinaryStore.USER_MIME_TYPE_KEY, String.valueOf(true));
        expect(s3Client.getObjectMetadata(BUCKET, TEST_KEY)).andReturn(objMeta);

        replayAll();

        BinaryValue binaryValue = createBinaryValue(TEST_KEY, TEST_CONTENT);
        String mimeType = s3BinaryStore.getStoredMimeType(binaryValue);
        assertEquals(TEST_MIME, mimeType);
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
     * Ensures that an exception is thrown if an attempt is made to store an
     * extracted text value which exceeds the capacity of S3
     */
    @Test
    public void testStoreExtractedTextTooLong() throws BinaryStoreException {
        StringBuilder textBuilder = new StringBuilder();
        for (int i = 0; i < 2001; i++) {
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

    @Test
    public void testStoreValueWhenAbsent() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(false);
        expect(s3Client.putObject(eq(BUCKET), isA(String.class),
                isA(InputStream.class), isA(ObjectMetadata.class)))
                .andReturn(null);

        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), false);

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(false));
    }

    @Test
    public void testStoreValueWhenExisting() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(true);

        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), false);

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(false));
    }

    @Test
    public void testStoreValueWhenExistingUnused() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(true);

        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(true)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), false);

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(false));
    }

    @Test
    public void testStoreUnusedValueWhenAbsent() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(false);
        expect(s3Client.putObject(eq(BUCKET), isA(String.class),
                isA(InputStream.class), isA(ObjectMetadata.class)))
                .andReturn(null);

        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), true);

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(true));
    }

    @Test
    public void testStoreUnusedValueWhenExisting() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(true);

        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(false)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), true);

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(true));
    }

    @Test
    public void testStoreUnusedValueWhenExistingUnused() throws BinaryStoreException, IOException {
        String valueToStore = "value-to-store";

        expect(s3Client.doesObjectExist(eq(BUCKET), isA(String.class))).andReturn(true);

        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(true)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        replayAll();

        s3BinaryStore.storeValue(new StringInputStream(valueToStore), true);
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
        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(true)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.markAsUsed(Collections.singleton(new BinaryKey(TEST_KEY)));

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(false));
    }

    @Test
    public void testMarkAsUnused() throws BinaryStoreException {
        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(false)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        Capture<SetObjectTaggingRequest> setTaggingRequestCapture = Capture.newInstance();
        expect(s3Client.setObjectTagging(capture(setTaggingRequestCapture)))
                .andReturn(new SetObjectTaggingResult());

        replayAll();

        s3BinaryStore.markAsUnused(Collections.singleton(new BinaryKey(TEST_KEY)));

        SetObjectTaggingRequest setTaggingRequest = setTaggingRequestCapture.getValue();
        assertUnusedTagInSetTaggingRequest(setTaggingRequest, String.valueOf(true));
    }

    @Test
    public void testMarkAsUsedNoChangeNeeded() throws BinaryStoreException {
        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(false)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        replayAll();

        s3BinaryStore.markAsUsed(Collections.singleton(new BinaryKey(TEST_KEY)));
    }

    @Test
    public void testMarkAsUnusedNoChangeNeeded() throws BinaryStoreException {
        Capture<GetObjectTaggingRequest> getTaggingRequestCapture = Capture.newInstance();
        GetObjectTaggingResult getTaggingResult = new GetObjectTaggingResult(new ArrayList<>());
        getTaggingResult.getTagSet().add(new Tag(S3BinaryStore.UNUSED_TAG_KEY, String.valueOf(true)));
        expect(s3Client.getObjectTagging(capture(getTaggingRequestCapture)))
                .andReturn(getTaggingResult);

        replayAll();

        s3BinaryStore.markAsUnused(Collections.singleton(new BinaryKey(TEST_KEY)));
    }

    @Test
    public void testGetAllBinaryKeys() throws BinaryStoreException {
        List<S3ObjectSummary> objectList = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
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
        for (BinaryKey key : allKeys) {
            keyCount++;
        }
        assertEquals(202, keyCount); // Expecting two sets of 101 objects
    }

    private void assertUnusedTagInSetTaggingRequest(SetObjectTaggingRequest setTaggingRequest, String tagValue) {
        if (tagValue == null) {
            assertTrue(setTaggingRequest.getTagging().getTagSet().stream().noneMatch(
                    tag -> tag.getKey().equals(S3BinaryStore.UNUSED_TAG_KEY)));
        } else {
            assertTrue(setTaggingRequest.getTagging().getTagSet().stream().anyMatch(
                    tag -> tag.getKey().equals(S3BinaryStore.UNUSED_TAG_KEY) &&
                            tag.getValue().equals(String.valueOf(tagValue))));
        }
    }

    private BinaryValue createBinaryValue(String key, String content) {
        return new StoredBinaryValue(s3BinaryStore, new BinaryKey(key), content.length());
    }
}
