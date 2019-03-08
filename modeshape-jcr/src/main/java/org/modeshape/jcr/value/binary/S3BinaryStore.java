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

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.iterable.S3Objects;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.CopyObjectRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingRequest;
import com.amazonaws.services.s3.model.GetObjectTaggingResult;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.ObjectTagging;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.SetObjectTaggingRequest;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.value.BinaryKey;
import org.modeshape.jcr.value.BinaryValue;

import javax.jcr.RepositoryException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Binary storage option which manages the storage of files to Amazon S3
 *
 * @author bbranan
 */
public class S3BinaryStore extends AbstractBinaryStore {
    /*
     * Key for storing and retrieving extracted text from S3 object user metadata
     */
    protected static final String EXTRACTED_TEXT_KEY = "extracted-text";

    /*
     * Key for storing boolean which describes if a MIME type has been explicitly set
     */
    protected static final String USER_MIME_TYPE_KEY = "user-mime-type";

    /**
     * Metadata key for storing boolean which describes if object is unused.
     * @see #migrateUnusedMetadataToTags()
     */
    @Deprecated
    protected static final String UNUSED_KEY = "unused";

    /**
     * Tag key for storing boolean which describes if object is unused.
     * @since Modeshape 5.5
     */
    protected static final String UNUSED_TAG_KEY = "modeshape.unused";
    
    /**
     * S3 {@link Rule} ID to delete unused objects.
     * @since Modeshape 5.5
     */
    protected static final String UNUSED_RULE = "modeshape.unused";

    /*
     * AWS client which provides access to Amazon S3 (non-final for EasyMock injection).
     */
    private AmazonS3 s3Client;

    /*
     * Temporary local file cache to allow for checksum computation
     */
    private final FileSystemBinaryStore fileSystemCache;

    /*
     * S3 bucket used to store and retrieve content
     */
    private final String bucketName;

    private final boolean deleteUnusedNatively;

    private boolean lifecycleUpdated = false;

    /**
     * Creates a binary store with a connection to Amazon S3.
     *
     * @param accessKey AWS access key credential
     * @param secretKey AWS secret key credential
     * @param bucketName Name of the S3 bucket in which binary content will be stored
     * @throws BinaryStoreException if S3 connection cannot be made to verify bucket
     */
    public S3BinaryStore(String accessKey, String secretKey, String bucketName) throws BinaryStoreException {
        this(accessKey, secretKey, bucketName, null, null);
    }

    /**
     * Creates a binary store with a connection to Amazon S3.
     *
     * @param accessKey AWS access key credential
     * @param secretKey AWS secret key credential
     * @param bucketName Name of the S3 bucket in which binary content will be stored
     * @param endPoint The S3 endpoint URL where the bucket will be accessed
     * @throws BinaryStoreException if S3 connection cannot be made to verify bucket
     */
    public S3BinaryStore(String accessKey, String secretKey, String bucketName, String endPoint) throws BinaryStoreException {
        this(accessKey, secretKey, bucketName, endPoint, null);
    }

    /**
     * Creates a binary store with a connection to Amazon S3.
     *
     * @param accessKey AWS access key credential
     * @param secretKey AWS secret key credential
     * @param bucketName Name of the S3 bucket in which binary content will be stored
     * @param endPoint The S3 endpoint URL where the bucket will be accessed
     * @param deleteUnusedNatively whether to use S3 lifecycle settings to handle deletion of unused objects
     * @throws BinaryStoreException if S3 connection cannot be made to verify bucket
     * @since ModeShape 5.5
     */
    public S3BinaryStore( String accessKey,
                          String secretKey,
                          String bucketName,
                          String endPoint,
                          Boolean deleteUnusedNatively )
        throws BinaryStoreException {
        this.bucketName = bucketName;
        this.deleteUnusedNatively = Boolean.TRUE.equals(deleteUnusedNatively);

        AWSCredentialsProvider credentialsProvider;
        if (accessKey == null && secretKey == null) {
            credentialsProvider = new DefaultAWSCredentialsProviderChain();
        } else {
            credentialsProvider = new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        }
        this.s3Client = new AmazonS3Client(credentialsProvider);

        // Support for compatible S3 storage systems
        if (endPoint != null) {
            this.s3Client.setEndpoint(endPoint);
        }

        this.fileSystemCache = TransientBinaryStore.get();
        this.fileSystemCache.setMinimumBinarySizeInBytes(0L);

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
     * @param deleteUnusedNatively whether to use S3 lifecycle settings to handle deletion of unused objects
     */
    protected S3BinaryStore(String bucketName, AmazonS3 s3Client, boolean deleteUnusedNatively) {
        this.bucketName = bucketName;
        this.deleteUnusedNatively = deleteUnusedNatively;
        this.s3Client = s3Client;
        this.fileSystemCache = TransientBinaryStore.get();
        this.fileSystemCache.setMinimumBinarySizeInBytes(1L);
    }

    @Override
    protected String getStoredMimeType(BinaryValue binaryValue) throws BinaryStoreException {
        try {
            return Optional.of(s3Client.getObjectMetadata(bucketName, binaryValue.getKey().toString()))
                    .filter(m -> Boolean.parseBoolean(m.getUserMetadata().get(USER_MIME_TYPE_KEY)))
                    .map(ObjectMetadata::getContentType)
                    .orElse(null);
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
            metadata.addUserMetadata(USER_MIME_TYPE_KEY, String.valueOf(true));

            // Update the object in place
            CopyObjectRequest copyRequest = new CopyObjectRequest(bucketName, key, bucketName, key);
            copyRequest.setNewObjectMetadata(metadata);
            s3Client.copyObject(copyRequest);
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public void storeExtractedText(BinaryValue binaryValue, String extractedText) throws BinaryStoreException {
        // User defined metadata for S3 objects cannot exceed 2KB
        // This checks for the absolute top of that range
        if (extractedText.length() > 2000) {
            throw new BinaryStoreException("S3 objects cannot store associated data that is larger than 2KB");
        }
        setS3ObjectUserProperty(binaryValue.getKey().toString(), EXTRACTED_TEXT_KEY, extractedText);
    }

    private void setS3ObjectUserProperty(String key, String metadataKey, String metadataValue) throws BinaryStoreException {
        try {
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, key);
            Map<String, String> userMetadata = metadata.getUserMetadata();

            if (null != metadataValue && metadataValue.equals(userMetadata.get(metadataKey))) {
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
        // File is marked as used (unused=false)
        BinaryValue cachedFile = fileSystemCache.storeValue(stream, false);
        try {
            // Retrieve SHA-1 hash
            BinaryKey key = new BinaryKey(cachedFile.getKey().toString());

            // If file is NOT already in S3 storage, store it
            if (!s3Client.doesObjectExist(bucketName, key.toString())) {
                ObjectMetadata metadata = new ObjectMetadata();
                metadata.setContentLength(cachedFile.getSize());
                // Set Mimetype
                metadata.setContentType(fileSystemCache.getMimeType(cachedFile, key.toString()));
                // Store content in S3
                s3Client.putObject(bucketName, key.toString(), fileSystemCache.getInputStream(key), metadata);
            }

            // Set the unused value, if necessary
            if (markAsUnused) {
                markAsUnused(Collections.singleton(key));
            } else {
                markAsUsed(Collections.singleton(key));
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
        for (BinaryKey key : keys) {
            setS3ObjectTag(key.toString(), UNUSED_TAG_KEY, String.valueOf(false));
        }
    }

    @Override
    public void markAsUnused(Iterable<BinaryKey> keys) throws BinaryStoreException {
        for (BinaryKey key : keys) {
            setS3ObjectTag(key.toString(), UNUSED_TAG_KEY, String.valueOf(true));
        }
    }

    /**
     * Sets a tag on a S3 object, potentially overwriting the existing value.
     * @param objectKey 
     * @param tagKey 
     * @param tagValue 
     * @throws BinaryStoreException 
     */
    private void setS3ObjectTag(String objectKey, String tagKey, String tagValue) throws BinaryStoreException {
        try {
            GetObjectTaggingRequest getTaggingRequest = new GetObjectTaggingRequest(bucketName, objectKey);
            GetObjectTaggingResult getTaggingResult = s3Client.getObjectTagging(getTaggingRequest);

            List<Tag> initialTagSet = getTaggingResult.getTagSet();
            List<Tag> mergedTagSet = mergeS3TagSet(initialTagSet, new Tag(tagKey, tagValue));

            if (initialTagSet.size() == mergedTagSet.size() && initialTagSet.containsAll(mergedTagSet)) {
                return;
            }

            SetObjectTaggingRequest setObjectTaggingRequest = new SetObjectTaggingRequest(bucketName, objectKey,
                    new ObjectTagging(mergedTagSet));

            s3Client.setObjectTagging(setObjectTaggingRequest);
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Merges a new tag into an existing list of tags.
     * It will be either appended to the list or overwrite the value of an existing tag with the same key.
     * @param initialTags 
     * @param changeTag 
     * @return {@link List} of merged {@link Tag}s
     */
    private List<Tag> mergeS3TagSet(List<Tag> initialTags, Tag changeTag) {
        Map<String, String> mergedTags = initialTags.stream().collect(Collectors.toMap(Tag::getKey, Tag::getValue));
        mergedTags.put(changeTag.getKey(), changeTag.getValue());
        return mergedTags.entrySet().stream().map(
                entry -> new Tag(entry.getKey(), entry.getValue())).collect(Collectors.toList());
    }

    @Override
    public void removeValuesUnusedLongerThan(long minimumAge, TimeUnit timeUnit) throws BinaryStoreException {
        try {
            updateBucketLifecycle();
            if (deleteUnusedNatively) {
                return;
            }
            Date deadline = new Date(System.currentTimeMillis() - timeUnit.toMillis(minimumAge));
    
            // There is no capacity in S3 to query on object properties. This must be done
            // by straight iteration, so may take a very long time for large data sets.
            // Here we attempt parallel iteration; see #supplyObjects()
    
            supplyObjects().get().map(S3ObjectSummary::getKey).filter(k ->
                    s3Client.getObjectTagging(new GetObjectTaggingRequest(bucketName, k)).getTagSet().stream().anyMatch(
                            t -> UNUSED_TAG_KEY.equals(t.getKey()) && Boolean.parseBoolean(t.getValue()))
                                    && s3Client.getObjectMetadata(bucketName, k).getLastModified().before(deadline)).forEach(k -> {
                try {
                    s3Client.deleteObject(bucketName, k);
                } catch (AmazonClientException e) {
                    Logger.getLogger(getClass()).warn(e, JcrI18n.unableToDeleteTemporaryFile, e.getMessage());
                }
            });
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    @Override
    public Iterable<BinaryKey> getAllBinaryKeys() throws BinaryStoreException {
        try {
            // get a single (non-inlined) Supplier, but we only call for it, and thence its iterator, when #iterator() is called
            final Supplier<Stream<S3ObjectSummary>> streamSupplier = supplyObjects();
            return () -> streamSupplier.get().map(S3ObjectSummary::getKey).map(BinaryKey::new).iterator();
        } catch (AmazonClientException e) {
            throw new BinaryStoreException(e);
        }
    }

    /**
     * Upgrade function to migrate {@link #UNUSED_KEY} to {@link #UNUSED_TAG_KEY}.
     * @throws BinaryStoreException 
     * @since Modeshape 5.5
     */
    public void migrateUnusedMetadataToTags() throws BinaryStoreException {
        for (S3ObjectSummary s : S3Objects.inBucket(s3Client, bucketName)) {
            String k = s.getKey();
            ObjectMetadata metadata = s3Client.getObjectMetadata(bucketName, k);
            String unused = metadata.getUserMetaDataOf(UNUSED_KEY);
            if (unused != null) {
                setS3ObjectTag(k, UNUSED_TAG_KEY, unused);
                setS3ObjectUserProperty(k, UNUSED_KEY, null);
            }
        }
    }

    private Supplier<Stream<S3ObjectSummary>> supplyObjects() {
        final S3Objects s3Objects = S3Objects.inBucket(s3Client, bucketName);
        return () -> StreamSupport.stream(s3Objects.spliterator(), true);
    }

    private void updateBucketLifecycle() {
        synchronized (this) {
            if (lifecycleUpdated) {
                return;
            }
            lifecycleUpdated = true;
        }
        final BucketLifecycleConfiguration bucketLifecycleConfiguration = 
            Optional.of(bucketName).map(s3Client::getBucketLifecycleConfiguration).orElseGet(BucketLifecycleConfiguration::new);

        final List<Rule> rules = Optional.of(bucketLifecycleConfiguration).map(BucketLifecycleConfiguration::getRules)
            .map(ArrayList::new).orElseGet(ArrayList::new);

        final Optional<Rule> unusedRule = rules.stream().filter(r -> UNUSED_RULE.equals(r.getId())).findFirst();

        if (unusedRule.isPresent()) {
            final Rule r = unusedRule.get();
            if (BucketLifecycleConfiguration.DISABLED.equals(r.getStatus()) ^ deleteUnusedNatively) {
                // nothing to do
                return;
            }
            r.setStatus(deleteUnusedNatively ? BucketLifecycleConfiguration.ENABLED : BucketLifecycleConfiguration.DISABLED);
        } else if (deleteUnusedNatively) {
            final Rule r = new Rule();
            r.setId(UNUSED_RULE);
            r.setFilter(new LifecycleFilter().withPredicate(
                    new LifecycleTagPredicate(new Tag(UNUSED_TAG_KEY, Boolean.toString(true)))));
            r.setExpirationInDays(0);
            r.setStatus(BucketLifecycleConfiguration.ENABLED);
            rules.add(r);
        } else {
            // nothing to do
            return;
        }
        bucketLifecycleConfiguration.setRules(rules);
        s3Client.setBucketLifecycleConfiguration(bucketName, bucketLifecycleConfiguration);
    }
}
