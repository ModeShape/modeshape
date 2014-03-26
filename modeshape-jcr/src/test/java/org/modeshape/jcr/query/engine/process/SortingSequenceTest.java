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
package org.modeshape.jcr.query.engine.process;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.AbstractNodeSequenceTest;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.RowExtractors;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.model.NullOrder;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.ValueTypeSystem;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class SortingSequenceTest extends AbstractNodeSequenceTest {

    private ExecutionContext context;
    private BufferManager bufferMgr;
    private TypeSystem types;

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
        this.context = new ExecutionContext();
        this.bufferMgr = new BufferManager(context);
        this.types = new ValueTypeSystem(context.getValueFactories());
    }

    @After
    @Override
    public void afterEach() {
        this.bufferMgr.close();
    }

    @Test
    public void shouldSortSequenceWithDuplicatesOnHeapBuffer() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        boolean allowDups = true;
        ExtractFromRow extractor = RowExtractors.extractPath(0, cache, types);
        SortingSequence sorted = new SortingSequence(workspaceName(), allNodes(), extractor, bufferMgr, cache, pack, useHeap,
                                                     allowDups, NullOrder.NULLS_LAST);
        assertThat(sorted.getRowCount(), is(countRows(allNodes())));
        assertSorted(sorted, extractor);
    }

    @Test
    public void shouldSortSequenceWithoutDuplicatesOnHeapBuffer() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        boolean allowDups = false;
        ExtractFromRow extractor = RowExtractors.extractPath(0, cache, types);
        SortingSequence sorted = new SortingSequence(workspaceName(), allNodes(), extractor, bufferMgr, cache, pack, useHeap,
                                                     allowDups, NullOrder.NULLS_LAST);
        assertThat(sorted.getRowCount(), is(countRows(allNodes())));
        assertSorted(sorted, extractor);
    }

    @Test
    public void shouldSortSequenceWithDuplicatesOffHeapBuffer() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        boolean allowDups = true;
        ExtractFromRow extractor = RowExtractors.extractPath(0, cache, types);
        SortingSequence sorted = new SortingSequence(workspaceName(), allNodes(), extractor, bufferMgr, cache, pack, useHeap,
                                                     allowDups, NullOrder.NULLS_LAST);
        assertThat(sorted.getRowCount(), is(countRows(allNodes())));
        assertSorted(sorted, extractor);
    }

    @Test
    public void shouldSortSequenceWithoutDuplicatesOffHeapBuffer() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        boolean allowDups = false;
        ExtractFromRow extractor = RowExtractors.extractPath(0, cache, types);
        SortingSequence sorted = new SortingSequence(workspaceName(), allNodes(), extractor, bufferMgr, cache, pack, useHeap,
                                                     allowDups, NullOrder.NULLS_LAST);
        assertThat(sorted.getRowCount(), is(countRows(allNodes())));
        assertSorted(sorted, extractor);
    }

    @Test
    public void shouldSortSequenceWithDuplicatesWithNullSortValues() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        boolean allowDups = true;
        ExtractFromRow extractor = RowExtractors.extractPropertyValue(name("propC"), 0, cache, types.getStringFactory());
        SortingSequence sorted = new SortingSequence(workspaceName(), allNodes(), extractor, bufferMgr, cache, pack, useHeap,
                                                     allowDups, NullOrder.NULLS_LAST);
        assertSorted(sorted, extractor);
    }

    protected void assertSorted( NodeSequence sequence,
                                 ExtractFromRow extractor ) {
        List<Object> values = new ArrayList<Object>();
        // Iterate over the batches ...
        try {
            Batch batch = null;
            while ((batch = sequence.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    Object value = extractor.getValueInRow(batch);
                    values.add(value);
                    print("Found " + value);
                }
            }
        } finally {
            sequence.close();
        }
        @SuppressWarnings( "unchecked" )
        Comparator<Object> comparator = (Comparator<Object>)extractor.getType().getComparator();
        List<Object> naturallySorted = new ArrayList<Object>(values);
        Collections.sort(naturallySorted, comparator);
        assertThat(values, is(naturallySorted));
    }
}
