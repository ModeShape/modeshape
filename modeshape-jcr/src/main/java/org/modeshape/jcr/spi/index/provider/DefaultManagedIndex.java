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
package org.modeshape.jcr.spi.index.provider;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.spi.index.IndexConstraints;

/**
 * A {@link ManagedIndex} implementation which wraps an index instance created by a particular provider, offering
 * a default implementation for some index-related operations.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 * @since 4.5
 */
@Immutable
public final class DefaultManagedIndex implements ManagedIndex {
    
    private final ProvidedIndex<?> index;
    private final IndexChangeAdapter adapter;
    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicReference<IndexManager.IndexStatus> status = new AtomicReference<>(IndexManager.IndexStatus.ENABLED);

    /**
     * Creates a new managed index instance which wraps an provider-specific instance and a change adapter. 
     * 
     * @param index a {@link ProvidedIndex} instance, may not be null.
     * @param adapter a {@link IndexChangeAdapter} instance, may not be null.
     */
    public DefaultManagedIndex( ProvidedIndex<?> index, IndexChangeAdapter adapter ) {
        assert index != null;
        assert adapter != null;
        
        this.index = index;
        this.adapter = adapter;
    }

    @Override
    public long estimateTotalCount() {
        return index.estimateTotalCount();
    }

    @Override
    public long estimateCardinality( List<Constraint> andedConstraints,
                                     Map<String, Object> variables ) {
        return index.estimateCardinality(andedConstraints, variables);
    }

    @Override
    public Results filter( IndexConstraints constraints ) {
        return index.filter(constraints);
    }

    @Override
    public IndexChangeAdapter getIndexChangeAdapter() {
        return adapter;
    }

    @Override
    public void enable( boolean enable ) {
        this.enabled.set(enable);
        if (enable) {
            updateStatus(IndexManager.IndexStatus.DISABLED, IndexManager.IndexStatus.ENABLED);
        } else {
            updateStatus(IndexManager.IndexStatus.ENABLED, IndexManager.IndexStatus.DISABLED);
        }
    }

    @Override
    public boolean isEnabled() {
        return this.enabled.get();
    }

    @Override
    public void shutdown( boolean destroyed ) {
        try {
           index.shutdown(destroyed);
        } finally {
            enable(false);
        }
    }

    @Override
    public void clearAllData() {
        index.clearAllData();
    }

    @Override
    public IndexManager.IndexStatus getStatus() {
        return status.get();
    }

    @Override
    public void updateStatus(IndexManager.IndexStatus currentStatus, IndexManager.IndexStatus newStatus) {
        this.status.compareAndSet(currentStatus, newStatus);
    }

    @Override
    public boolean requiresReindexing() {
        return index.requiresReindexing();
    }
}
