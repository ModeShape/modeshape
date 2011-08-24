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

import org.infinispan.Cache;
import org.infinispan.batch.AutoBatchSupport;
import org.infinispan.batch.BatchContainer;
import org.infinispan.container.entries.CacheEntry;
import org.infinispan.context.Flag;
import org.infinispan.context.InvocationContext;
import org.infinispan.context.InvocationContextContainer;
import org.infinispan.marshall.MarshalledValue;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Binary;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

/**
 * A layer of indirection around an {@link SchematicEntryLiteral} to provide consistency and isolation for concurrent readers
 * while writes may also be going on. The techniques used in this implementation are very similar to the lock-free reader MVCC
 * model used in the {@link org.infinispan.container.entries.MVCCEntry} implementations for the core data container, which closely
 * follow software transactional memory approaches to dealing with concurrency. <br />
 * <br />
 * Implementations of this class are rarely created on their own;
 * {@link SchematicEntryLiteral#getProxy(Cache, String, BatchContainer, InvocationContextContainer)} should be used to retrieve an
 * instance of this proxy. <br />
 * <br />
 * Typically proxies are only created by the {@link SchematicEntryLookup} helper, and would not be created by end-user code
 * directly.
 * 
 * @author Randall Hauch <rhauch@redhat.com> (C) 2011 Red Hat Inc.
 * @since 5.1
 * @see org.infinispan.atomic.AtomicHashMapProxy
 */
public class SchematicEntryProxy extends AutoBatchSupport implements SchematicEntry {
    private static final Log log = LogFactory.getLog(SchematicEntryProxy.class);
    private static final boolean trace = log.isTraceEnabled();

    final Cache<String, SchematicEntry> cache;
    final String key;
    final InvocationContextContainer icc;
    volatile boolean startedReadingValue = false;

    SchematicEntryProxy( Cache<String, SchematicEntry> cache,
                         String key,
                         BatchContainer batchContainer,
                         InvocationContextContainer icc ) {
        this.key = key;
        this.cache = cache;
        this.batchContainer = batchContainer;
        this.icc = icc;
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
     * Utility method that gets the {@link SchematicEntryLiteral literal value} for this proxy, and which marks the value for
     * reading and performs various assertion checks. Comment this
     * 
     * @return the literal entry
     */
    // internal helper, reduces lots of casts.
    private SchematicEntryLiteral getDeltaValueForRead() {
        SchematicEntryLiteral value = toValue(cache.get(key));
        if (value != null && !startedReadingValue) {
            startedReadingValue = true;
        }
        assertValid(value);
        return value;
    }

    private SchematicEntryLiteral getDeltaValueForWrite( InvocationContext ctx ) {
        CacheEntry lookedUpEntry = ctx.lookupEntry(key);
        boolean lockedAndCopied = lookedUpEntry != null && lookedUpEntry.isChanged() && toValue(lookedUpEntry.getValue()).copied;

        if (lockedAndCopied) {
            return getDeltaValueForRead();
        }
        // Otherwise, acquire the write lock ...
        boolean suppressLocks = ctx.hasFlag(Flag.SKIP_LOCKING);
        if (!suppressLocks) ctx.setFlags(Flag.FORCE_WRITE_LOCK);

        if (trace) {
            if (suppressLocks) {
                log.trace("Skip locking flag used.  Skipping locking.");
            } else {
                log.trace("Forcing write lock even for reads");
            }
        }

        SchematicEntryLiteral value = getDeltaValueForRead();
        // copy for write
        SchematicEntryLiteral copy = value == null ? new SchematicEntryLiteral(key, true) : value.copyForWrite();
        copy.initForWriting();
        // reinstate the flag
        if (suppressLocks) {
            ctx.setFlags(Flag.SKIP_LOCKING);
        }
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
            InvocationContext ic = icc.createInvocationContext(true);
            getDeltaValueForWrite(ic).setContent(content, metadata, defaultContentType);
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
            InvocationContext ic = icc.createInvocationContext(true);
            getDeltaValueForWrite(ic).setContent(content, metadata, defaultContentType);
        } finally {
            endAtomic();
        }
    }

    @Override
    public EditableDocument editDocumentContent() {
        try {
            startAtomic();
            InvocationContext ic = icc.createInvocationContext(true);
            return getDeltaValueForWrite(ic).editDocumentContent();
        } finally {
            endAtomic();
        }
    }

    @Override
    public EditableDocument editMetadata() {
        try {
            startAtomic();
            InvocationContext ic = icc.createInvocationContext(true);
            return getDeltaValueForWrite(ic).editMetadata();
        } finally {
            endAtomic();
        }
    }
}
