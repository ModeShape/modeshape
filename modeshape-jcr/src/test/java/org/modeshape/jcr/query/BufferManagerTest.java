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

package org.modeshape.jcr.query;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.util.Comparator;
import java.util.Iterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.Serializer;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.BufferManager.DistinctBuffer;
import org.modeshape.jcr.query.BufferManager.SortingBuffer;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class BufferManagerTest {

    private ExecutionContext context;
    private BufferManager mgr;
    private TypeSystem types;

    @Before
    public void beforeEach() {
        context = new ExecutionContext();
        mgr = new BufferManager(context);
        types = context.getValueFactories().getTypeSystem();
    }

    @After
    public void afterEach() {
        mgr.close();
    }

    @Test
    public void shouldCreateDistinctBufferOnHeap() {
        try (DistinctBuffer<String> buffer = mgr.createDistinctBuffer(Serializer.STRING).useHeap(true).keepSize(true).make()) {
            assertTrue(buffer.addIfAbsent("first"));
            assertTrue(buffer.addIfAbsent("second"));
            assertTrue(buffer.addIfAbsent("third"));
            assertTrue(buffer.addIfAbsent("fourth"));
            assertFalse(buffer.addIfAbsent("first"));
            assertFalse(buffer.addIfAbsent("second"));
            assertFalse(buffer.addIfAbsent("fourth"));
            assertFalse(buffer.addIfAbsent("third"));
            assertThat(buffer.size(), is(4L));
        }
    }

    @Test
    public void shouldCreateDistinctBufferOffHeap() {
        try (DistinctBuffer<String> buffer = mgr.createDistinctBuffer(Serializer.STRING).useHeap(false).keepSize(true).make()) {
            assertTrue(buffer.addIfAbsent("first"));
            assertTrue(buffer.addIfAbsent("second"));
            assertTrue(buffer.addIfAbsent("third"));
            assertTrue(buffer.addIfAbsent("fourth"));
            assertFalse(buffer.addIfAbsent("first"));
            assertFalse(buffer.addIfAbsent("second"));
            assertFalse(buffer.addIfAbsent("fourth"));
            assertFalse(buffer.addIfAbsent("third"));
            assertThat(buffer.size(), is(4L));
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldCreateSortBufferOnHeap() {
        TypeFactory<String> stringType = types.getStringFactory();
        BTreeKeySerializer<String> strKeySerializer = (BTreeKeySerializer<String>)mgr.bTreeKeySerializerFor(stringType, false);
        Serializer<String> strSerializer = (Serializer<String>)mgr.serializerFor(stringType);
        try (SortingBuffer<String, String> buffer = mgr.createSortingBuffer(strKeySerializer, strSerializer).useHeap(true)
                                                       .keepSize(true).make()) {
            buffer.put("value1", "first");
            buffer.put("value2", "first");
            buffer.put("value3", "first");
            buffer.put("value1", "second");
            buffer.put("value3", "second");
            buffer.put("value1", "third");
            assertThat(buffer.size(), is(3L));

            Iterator<String> iter = buffer.ascending();
            assertThat(iter.next(), is("third"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("second"));
            assertThat(iter.hasNext(), is(false));

            iter = buffer.descending();
            assertThat(iter.next(), is("second"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("third"));
            assertThat(iter.hasNext(), is(false));

            iter = buffer.getAll("value1");
            assertThat(iter.next(), is("third"));
            assertThat(iter.hasNext(), is(false));
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldCreateSortBufferOffHeap() {
        TypeFactory<String> stringType = types.getStringFactory();
        BTreeKeySerializer<String> strKeySerializer = (BTreeKeySerializer<String>)mgr.bTreeKeySerializerFor(stringType, false);
        Serializer<String> strSerializer = (Serializer<String>)mgr.serializerFor(stringType);
        try (SortingBuffer<String, String> buffer = mgr.createSortingBuffer(strKeySerializer, strSerializer).useHeap(false)
                                                       .keepSize(true).make()) {
            buffer.put("value1", "first");
            buffer.put("value2", "first");
            buffer.put("value3", "first");
            buffer.put("value1", "second");
            buffer.put("value3", "second");
            buffer.put("value1", "third");
            assertThat(buffer.size(), is(3L));

            Iterator<String> iter = buffer.ascending();
            assertThat(iter.next(), is("third"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("second"));
            assertThat(iter.hasNext(), is(false));

            iter = buffer.descending();
            assertThat(iter.next(), is("second"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("third"));
            assertThat(iter.hasNext(), is(false));

            iter = buffer.getAll("value1");
            assertThat(iter.next(), is("third"));
            assertThat(iter.hasNext(), is(false));
        }
    }

    @SuppressWarnings( "unchecked" )
    @Test
    public void shouldCreateSortWithDuplicateKeysBufferOnHeap() {
        TypeFactory<String> stringType = types.getStringFactory();
        Serializer<String> strSerializer = (Serializer<String>)mgr.serializerFor(stringType);
        Comparator<String> keyComparator = stringType.getComparator();
        try (SortingBuffer<String, String> buffer = mgr.createSortingWithDuplicatesBuffer(strSerializer, keyComparator,
                                                                                          strSerializer).useHeap(true)
                                                       .keepSize(true).make()) {
            buffer.put("value1", "first");
            buffer.put("value2", "first");
            buffer.put("value3", "first");
            buffer.put("value1", "second");
            buffer.put("value3", "second");
            buffer.put("value1", "third");
            assertThat(buffer.size(), is(6L));

            Iterator<String> iter = buffer.ascending();
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("second"));
            assertThat(iter.next(), is("third"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("second"));
            assertThat(iter.hasNext(), is(false));

            iter = buffer.descending();
            assertThat(iter.next(), is("second"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("third"));
            assertThat(iter.next(), is("second"));
            assertThat(iter.next(), is("first"));
            assertThat(iter.hasNext(), is(false));

            iter = buffer.getAll("value1");
            assertThat(iter.next(), is("first"));
            assertThat(iter.next(), is("second"));
            assertThat(iter.next(), is("third"));
            assertThat(iter.hasNext(), is(false));
        }
    }
}
