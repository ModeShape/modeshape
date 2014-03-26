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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.Serializable;
import org.mapdb.Serializer;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.RowAccessor;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class BufferedRows {

    private BufferedRows() {
    }

    protected static interface BufferedRow extends RowAccessor {
    }

    public static final class SingleNodeRow implements BufferedRow {
        private final CachedNode node;
        private final float score;

        protected SingleNodeRow( CachedNode node,
                                 float score ) {
            this.node = node;
            this.score = score;
        }

        @Override
        public int width() {
            return 1;
        }

        @Override
        public CachedNode getNode() {
            return node;
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index > 0) throw new IndexOutOfBoundsException();
            return node;
        }

        @Override
        public float getScore() {
            return score;
        }

        @Override
        public float getScore( int index ) {
            if (index > 0) throw new IndexOutOfBoundsException();
            return score;
        }

        @Override
        public String toString() {
            return "{ " + score + " " + node + " }";
        }
    }

    public static final class DoubleNodeRow implements BufferedRow {
        private final CachedNode node1;
        private final CachedNode node2;
        private final float score1;
        private final float score2;

        protected DoubleNodeRow( CachedNode node1,
                                 CachedNode node2,
                                 float score1,
                                 float score2 ) {
            this.node1 = node1;
            this.node2 = node2;
            this.score1 = score1;
            this.score2 = score2;
        }

        @Override
        public int width() {
            return 2;
        }

        @Override
        public CachedNode getNode() {
            return node1;
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index == 0) return node1;
            if (index == 1) return node2;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public float getScore() {
            return score1;
        }

        @Override
        public float getScore( int index ) {
            if (index == 0) return score1;
            if (index == 1) return score2;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString() {
            return "{ " + score1 + " " + node1 + " }\n {" + score2 + " " + node2 + " }";
        }
    }

    public static final class TripleNodeRow implements BufferedRow {
        private final CachedNode node1;
        private final CachedNode node2;
        private final CachedNode node3;
        private final float score1;
        private final float score2;
        private final float score3;

        protected TripleNodeRow( CachedNode node1,
                                 CachedNode node2,
                                 CachedNode node3,
                                 float score1,
                                 float score2,
                                 float score3 ) {
            this.node1 = node1;
            this.node2 = node2;
            this.node3 = node3;
            this.score1 = score1;
            this.score2 = score2;
            this.score3 = score3;
        }

        @Override
        public int width() {
            return 3;
        }

        @Override
        public CachedNode getNode() {
            return node1;
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index == 0) return node1;
            if (index == 1) return node2;
            if (index == 2) return node3;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public float getScore() {
            return score1;
        }

        @Override
        public float getScore( int index ) {
            if (index == 0) return score1;
            if (index == 1) return score2;
            if (index == 2) return score3;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString() {
            return "{ " + score1 + " " + node1 + " }\n {" + score2 + " " + node2 + " }\n {" + score3 + " " + node3 + " }";
        }
    }

    public static final class QuadNodeRow implements BufferedRow {
        private final CachedNode node1;
        private final CachedNode node2;
        private final CachedNode node3;
        private final CachedNode node4;
        private final float score1;
        private final float score2;
        private final float score3;
        private final float score4;

        protected QuadNodeRow( CachedNode node1,
                               CachedNode node2,
                               CachedNode node3,
                               CachedNode node4,
                               float score1,
                               float score2,
                               float score3,
                               float score4 ) {
            this.node1 = node1;
            this.node2 = node2;
            this.node3 = node3;
            this.node4 = node4;
            this.score1 = score1;
            this.score2 = score2;
            this.score3 = score3;
            this.score4 = score4;
        }

        @Override
        public int width() {
            return 4;
        }

        @Override
        public CachedNode getNode() {
            return node1;
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index == 0) return node1;
            if (index == 1) return node2;
            if (index == 2) return node3;
            if (index == 3) return node4;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public float getScore() {
            return score1;
        }

        @Override
        public float getScore( int index ) {
            if (index == 0) return score1;
            if (index == 1) return score2;
            if (index == 2) return score3;
            if (index == 3) return score4;
            throw new IndexOutOfBoundsException();
        }

        @Override
        public String toString() {
            return "{ " + score1 + " " + node1 + " }\n {" + score2 + " " + node2 + " }\n {" + score3 + " " + node3 + " }\n {"
                   + score4 + " " + node4 + " }";
        }
    }

    public static final class MultiNodeRow implements BufferedRow {
        private final CachedNode[] nodes;
        private final float[] scores;

        protected MultiNodeRow( CachedNode[] nodes,
                                float[] scores ) {
            this.nodes = nodes;
            this.scores = scores;
        }

        @Override
        public int width() {
            return nodes.length;
        }

        @Override
        public CachedNode getNode() {
            return nodes[0];
        }

        @Override
        public CachedNode getNode( int index ) {
            return nodes[index];
        }

        @Override
        public float getScore() {
            return scores[0];
        }

        @Override
        public float getScore( int index ) {
            return scores[index];
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i != nodes.length; ++i) {
                if (i != 0) sb.append("\n ");
                else sb.append(" { ");
                sb.append(scores[i]).append(" ").append(nodes[i]);
                sb.append(" } ");
                if (i != 0) sb.append(" ");
            }
            return sb.toString();
        }
    }

    public static interface BufferedRowFactory<T extends BufferedRow> extends Serializer<T> {
        T createRow( Batch currentRow );
    }

    public static BufferedRowFactory<? extends BufferedRow> serializer( CachedNodeSupplier nodeCache,
                                                                        int width ) {
        if (width == 1) return new SingleNodeRowSerializer(nodeCache);
        if (width == 2) return new DoubleNodeRowSerializer(nodeCache);
        if (width == 3) return new TripleNodeRowSerializer(nodeCache);
        if (width == 4) return new QuadNodeRowSerializer(nodeCache);
        return new MultiNodeRowSerializer(nodeCache, width);
    }

    protected static String serializeNodeKey( CachedNode node ) {
        return node != null ? node.getKey().toString() : "";
    }

    protected static CachedNode deserializeNodeKey( String keyStr,
                                                    CachedNodeSupplier cache ) {
        return keyStr.length() == 0 ? null : cache.getNode(new NodeKey(keyStr));
    }

    protected static final class SingleNodeRowSerializer implements BufferedRowFactory<SingleNodeRow>, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient CachedNodeSupplier cache;

        public SingleNodeRowSerializer( CachedNodeSupplier cache ) {
            this.cache = cache;
        }

        @Override
        public void serialize( DataOutput out,
                               SingleNodeRow value ) throws IOException {
            out.writeUTF(serializeNodeKey(value.getNode()));
            out.writeFloat(value.getScore());
        }

        @Override
        public SingleNodeRow deserialize( DataInput in,
                                          int available ) throws IOException {
            String keyStr = in.readUTF();
            float score = in.readFloat();
            CachedNode node = deserializeNodeKey(keyStr, cache);
            return new SingleNodeRow(node, score);
        }

        @Override
        public SingleNodeRow createRow( Batch currentRow ) {
            return new SingleNodeRow(currentRow.getNode(), currentRow.getScore());
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }

        @Override
        public String toString() {
            return "SingleNodeRowSerializer";
        }
    }

    protected static final class DoubleNodeRowSerializer implements BufferedRowFactory<DoubleNodeRow>, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient CachedNodeSupplier cache;

        public DoubleNodeRowSerializer( CachedNodeSupplier cache ) {
            this.cache = cache;
        }

        @Override
        public void serialize( DataOutput out,
                               DoubleNodeRow value ) throws IOException {
            out.writeUTF(serializeNodeKey(value.getNode()));
            out.writeUTF(serializeNodeKey(value.getNode(1)));
            out.writeFloat(value.getScore());
            out.writeFloat(value.getScore(1));
        }

        @Override
        public DoubleNodeRow deserialize( DataInput in,
                                          int available ) throws IOException {
            CachedNode node1 = deserializeNodeKey(in.readUTF(), cache);
            CachedNode node2 = deserializeNodeKey(in.readUTF(), cache);
            return new DoubleNodeRow(node1, node2, in.readFloat(), in.readFloat());
        }

        @Override
        public DoubleNodeRow createRow( Batch currentRow ) {
            return new DoubleNodeRow(currentRow.getNode(), currentRow.getNode(1), currentRow.getScore(), currentRow.getScore(1));
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }

        @Override
        public String toString() {
            return "DoubleNodeRowSerializer";
        }
    }

    protected static final class TripleNodeRowSerializer implements BufferedRowFactory<TripleNodeRow>, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient CachedNodeSupplier cache;

        public TripleNodeRowSerializer( CachedNodeSupplier cache ) {
            this.cache = cache;
        }

        @Override
        public void serialize( DataOutput out,
                               TripleNodeRow value ) throws IOException {
            out.writeUTF(serializeNodeKey(value.getNode()));
            out.writeUTF(serializeNodeKey(value.getNode(1)));
            out.writeUTF(serializeNodeKey(value.getNode(2)));
            out.writeFloat(value.getScore());
            out.writeFloat(value.getScore(1));
            out.writeFloat(value.getScore(2));
        }

        @Override
        public TripleNodeRow deserialize( DataInput in,
                                          int available ) throws IOException {
            CachedNode node1 = deserializeNodeKey(in.readUTF(), cache);
            CachedNode node2 = deserializeNodeKey(in.readUTF(), cache);
            CachedNode node3 = deserializeNodeKey(in.readUTF(), cache);
            return new TripleNodeRow(node1, node2, node3, in.readFloat(), in.readFloat(), in.readFloat());
        }

        @Override
        public TripleNodeRow createRow( Batch currentRow ) {
            return new TripleNodeRow(currentRow.getNode(), currentRow.getNode(1), currentRow.getNode(2), currentRow.getScore(),
                                     currentRow.getScore(1), currentRow.getScore(2));
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }

        @Override
        public String toString() {
            return "TripleNodeRowSerializer";
        }
    }

    protected static final class QuadNodeRowSerializer implements BufferedRowFactory<QuadNodeRow>, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient CachedNodeSupplier cache;

        public QuadNodeRowSerializer( CachedNodeSupplier cache ) {
            this.cache = cache;
        }

        @Override
        public void serialize( DataOutput out,
                               QuadNodeRow value ) throws IOException {
            out.writeUTF(serializeNodeKey(value.getNode()));
            out.writeUTF(serializeNodeKey(value.getNode(1)));
            out.writeUTF(serializeNodeKey(value.getNode(2)));
            out.writeUTF(serializeNodeKey(value.getNode(3)));
            out.writeFloat(value.getScore());
            out.writeFloat(value.getScore(1));
            out.writeFloat(value.getScore(2));
            out.writeFloat(value.getScore(3));
        }

        @Override
        public QuadNodeRow deserialize( DataInput in,
                                        int available ) throws IOException {
            CachedNode node1 = deserializeNodeKey(in.readUTF(), cache);
            CachedNode node2 = deserializeNodeKey(in.readUTF(), cache);
            CachedNode node3 = deserializeNodeKey(in.readUTF(), cache);
            CachedNode node4 = deserializeNodeKey(in.readUTF(), cache);
            return new QuadNodeRow(node1, node2, node3, node4, in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat());
        }

        @Override
        public QuadNodeRow createRow( Batch currentRow ) {
            return new QuadNodeRow(currentRow.getNode(), currentRow.getNode(1), currentRow.getNode(2), currentRow.getNode(3),
                                   currentRow.getScore(), currentRow.getScore(1), currentRow.getScore(2), currentRow.getScore(3));
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }

        @Override
        public String toString() {
            return "QuadNodeRowSerializer";
        }
    }

    protected static final class MultiNodeRowSerializer implements BufferedRowFactory<MultiNodeRow>, Serializable {
        private static final long serialVersionUID = 1L;
        private final transient CachedNodeSupplier cache;
        private final int width;

        public MultiNodeRowSerializer( CachedNodeSupplier cache,
                                       int width ) {
            this.cache = cache;
            this.width = width;
        }

        @Override
        public void serialize( DataOutput out,
                               MultiNodeRow value ) throws IOException {
            for (int i = 0; i != width; ++i) {
                out.writeUTF(serializeNodeKey(value.getNode(i)));
                out.writeFloat(value.getScore(i));
            }
        }

        @Override
        public MultiNodeRow deserialize( DataInput in,
                                         int available ) throws IOException {
            CachedNode[] nodes = new CachedNode[width];
            float[] scores = new float[width];
            for (int i = 0; i != width; ++i) {
                nodes[i] = deserializeNodeKey(in.readUTF(), cache);
                scores[i] = in.readFloat();
            }
            return new MultiNodeRow(nodes, scores);
        }

        @Override
        public MultiNodeRow createRow( Batch currentRow ) {
            CachedNode[] nodes = new CachedNode[width];
            float[] scores = new float[width];
            for (int i = 0; i != width; ++i) {
                nodes[i] = currentRow.getNode(i);
                scores[i] = currentRow.getScore(i);
            }
            return new MultiNodeRow(nodes, scores);
        }

        @Override
        public int fixedSize() {
            return -1; // not fixed size
        }

        @Override
        public String toString() {
            return "MultiNodeRowSerializer";
        }
    }
}
