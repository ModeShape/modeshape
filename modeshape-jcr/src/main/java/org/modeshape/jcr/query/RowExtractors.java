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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.RowAccessor;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.NullOrder;
import org.modeshape.jcr.query.model.Order;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * Standard {@link ExtractFromRow} implementations.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RowExtractors {

    /**
     * Obtain a new {@link ExtractFromRow} instance that uses the supplied row extractor but converts the value to the supplied
     * type.
     * 
     * @param extractor the extractor that obtains a value from the row; may not be null
     * @param newType the factory for the desired type; may not be null
     * @return the converting row extractor instance; never null
     */
    public static ExtractFromRow convert( final ExtractFromRow extractor,
                                          final TypeFactory<?> newType ) {
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return newType;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                Object value = extractor.getValueInRow(row);
                if (value == null) return value;
                if (value instanceof Object[]) {
                    Object[] values = (Object[])value;
                    for (int i = 0; i != values.length; ++i) {
                        values[i] = newType.create(values[i]);
                    }
                    return values;
                }
                return newType.create(extractor.getValueInRow(row));
            }

            @Override
            public String toString() {
                return "(as-string " + extractor.toString() + " )";
            }
        };
    }

    /**
     * Obtain a new {@link ExtractFromRow} instance that will extract the full text for a node.
     * <p>
     * Note that if the node is null at the specified index in the row, the extractor's
     * {@link ExtractFromRow#getValueInRow(RowAccessor)} will return null.
     * </p>
     * 
     * @param indexInRow the index of the selector in the row; presumed to be valid
     * @param cache the node cache; may not be null
     * @param types the system of types; may not be null
     * @param binaries the binary store; may not be null
     * @return the object that extracts a value from the row; never null
     */
    public static ExtractFromRow extractFullText( final int indexInRow,
                                                  final NodeCache cache,
                                                  TypeSystem types,
                                                  final BinaryStore binaries ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                StringBuilder fullTextString = new StringBuilder();
                Name name = node.getName(cache);
                fullTextString.append(name.getLocalName());
                Iterator<Property> iter = node.getProperties(cache);
                while (iter.hasNext()) {
                    extractFullTextFrom(iter.next(), type, fullTextString, binaries, node, cache);
                }
                if (trace) NodeSequence.LOGGER.trace("Extracting full-text from {0}: {1}", node.getPath(cache), fullTextString);
                // There should always be some content, since every node except the root has a name and even the
                // root node has some properties that will be converted to full-text ...
                return fullTextString.toString();
            }

            @Override
            public String toString() {
                return "(extract-full-text)";
            }
        };
    }

    /**
     * Obtain a new {@link ExtractFromRow} instance that will extract the full text for a single property of a node.
     * <p>
     * Note that if the named property does not exist on a node or the node is null at the specified index in the row, the
     * extractor's {@link ExtractFromRow#getValueInRow(RowAccessor)} will return null.
     * </p>
     * 
     * @param indexInRow the index of the selector in the row; presumed to be valid
     * @param cache the node cache; may not be null
     * @param propertyName the name of the property from which the full text is to be extracted; may not be null
     * @param types the system of types; may not be null
     * @param binaries the binary store; may not be null
     * @return the object that extracts a value from the row; never null
     */
    public static ExtractFromRow extractFullText( final int indexInRow,
                                                  final NodeCache cache,
                                                  final Name propertyName,
                                                  TypeSystem types,
                                                  final BinaryStore binaries ) {
        final TypeFactory<String> type = types.getStringFactory();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Property prop = node.getProperty(propertyName, cache);
                if (prop == null || prop.isEmpty()) return null;
                StringBuilder fullTextString = new StringBuilder();
                extractFullTextFrom(prop, type, fullTextString, binaries, node, cache);
                return fullTextString.toString();
            }

            @Override
            public String toString() {
                return "(extract-full-text property=" + type.create(propertyName) + ")";
            }
        };
    }

    protected static void extractFullTextFrom( Property property,
                                               TypeFactory<String> type,
                                               StringBuilder fullTextString,
                                               BinaryStore binaries,
                                               CachedNode node,
                                               NodeCache cache ) {
        for (Object value : property) {
            extractFullTextFrom(value, type, binaries, fullTextString);
        }
    }

    public static void extractFullTextFrom( Object propertyValue,
                                            TypeFactory<String> type,
                                            BinaryStore binaries,
                                            StringBuilder fullTextString ) {
        if (propertyValue instanceof Binary && binaries != null) {
            // Try extracting the text from the binary value ...
            try {
                propertyValue = binaries.getText((BinaryValue)propertyValue);
            } catch (BinaryStoreException e) {
                NodeSequence.LOGGER.debug("Error getting full text from binary {0}", propertyValue);
            }
        }
        if (propertyValue != null) {
            // Convert all other types to a string ...
            fullTextString.append(' ').append(type.create(propertyValue));
        }
    }

    /**
     * Create an extractor that extracts the {@link NodeKey} from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the node key extractor; never null
     */
    public static ExtractFromRow extractNodeKey( final int indexInRow,
                                                 final NodeCache cache,
                                                 TypeSystem types ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                if (trace) NodeSequence.LOGGER.trace("Extracting node key from {0}: {1}", node.getPath(cache), node.getKey());
                return node.getKey().toString();
            }

            @Override
            public String toString() {
                return "(extract-node-key)";
            }
        };
    }

    /**
     * Create an extractor that extracts the parent's {@link NodeKey} from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the node key extractor; never null
     */
    public static ExtractFromRow extractParentNodeKey( final int indexInRow,
                                                       final NodeCache cache,
                                                       TypeSystem types ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                NodeKey parentKey = node.getParentKey(cache);
                if (trace) NodeSequence.LOGGER.trace("Extracting parent key from {0}: {1}", node.getPath(cache), parentKey);
                return parentKey != null ? parentKey.toString() : null;
            }

            @Override
            public String toString() {
                return "(extract-parent-key)";
            }
        };
    }

    /**
     * Create an extractor that extracts the path from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractPath( final int indexInRow,
                                              final NodeCache cache,
                                              TypeSystem types ) {
        final TypeFactory<Path> type = types.getPathFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Path> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Path path = node.getPath(cache);
                if (trace) NodeSequence.LOGGER.trace("Extracting path from {0}", path);
                return path;
            }

            @Override
            public String toString() {
                return "(extract-path)";
            }
        };
    }

    /**
     * Create an extractor that extracts the parent path from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractParentPath( final int indexInRow,
                                                    final NodeCache cache,
                                                    TypeSystem types ) {
        final TypeFactory<Path> type = types.getPathFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Path> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                NodeKey parentKey = node.getParentKey(cache);
                if (parentKey == null) return null;
                CachedNode parent = cache.getNode(parentKey);
                if (parent == null) return null;
                Path parentPath = parent.getPath(cache);
                if (trace) NodeSequence.LOGGER.trace("Extracting parent path from {0}: {1}", node.getPath(cache), parentPath);
                return parentPath;
            }

            @Override
            public String toString() {
                return "(extract-parent-path)";
            }
        };
    }

    /**
     * Create an extractor that extracts the path from the node at the given position in the row and applies the relative path.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param relativePath the relative path that should be applied to the nodes' path; may not be null and must be a valid
     *        relative path
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractRelativePath( final int indexInRow,
                                                      final Path relativePath,
                                                      final NodeCache cache,
                                                      TypeSystem types ) {
        CheckArg.isNotNull(relativePath, "relativePath");
        final TypeFactory<Path> type = types.getPathFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Path> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Path nodePath = node.getPath(cache);
                try {
                    Path path = nodePath.resolve(relativePath);
                    if (trace) NodeSequence.LOGGER.trace("Extracting relative path {2} from {0}: {2}", node.getPath(cache),
                                                         relativePath, path);
                    return path;
                } catch (InvalidPathException e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return "(extract-relative-path)";
            }
        };
    }

    /**
     * Create an extractor that extracts the name from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractName( final int indexInRow,
                                              final NodeCache cache,
                                              TypeSystem types ) {
        final TypeFactory<Name> type = types.getNameFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Name> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Name name = node.getName(cache);
                if (trace) NodeSequence.LOGGER.trace("Extracting name from {0}: {1}", node.getPath(cache), name);
                return name;
            }

            @Override
            public String toString() {
                return "(extract-name)";
            }
        };
    }

    /**
     * Create an extractor that extracts the name from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractLocalName( final int indexInRow,
                                                   final NodeCache cache,
                                                   TypeSystem types ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = NodeSequence.LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                String name = node.getName(cache).getLocalName();
                if (trace) NodeSequence.LOGGER.trace("Extracting name from {0}: {1}", node.getPath(cache), name);
                return name;
            }

            @Override
            public String toString() {
                return "(extract-local-name)";
            }
        };
    }

    /**
     * Create an extractor that extracts an object that uniquely identifies the row. The object will be either a {@link NodeKey}
     * (if rowWidth is 1), or a {@link Tuples tuple} containing each of the {@link NodeKey}s of the nodes in the row. Note that
     * any of the node keys can be null, but this extractor will never return a null object.
     * 
     * @param rowWidth the number of nodes in each row; must be positive
     * @param types the types system; may not be null
     * @return the extractor; never null
     */
    public static ExtractFromRow extractUniqueKey( final int rowWidth,
                                                   TypeSystem types ) {
        final TypeFactory<NodeKey> keyType = types.getNodeKeyFactory();
        assert rowWidth > 0;
        if (rowWidth == 1) {
            return new ExtractFromRow() {
                @Override
                public TypeFactory<NodeKey> getType() {
                    return keyType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return NodeSequence.keyFor(row.getNode());
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-1-tuples)";
                }
            };
        }
        if (rowWidth == 2) {
            final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, keyType);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return tupleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return Tuples.tuple(NodeSequence.keyFor(row.getNode(0)), NodeSequence.keyFor(row.getNode(1)));
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-2-tuples)";
                }
            };
        }
        if (rowWidth == 3) {
            final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, keyType, keyType);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return tupleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return Tuples.tuple(NodeSequence.keyFor(row.getNode(0)), NodeSequence.keyFor(row.getNode(1)),
                                        NodeSequence.keyFor(row.getNode(2)));
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-3-tuples)";
                }
            };
        }
        if (rowWidth == 4) {
            final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, keyType, keyType, keyType);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return tupleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return Tuples.tuple(NodeSequence.keyFor(row.getNode(0)), NodeSequence.keyFor(row.getNode(1)),
                                        NodeSequence.keyFor(row.getNode(2)), NodeSequence.keyFor(row.getNode(3)));
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-4-tuples)";
                }
            };
        }
        final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, rowWidth);
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return tupleType;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                Object[] keys = new Object[rowWidth];
                for (int i = 0; i != rowWidth; ++i) {
                    keys[i] = NodeSequence.keyFor(row.getNode(i));
                }
                return Tuples.tuple(keys);
            }

            @Override
            public String toString() {
                return "(extract-key-for-order-N-tuples)";
            }
        };
    }

    /**
     * Create an extractor that extracts the property value from the node at the given position in the row.
     * 
     * @param propertyName the name of the property; may not be null
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param desiredType the desired type, which should be converted from the actual value; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractPropertyValue( final Name propertyName,
                                                       final int indexInRow,
                                                       final NodeCache cache,
                                                       final TypeFactory<?> desiredType ) {
        return new ExtractFromRow() {
            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                org.modeshape.jcr.value.Property prop = node.getProperty(propertyName, cache);
                return prop == null ? null : desiredType.create(prop.getFirstValue());
            }

            @Override
            public TypeFactory<?> getType() {
                return desiredType;
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param first the first extractor; may not be null
     * @param second the second extractor; may not be null
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow first,
                                                final ExtractFromRow second ) {
        final TypeFactory<?> type = Tuples.typeFactory(first.getType(), second.getType());
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return Tuples.tuple(first.getValueInRow(row), second.getValueInRow(row));
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param first the first extractor; may not be null
     * @param second the second extractor; may not be null
     * @param third the third extractor; may not be null
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow first,
                                                final ExtractFromRow second,
                                                final ExtractFromRow third ) {
        final TypeFactory<?> type = Tuples.typeFactory(first.getType(), second.getType(), third.getType());
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return Tuples.tuple(first.getValueInRow(row), second.getValueInRow(row), third.getValueInRow(row));
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param first the first extractor; may not be null
     * @param second the second extractor; may not be null
     * @param third the third extractor; may not be null
     * @param fourth the fourth extractor; may not be null
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow first,
                                                final ExtractFromRow second,
                                                final ExtractFromRow third,
                                                final ExtractFromRow fourth ) {
        final TypeFactory<?> type = Tuples.typeFactory(first.getType(), second.getType(), third.getType(), fourth.getType());
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return Tuples.tuple(first.getValueInRow(row), second.getValueInRow(row), third.getValueInRow(row),
                                    fourth.getValueInRow(row));
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param extractors the extractors; may not be null or empty
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final Collection<ExtractFromRow> extractors ) {
        final int len = extractors.size();
        assert len > 0;
        // There are a few cases where specific row extractor implementations would be better ...
        if (len == 1) {
            return extractors.iterator().next();
        }
        if (len == 2) {
            Iterator<ExtractFromRow> iter = extractors.iterator();
            ExtractFromRow first = iter.next();
            ExtractFromRow second = iter.next();
            return extractorWith(first, second);
        }
        if (len == 3) {
            Iterator<ExtractFromRow> iter = extractors.iterator();
            ExtractFromRow first = iter.next();
            ExtractFromRow second = iter.next();
            ExtractFromRow third = iter.next();
            return extractorWith(first, second, third);
        }
        if (len == 4) {
            Iterator<ExtractFromRow> iter = extractors.iterator();
            ExtractFromRow first = iter.next();
            ExtractFromRow second = iter.next();
            ExtractFromRow third = iter.next();
            ExtractFromRow fourth = iter.next();
            return extractorWith(first, second, third, fourth);
        }
        // Okay, there are at least 4 extractors, so we need to return a general-case row extractor ...
        Collection<TypeFactory<?>> types = new ArrayList<TypeFactory<?>>();
        final ExtractFromRow[] extracts = new ExtractFromRow[len];
        int i = 0;
        for (ExtractFromRow extractor : extractors) {
            extracts[i++] = extractor;
            types.add(extractor.getType());
        }
        final TypeFactory<?> type = Tuples.typeFactory(types);
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                Object[] values = new Object[len];
                for (int i = 0; i != len; ++i) {
                    values[i] = extracts[i].getValueInRow(row);
                }
                return Tuples.tuple(values);
            }
        };
    }

    /**
     * Create an extractor that has a {@link ExtractFromRow#getType() type factory} with a {@link TypeFactory#getComparator()
     * comparator} that sorts according to the specified order and null-order behavior.
     * 
     * @param extractor the original extractor; may not be null
     * @param order the specification of whether the comparator should order ascending or descending; may not be null
     * @param nullOrder the specification of whether null values should appear first or last; may not be null
     * @return the extractor that is identical except that it has an inverted comparator; never null
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow extractor,
                                                final Order order,
                                                final NullOrder nullOrder ) {
        final TypeFactory<?> type = TypeSystem.with(extractor.getType(), order, nullOrder);
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return extractor.getValueInRow(row);
            }

            @Override
            public String toString() {
                return extractor.toString() + " " + order + " " + nullOrder;
            }
        };
    }

    private RowExtractors() {
    }

    /**
     * An operation that extracts a single value from the current row in a given {@link Batch}. Note that a single instance will
     * be called many times (once for each row in the batches of a {@link NodeSequence}).
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface ExtractFromRow {

        /**
         * Get the type of value that this extractor will return from {@link #getValueInRow}.
         * 
         * @return the type; never null
         */
        TypeFactory<?> getType();

        /**
         * Evaluate the {@link DynamicOperand} against the current row in the supplied batch. Note that implementations will need
         * to know whether to get the {@link Batch#getNode() only node in the row} or how to get a {@link Batch#getNode(int)} for
         * a specific selector.
         * 
         * @param row the row accessor through which the current row values can be obtained; never null
         * @return the dynamic operands value for the current row; may be null
         */
        Object getValueInRow( RowAccessor row );
    }

}
