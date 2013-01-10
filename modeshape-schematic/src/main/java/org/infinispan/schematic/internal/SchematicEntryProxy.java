/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.schematic.internal;

import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import org.infinispan.AdvancedCache;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.FlagContainer;
import org.infinispan.marshall.MarshalledValue;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Path;
import org.infinispan.schematic.internal.SchematicEntryLiteral.FieldPath;
import org.infinispan.schematic.internal.delta.DocumentObserver;
import org.infinispan.schematic.internal.document.DocumentEditor;
import org.infinispan.schematic.internal.document.MutableDocument;
import org.infinispan.schematic.internal.document.ObservableDocumentEditor;
import org.infinispan.transaction.LocalTransaction;
import org.infinispan.transaction.TransactionTable;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A layer of indirection around an {@link SchematicEntryLiteral} to provide consistency and isolation for concurrent readers
 * while writes may also be going on. The techniques used in this implementation are very similar to the lock-free reader MVCC
 * model used in the {@link org.infinispan.container.entries.MVCCEntry} implementations for the core data container, which closely
 * follow software transactional memory approaches to dealing with concurrency. <br />
 * <br />
 * Implementations of this class are rarely created on their own;
 * {@link SchematicEntryLiteral#getProxy(CacheContext, String, FlagContainer)} should be used to retrieve an instance of this
 * proxy. <br />
 * <br />
 * Typically proxies are only created by the {@link SchematicEntryLookup} helper, and would not be created by end-user code
 * directly.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 * @see org.infinispan.atomic.AtomicHashMapProxy
 */
public class SchematicEntryProxy extends AutoBatchSupport implements SchematicEntry {
    private static final Log LOGGER = LogFactory.getLog(SchematicEntryProxy.class);
    private static final boolean TRACE = LOGGER.isTraceEnabled();

    private final CacheContext context;
    private final FlagContainer flagContainer;
    private final String key;
    private volatile boolean startedReadingValue = false;

    SchematicEntryProxy( CacheContext context,
                         String key,
                         FlagContainer flagContainer ) {
        this.key = key;
        this.context = context;
        this.batchContainer = context.getCacheForWriting().getBatchContainer();
        this.flagContainer = flagContainer;
    }

    /**
     * Utility method that casts the supplied cache entry value to a {@link SchematicEntryLiteral literal value} for reading
     * purposes.
     * 
     * @param object the cache entry value, which may be a {@link MarshalledValue}
     * @return the literal value; null only when <code>object</code> is null
     */
    protected SchematicEntryLiteral toValue( Object object ) {
        Object value = (object instanceof MarshalledValue) ? ((MarshalledValue)object).get() : object;
        return (SchematicEntryLiteral)value;
    }

    // readers

    /**
     * Utility method that checks that the literal value has not been removed if we've already started reading.
     * 
     * @param value the vlaue
     */
    private void assertValid( SchematicEntryLiteral value ) {
        if (startedReadingValue && (value == null || value.removed)) throw new IllegalStateException(
                                                                                                     "SchematicValue stored under key "
                                                                                                     + key
                                                                                                     + " has been concurrently removed!");
    }

    /**
     * Looks up the CacheEntry stored in transactional context corresponding to this SchematicEntryLiteral.
     * 
     * @return the cache entry; or null if the SchematicEntryLiteral has yet to be touched by the current transaction
     */
    protected CacheEntry lookupEntryFromCurrentTransaction() {
        // Grab the TransactionTable and check if an ongoing transaction exists and peek into transactional state...
        try {
            TransactionManager txnMgr = context.getTransactionManager();
            TransactionTable transactionTable = context.getTransactionTable();
            Transaction tx = txnMgr.getTransaction();
            LocalTransaction localTransaction = tx == null ? null : transactionTable.getLocalTransaction(tx);

            // The stored localTransaction could be null, if this is the first call in a transaction. In which case
            // we know that there is no transactional state to refer to - i.e., no entries have been looked up as yet.
            return localTransaction == null ? null : localTransaction.lookupEntry(key);
        } catch (SystemException e) {
            return null;
        }
    }

    /**
     * Look up the SchematicEntryLiteral in the cache. Note that if this thread is participating in a transaction and the entry
     * has been modified in that transaction, this method will return the (updated) stored entry. Otherwise, this method always
     * returns the persisted and available entry.
     * 
     * @return the literal entry
     */
    private SchematicEntryLiteral getDeltaValueForRead() {
        SchematicEntryLiteral value = toValue(context.getCache().get(key));
        if (value != null && !startedReadingValue) {
            startedReadingValue = true;
        }
        assertValid(value);
        return value;
    }

    private SchematicEntryLiteral getDeltaValueForWrite() {
        CacheEntry lookedUpEntry = lookupEntryFromCurrentTransaction();
        boolean lockedAndCopied = lookedUpEntry != null && lookedUpEntry.isChanged() && toValue(lookedUpEntry.getValue()).copied;

        if (lockedAndCopied) {
            return getDeltaValueForRead();
        }
        // Otherwise, acquire the write lock ...
        boolean suppressLocks = flagContainer != null && flagContainer.hasFlag(Flag.SKIP_LOCKING);
        if (!suppressLocks && flagContainer != null) flagContainer.setFlags(Flag.FORCE_WRITE_LOCK);

        if (TRACE) {
            if (suppressLocks) {
                LOGGER.trace("Skip locking flag used.  Skipping locking.");
            } else {
                LOGGER.trace("Forcing write lock even for reads");
            }
        }

        // We need to lock the entry **BEFORE** we copy the value (and clone its document). Without this explicit lock,
        // it's possible for multiple concurrent threads to both make copies of the same value, each change them to something
        // else, and then update the cache; the last update will overwrite the earlier updates.
        // This is known as "write-skew", and see MODE-1734 for background.

        // Now we've locked this value, can make the copy, and update the cache
        // (where it will be stored in the transaction table) ...
        if (context.isExplicitLockingEnabled()) {
            context.getCacheForLocking().lock(key); // released at TXN commit/rollback
        }

        // Make a copy that we'll use for the updates ...
        SchematicEntryLiteral value = getDeltaValueForRead();
        SchematicEntryLiteral copy = null;
        if (value != null) {
            copy = value.copyForWrite();
        } else {
            copy = new SchematicEntryLiteral(key);
            copy.copied = true;
        }
        // Create a delta implementation based on the configuration ...
        copy.createDelta(context);
        if (TRACE) {
            LOGGER.trace("Created copy of " + key + ": " + copy.data());
        }

        // reinstate the flag
        if (suppressLocks) {
            flagContainer.setFlags(Flag.SKIP_LOCKING);
        }
        AdvancedCache<String, SchematicEntry> cache = context.getCacheForWriting();
        cache.put(key, copy);
        return copy;
    }

    // ----------------------------------------------------------------------------------------
    // Read methods
    // ----------------------------------------------------------------------------------------

    @Override
    public Document getMetadata() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.getMetadata();
    }

    @Override
    public String getContentType() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.getContentType();
    }

    @Override
    public Object getContent() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.getContent();
    }

    @Override
    public Document getContentAsDocument() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.getContentAsDocument();
    }

    @Override
    public Binary getContentAsBinary() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.getContentAsBinary();
    }

    @Override
    public boolean hasDocumentContent() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.hasDocumentContent();
    }

    @Override
    public boolean hasBinaryContent() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.hasBinaryContent();
    }

    @Override
    public Document asDocument() {
        SchematicEntryLiteral value = getDeltaValueForRead();
        return value.asDocument();
    }

    @Override
    public String toString() {
        return "SchematicValueProxy{" + "key=" + key + '}';
    }

    // ----------------------------------------------------------------------------------------
    // Write methods
    // ----------------------------------------------------------------------------------------

    @Override
    public void setContent( Binary content,
                            Document metadata,
                            String defaultContentType ) {
        try {
            startAtomic();
            getDeltaValueForWrite().internalSetContent(content, metadata, defaultContentType);
        } finally {
            endAtomic();
        }
    }

    @Override
    public void setContent( Document content,
                            Document metadata,
                            String defaultContentType ) {
        try {
            startAtomic();
            getDeltaValueForWrite().internalSetContent(content, metadata, defaultContentType);
        } finally {
            endAtomic();
        }
    }

    @Override
    public EditableDocument editDocumentContent() {
        SchematicEntryLiteral writable = getDeltaValueForWrite();
        Document content = writable.data().getDocument(FieldName.CONTENT);
        return editorFor(content, FieldPath.CONTENT, writable.getDelta());
    }

    @Override
    public EditableDocument editMetadata() {
        SchematicEntryLiteral writable = getDeltaValueForWrite();
        Document metadata = writable.data().getDocument(FieldName.METADATA);
        return editorFor(metadata, FieldPath.METADATA, writable.getDelta());
    }

    protected EditableDocument editorFor( Document doc,
                                          Path pathToDocument,
                                          DocumentObserver observer ) {
        if (doc == null) return null;
        if (doc instanceof DocumentEditor) {
            return (DocumentEditor)doc;
        }
        // Otherwise, create the editor ...
        if (context.isDeltaContainingChangesEnabled() && context.isClustered()) {
            // It's clustered and we're supposed to record changes for the deltas ...
            return new ObservableDocumentEditor((MutableDocument)doc, pathToDocument, observer, null);
        }
        // No need to record change operations ...
        return new DocumentEditor((MutableDocument)doc);
    }
}
