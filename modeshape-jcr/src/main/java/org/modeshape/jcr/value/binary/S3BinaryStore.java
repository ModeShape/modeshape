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
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.jcr.RepositoryException;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

/**
 * Binary storage option which manages the storage of files to Amazon S3
 *
 * @author bbranan
 */
public class S3BinaryStore extends AbstractBinaryStore {

    /*
     * AWS client which provides access to Amazon S3
     */
    private AmazonS3Client s3Client = null;

    /*
     * Temporary local file cache to allow for checksum computation
     */
    private FileSystemBinaryStore fileSystemCache;

    /*
     * S3 bucket used to store and retrieve content
     */
    private String bucketName;

    /*
     * Key for storing and retrieving extracted text from S3 object user metadata
     */
    protected static final String EXTRACTED_TEXT_KEY = "extracted-text";

    /*
     * Key for storing boolean which describes if object is unused
     */
    protected static final String UNUSED_KEY = "unused";

    /**
     * Creates a binary store with a connection to Amazon S3
     *
     * @param accessKey AWS access key credential
     * @param secretKey AWS secret key credential
     * @param bucketName Name of the S3 bucket in which binary content will be stored
     * @throws BinaryStoreException if S3 connection cannot be made to verify bucket
     */
    public S3BinaryStore(String accessKey, String secretKey, String bucketName) throws BinaryStoreException {
        this(accessKey, secretKey, bucketName, null);
    }

    /**
     * Creates a binary store with a connection to Amazon S3
     *
     * @param accessKey AWS access key credential
     * @param secretKey AWS secret key credential
     * @param bucketName Name of the S3 bucket in which binary content will be stored
     * @param endPoint The S3 endpoint URL where the bucket will be accessed
     * @throws BinaryStoreException if S3 connection cannot be made to verify bucket
     */
    public S3BinaryStore(String accessKey, String secretKey, String bucketName, String endPoint) throws BinaryStoreException {
        this.bucketName = bucketName;
        this.s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));

        // Support for compatible S3 storage systems
        if(endPoint != null)
            this.s3Client.setEndpoint(endPoint);

        this.fileSystemCache = TransientBinaryStore.get();
        this.fileSystemCache.setMinimumBinarySizeInBytes(1L);

        // Ensure bucket exists
        try {
            if (!s3Client.doesBucketExist(bucketName)) {
                s3Client.createBucket(bucketName);
            }
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Creates a binary store with a connection to Amazon S3. This constructor is
     * intended for testing only.
     *
     * @param bucketName Name of the S3 bucket in which binary content will be stored
     * @param s3Client Client for communicating with Amazon S3
     */
    protected S3BinaryStore(String bucketName, AmazonS3Client s3Client) {
        this.bucketName = bucketName;
        this.s3Client = s3Client;
        this.fileSystemCache = TransientBinaryStore.get();
        this.fileSystemCache.setMinimumBinarySizeInBytes(1L);
    }

    @Override
    protected String getStoredMimeType(BinaryValue binaryValue) throws BinaryStoreException {
        try {
            String key = binaryValue.getKey().toString();
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
            return metadata.getContentType();
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    protected void storeMimeType(BinaryValue binaryValue, String mimeType) throws BinaryStoreException {
        try {
            String key = binaryValue.getKey().toString();
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
            metadata.setContentType(mimeType);

            // Update the object in place
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
            copyRequest.setNewObjectMetadata(metadata);
            s3Client.copyObject(copyRequest);
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void storeExtractedText(BinaryValue binaryValue, String extractedText)
        throws BinaryStoreException {
        // User defined metadata for S3 objects cannot exceed 2KB
        // This checks for the absolute top of that range
        if(extractedText.length() > 2000) {
            throw new BinaryStoreException("S3 objects cannot store associated data " +
                                           "that is larger than 2KB");
        }

        setS3ObjectUserProperty(binaryValue.getKey(), EXTRACTED_TEXT_KEY, extractedText);
    }

    private void setS3ObjectUserProperty(BinaryKey binaryKey, String metadataKey, String metadataValue) throws BinaryStoreException {
        try {
            String key = binaryKey.toString();
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
            Map<String, String> userMetadata = metadata.getUserMetadata();

            if(null != metadataValue && metadataValue.equals(userMetadata.get(metadataKey))) {
                return; // The key/value pair already exists in user metadata, skip update
            }

            userMetadata.put(metadataKey, metadataValue);
            metadata.setUserMetadata(userMetadata);

            // Update the object in place
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
            copyRequest.setNewObjectMetadata(metadata);
            s3Client.copyObject(copyRequest);
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public String getExtractedText(BinaryValue binaryValue) throws BinaryStoreException {
        try {
            String key = binaryValue.getKey().toString();
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
            return metadata.getUserMetadata().get(EXTRACTED_TEXT_KEY);
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public BinaryValue storeValue(InputStream stream, boolean markAsUnused) throws BinaryStoreException {
        // Cache file on the file system in order to have SHA-1 hash calculated
        BinaryValue cachedFile = fileSystemCache.storeValue(stream, markAsUnused);
        try {
            // Retrieve SHA-1 hash
            BinaryKey key = new BinaryKey(cachedFile.getKey().toString());

            // If file is NOT already in S3 storage, store it
            if(!s3Client.doesObjectExist(bucketName, key.toString())) {
                ObjectMetadata metadata = new ObjectMetadata();
                // Set Mimetype
                metadata.setContentType(fileSystemCache.getMimeType(cachedFile, key.toString()));
                // Set Unused value
                Map<String, String> userMetadata = metadata.getUserMetadata();
                userMetadata.put(UNUSED_KEY, String.valueOf(markAsUnused));
                metadata.setUserMetadata(userMetadata);
                // Store content in S3
                s3Client.putObject(bucketName, key.toString(), fileSystemCache.getInputStream(key), metadata);
            } else {
                // Set the unused value, if necessary
                if(markAsUnused) {
                    markAsUnused(Collections.singleton(key));
                } else {
                    markAsUsed(Collections.singleton(key));
                }
            }
            return new StoredBinaryValue(this, key, cachedFile.getSize());
        } catch (AmazonClientException|RepositoryException |IOException e) {
            throw new BinaryStoreException(e);
        } finally {
            // Remove cached file
            fileSystemCache.markAsUnused(Collections.singleton(cachedFile.getKey()));
            fileSystemCache.removeValuesUnusedLongerThan(1, TimeUnit.MICROSECONDS);
        }
    }

    @Override
    public InputStream getInputStream(BinaryKey key) throws BinaryStoreException {
        try {
            S3Object s3Object = s3Client.getObject(bucketName, key.toString());
            return s3Object.getObjectContent();
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void markAsUsed(Iterable<BinaryKey> keys) throws BinaryStoreException {
        for(BinaryKey key : keys) {
            setS3ObjectUserProperty(key, UNUSED_KEY, String.valueOf(false));
        }
    }

    @Override
    public void markAsUnused(Iterable<BinaryKey> keys) throws BinaryStoreException {
        for(BinaryKey key : keys) {
            setS3ObjectUserProperty(key, UNUSED_KEY, String.valueOf(true));
        }
    }

    @Override
    public void removeValuesUnusedLongerThan(long minimumAge, TimeUnit timeUnit) throws BinaryStoreException {
        Date deadline = new Date(System.currentTimeMillis() - timeUnit.toMillis(minimumAge));

        // There is no capacity in S3 to query on object properties. This must be done
        // by straight iteration, so may take a very long time for large data sets.
        try {
            for(BinaryKey key : getAllBinaryKeys()) {
                ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key.toString());
                String unused = metadata.getUserMetadata().get(UNUSED_KEY);
                if (null != unused && unused.equals(String.valueOf(true))) {
                    Date lastMod = metadata.getLastModified();
                    if (lastMod.before(deadline)) {
                        try {
                            s3Client.deleteObject(bucketName, key.toString());
                        } catch (AmazonClientException e) {
                            Logger log = Logger.getLogger(getClass());
                            log.warn(e, JcrI18n.unableToDeleteTemporaryFile, e.getMessage());
                        }
                    }
                } // Assumes that if no value is set, content is used
            }
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        try {
            final Iterator<S3ObjectSummary> objectsIterator =
                S3Objects.inBucket(s3Client, bucketName).iterator();
            // Lambda to hand back BinaryKeys rather than S3ObjectSummaries
            return () -> {
                return new Iterator<BinaryKey>() {
                    @Override
                    public boolean hasNext() {
                        return objectsIterator.hasNext();
                    }

                    @Override
                    public BinaryKey next() {
                        S3ObjectSummary object = objectsIterator.next();
                        return new BinaryKey(object.getKey());
                    }
                };
            };
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

}
