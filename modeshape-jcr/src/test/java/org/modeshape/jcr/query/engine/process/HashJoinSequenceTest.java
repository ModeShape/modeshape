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
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.query.AbstractNodeSequenceTest;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence.RowAccessor;
import org.modeshape.jcr.query.RowExtractors;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.engine.process.JoinSequence.RangeProducer;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.value.ValueTypeSystem;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class HashJoinSequenceTest extends AbstractNodeSequenceTest {

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

    @Test
    public void shouldInnerJoinParentToChildOnHeap() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.INNER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          leftInnerJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldInnerJoinParentToChildOffHeap() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.INNER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          leftInnerJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldLeftOuterJoinParentToChildOnHeap() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.LEFT_OUTER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          leftOuterJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldLeftOuterJoinParentToChildOffHeap() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.LEFT_OUTER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          leftOuterJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldRightOuterJoinParentToChildOnHeap() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.RIGHT_OUTER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          rightOuterJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                 RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldRightOuterJoinParentToChildOffHeap() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.RIGHT_OUTER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          rightOuterJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                 RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldFullOuterJoinParentToChildOnHeap() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.FULL_OUTER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          fullOuterJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldFullOuterJoinParentToChildOffHeap() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
            print("Parent of nodes:", allNodes(), RowExtractors.extractParentNodeKey(0, cache, types));
        }
        JoinType joinType = JoinType.FULL_OUTER;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractParentNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          fullOuterJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                                RowExtractors.extractParentPath(1, cache, types)));
    }

    @Test
    public void shouldCrossJoinParentToChildOnHeap() {
        // print(true);
        boolean useHeap = true;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
        }
        long nodeCount = countRows(allNodes());
        JoinType joinType = JoinType.CROSS;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          crossJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                            RowExtractors.extractPath(1, cache, types), nodeCount * nodeCount));
    }

    @Test
    public void shouldCrossJoinParentToChildOffHeap() {
        // print(true);
        boolean useHeap = false;
        boolean pack = false;
        if (print()) {
            print("All nodes:", allNodes(), RowExtractors.extractPath(0, cache, types));
            print("NodeKeys of nodes:", allNodes(), RowExtractors.extractNodeKey(0, cache, types));
        }
        long nodeCount = countRows(allNodes());
        JoinType joinType = JoinType.CROSS;
        ExtractFromRow leftExtractor = RowExtractors.extractNodeKey(0, cache, types);
        ExtractFromRow rightExtractor = RowExtractors.extractNodeKey(0, cache, types);
        RangeProducer<?> rangeProducer = null;
        HashJoinSequence join = new HashJoinSequence(workspaceName(), allNodes(), allNodes(), leftExtractor, rightExtractor,
                                                     joinType, bufferMgr, cache, rangeProducer, pack, useHeap);
        // Verify the join ...
        assertRowsSatisfy(join,
                          crossJoinVerifier(RowExtractors.extractPath(0, cache, types),
                                            RowExtractors.extractPath(1, cache, types), nodeCount * nodeCount));
    }

    protected Verifier leftInnerJoinVerifier( final ExtractFromRow leftExtractor,
                                              final ExtractFromRow rightExtractor ) {
        return new Verifier() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void verify( RowAccessor currentRow ) {
                Object leftValue = leftExtractor.getValueInRow(currentRow);
                Object rightValue = rightExtractor.getValueInRow(currentRow);
                print("Found [" + leftValue + ", " + rightValue + "] " + rowAsString(currentRow));
                assertThat(leftValue, is(rightValue));
            }

            @Override
            public void complete() {
            }
        };
    }

    protected Verifier leftOuterJoinVerifier( final ExtractFromRow leftExtractor,
                                              final ExtractFromRow rightExtractor ) {
        return new Verifier() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void verify( RowAccessor currentRow ) {
                Object leftValue = leftExtractor.getValueInRow(currentRow);
                Object rightValue = rightExtractor.getValueInRow(currentRow);
                print("Found [" + leftValue + ", " + rightValue + "] " + rowAsString(currentRow));
                if (rightValue == null) return; // LEFT OUTER JOIN
                assertThat(leftValue, is(rightValue));
            }

            @Override
            public void complete() {
            }
        };
    }

    protected Verifier rightOuterJoinVerifier( final ExtractFromRow leftExtractor,
                                               final ExtractFromRow rightExtractor ) {
        return new Verifier() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void verify( RowAccessor currentRow ) {
                Object leftValue = leftExtractor.getValueInRow(currentRow);
                Object rightValue = rightExtractor.getValueInRow(currentRow);
                print("Found [" + leftValue + ", " + rightValue + "] " + rowAsString(currentRow));
                if (leftValue == null) return; // RIGHT OUTER JOIN
                assertThat(leftValue, is(rightValue));
            }

            @Override
            public void complete() {
            }
        };
    }

    protected Verifier fullOuterJoinVerifier( final ExtractFromRow leftExtractor,
                                              final ExtractFromRow rightExtractor ) {
        return new Verifier() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void verify( RowAccessor currentRow ) {
                Object leftValue = leftExtractor.getValueInRow(currentRow);
                Object rightValue = rightExtractor.getValueInRow(currentRow);
                print("Found [" + leftValue + ", " + rightValue + "] " + rowAsString(currentRow));
                if (leftValue == null || rightValue == null) return; // FULL OUTER JOIN
                assertThat(leftValue, is(rightValue));
            }

            @Override
            public void complete() {
            }
        };
    }

    protected Verifier crossJoinVerifier( final ExtractFromRow leftExtractor,
                                          final ExtractFromRow rightExtractor,
                                          final long rowCount ) {
        return new Verifier() {
            private long found = 0;

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void verify( RowAccessor currentRow ) {
                Object leftValue = leftExtractor.getValueInRow(currentRow);
                Object rightValue = rightExtractor.getValueInRow(currentRow);
                print("Found [" + leftValue + ", " + rightValue + "] " + rowAsString(currentRow));
                ++found;
            }

            @Override
            public void complete() {
                assertThat(found, is(rowCount));
            }
        };
    }
}
