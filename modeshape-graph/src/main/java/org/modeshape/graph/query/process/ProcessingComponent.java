/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.query.process;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.ArithmeticOperand;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.LowerCase;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.UpperCase;
import org.modeshape.graph.query.model.TypeSystem.TypeFactory;
import org.modeshape.graph.query.validate.Schemata;

/**
 * A component that performs (some) portion of the query processing by {@link #execute() returning the tuples} that result from
 * this stage of processing. Processing components are designed to be assembled into a processing structure, with a single
 * component at the top that returns the results of a query.
 */
@NotThreadSafe
public abstract class ProcessingComponent {

    private final QueryContext context;
    private final Columns columns;

    protected ProcessingComponent( QueryContext context,
                                   Columns columns ) {
        this.context = context;
        this.columns = columns;
        assert this.context != null;
        assert this.columns != null;
    }

    /**
     * Get the context in which this query is being executed.
     * 
     * @return context
     */
    public final QueryContext getContext() {
        return context;
    }

    /**
     * Get the column definitions.
     * 
     * @return the column mappings; never null
     */
    public final Columns getColumns() {
        return columns;
    }

    /**
     * Get the container for problems encountered during processing.
     * 
     * @return the problems container; never null
     */
    protected final Problems problems() {
        return context.getProblems();
    }

    /**
     * Execute this stage of processing and return the resulting tuples that each conform to the {@link #getColumns() columns}.
     * 
     * @return the list of tuples, where each tuple corresonds to the {@link #getColumns() columns}; never null
     */
    public abstract List<Object[]> execute();

    /**
     * Close these results, allowing any resources to be released.
     */
    public void close() {
    }

    /**
     * Utility method to create a new tuples list that is empty.
     * 
     * @return the empty tuples list; never null
     */
    protected List<Object[]> emptyTuples() {
        return new ArrayList<Object[]>(0);
    }

    /**
     * Interface for evaluating a {@link DynamicOperand} to return the resulting value.
     */
    protected static interface DynamicOperation {
        /**
         * Get the expected type of the result of this evaluation
         * 
         * @return the property type; never null
         */
        String getExpectedType();

        /**
         * Perform the dynamic evaluation to obtain the desired result.
         * 
         * @param tuple the tuple; never null
         * @return the value that results from dynamically evaluating the operand against the tuple; may be null
         */
        Object evaluate( Object[] tuple );
    }

    /**
     * Create a {@link DynamicOperation} instance that is able to evaluate the supplied {@link DynamicOperand}.
     * 
     * @param typeSystem the type system; may not be null
     * @param schemata the schemata; may not be null
     * @param columns the definition of the result columns and the tuples; may not be null
     * @param operand the dynamic operand that is to be evaluated by the returned object; may not be null
     * @return the dynamic operand operation; never null
     */
    protected DynamicOperation createDynamicOperation( final TypeSystem typeSystem,
                                                       Schemata schemata,
                                                       Columns columns,
                                                       DynamicOperand operand ) {
        assert operand != null;
        assert columns != null;
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            String propertyName = propValue.propertyName();
            String selectorName = propValue.selectorName().name();
            final int index = columns.getColumnIndexForProperty(selectorName, propertyName);
            // Find the expected property type of the value ...
            final String expectedType = columns.getColumnTypes().get(index);
            final TypeFactory<?> typeFactory = typeSystem.getTypeFactory(expectedType);
            return new DynamicOperation() {
                public String getExpectedType() {
                    return expectedType;
                }

                public Object evaluate( Object[] tuple ) {
                    return typeFactory.create(tuple[index]);
                }
            };
        }
        if (operand instanceof ReferenceValue) {
            ReferenceValue refValue = (ReferenceValue)operand;
            String propertyName = refValue.propertyName();
            String selectorName = refValue.selectorName().name();
            final int index = columns.getColumnIndexForProperty(selectorName, propertyName);
            // Find the expected property type of the value ...
            final String expectedType = columns.getColumnTypes().get(index);
            final TypeFactory<?> typeFactory = typeSystem.getTypeFactory(expectedType);
            return new DynamicOperation() {
                public String getExpectedType() {
                    return expectedType;
                }

                public Object evaluate( Object[] tuple ) {
                    return typeFactory.create(tuple[index]);
                }
            };
        }
        final TypeFactory<String> stringFactory = typeSystem.getStringFactory();
        if (operand instanceof Length) {
            Length length = (Length)operand;
            PropertyValue value = length.propertyValue();
            String propertyName = value.propertyName();
            String selectorName = value.selectorName().name();
            final int index = columns.getColumnIndexForProperty(selectorName, propertyName);
            // Find the expected property type of the value ...
            final String expectedType = columns.getColumnTypes().get(index);
            final TypeFactory<?> typeFactory = typeSystem.getTypeFactory(expectedType);
            final TypeFactory<Long> longFactory = typeSystem.getLongFactory();
            return new DynamicOperation() {
                public String getExpectedType() {
                    return longFactory.getTypeName(); // length is always LONG
                }

                public Object evaluate( Object[] tuple ) {
                    Object value = tuple[index];
                    return typeFactory.length(typeFactory.create(value));
                }
            };
        }
        if (operand instanceof LowerCase) {
            LowerCase lowerCase = (LowerCase)operand;
            final DynamicOperation delegate = createDynamicOperation(typeSystem, schemata, columns, lowerCase.operand());
            return new DynamicOperation() {
                public String getExpectedType() {
                    return stringFactory.getTypeName();
                }

                public Object evaluate( Object[] tuple ) {
                    String result = stringFactory.create(delegate.evaluate(tuple));
                    return result != null ? result.toLowerCase() : null;
                }
            };
        }
        if (operand instanceof UpperCase) {
            UpperCase upperCase = (UpperCase)operand;
            final DynamicOperation delegate = createDynamicOperation(typeSystem, schemata, columns, upperCase.operand());
            return new DynamicOperation() {
                public String getExpectedType() {
                    return stringFactory.getTypeName();
                }

                public Object evaluate( Object[] tuple ) {
                    String result = stringFactory.create(delegate.evaluate(tuple));
                    return result != null ? result.toUpperCase() : null;
                }
            };
        }
        if (operand instanceof NodeDepth) {
            NodeDepth nodeDepth = (NodeDepth)operand;
            final int locationIndex = columns.getLocationIndex(nodeDepth.selectorName().name());
            return new DynamicOperation() {
                public String getExpectedType() {
                    return typeSystem.getLongFactory().getTypeName(); // depth is always LONG
                }

                public Object evaluate( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    if (location == null) return null;
                    Path path = location.getPath();
                    assert path != null;
                    return new Long(path.size());
                }
            };
        }
        if (operand instanceof NodePath) {
            NodePath nodePath = (NodePath)operand;
            final int locationIndex = columns.getLocationIndex(nodePath.selectorName().name());
            return new DynamicOperation() {
                public String getExpectedType() {
                    return stringFactory.getTypeName();
                }

                public Object evaluate( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    if (location == null) return null;
                    assert location.getPath() != null;
                    return stringFactory.create(location.getPath());
                }
            };
        }
        if (operand instanceof NodeName) {
            NodeName nodeName = (NodeName)operand;
            final int locationIndex = columns.getLocationIndex(nodeName.selectorName().name());
            return new DynamicOperation() {
                public String getExpectedType() {
                    return stringFactory.getTypeName();
                }

                public Object evaluate( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    if (location == null) return null;
                    Path path = location.getPath();
                    assert path != null;
                    return path.isRoot() ? "" : stringFactory.create(location.getPath().getLastSegment().getName());
                }
            };
        }
        if (operand instanceof NodeLocalName) {
            NodeLocalName nodeName = (NodeLocalName)operand;
            final int locationIndex = columns.getLocationIndex(nodeName.selectorName().name());
            return new DynamicOperation() {
                public String getExpectedType() {
                    return stringFactory.getTypeName();
                }

                public Object evaluate( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    if (location == null) return null;
                    Path path = location.getPath();
                    assert path != null;
                    return path.isRoot() ? "" : location.getPath().getLastSegment().getName().getLocalName();
                }
            };
        }
        if (operand instanceof FullTextSearchScore) {
            FullTextSearchScore score = (FullTextSearchScore)operand;
            String selectorName = score.selectorName().name();
            final int index = columns.getFullTextSearchScoreIndexFor(selectorName);
            final TypeFactory<Double> doubleFactory = typeSystem.getDoubleFactory();
            if (index < 0) {
                // No full-text search score for this selector, so return 0.0d;
                return new DynamicOperation() {
                    public String getExpectedType() {
                        return doubleFactory.getTypeName();
                    }

                    public Object evaluate( Object[] tuple ) {
                        return new Double(0.0d);
                    }
                };
            }
            return new DynamicOperation() {
                public String getExpectedType() {
                    return doubleFactory.getTypeName();
                }

                public Object evaluate( Object[] tuple ) {
                    return tuple[index];
                }
            };
        }
        if (operand instanceof ArithmeticOperand) {
            ArithmeticOperand arith = (ArithmeticOperand)operand;
            final DynamicOperation leftOp = createDynamicOperation(typeSystem, schemata, columns, arith.left());
            final DynamicOperation rightOp = createDynamicOperation(typeSystem, schemata, columns, arith.right());
            // compute the expected (common) type ...
            String leftType = leftOp.getExpectedType();
            String rightType = rightOp.getExpectedType();
            final String commonType = typeSystem.getCompatibleType(leftType, rightType);
            if (typeSystem.getDoubleFactory().getTypeName().equals(commonType)) {
                final TypeFactory<Double> commonTypeFactory = typeSystem.getDoubleFactory();
                switch (arith.operator()) {
                    case ADD:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Double right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Double left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null) return left;
                                if (left == null) return right;
                                return left.doubleValue() / right.doubleValue();
                            }
                        };
                    case SUBTRACT:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Double right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Double left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null) return left;
                                if (left == null) left = 0.0d;
                                return left.doubleValue() * right.doubleValue();
                            }
                        };
                    case MULTIPLY:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Double right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Double left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null || left == null) return null;
                                return left.doubleValue() * right.doubleValue();
                            }
                        };
                    case DIVIDE:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Double right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Double left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null || left == null) return null;
                                return left.doubleValue() / right.doubleValue();
                            }
                        };
                }
            } else if (typeSystem.getLongFactory().getTypeName().equals(commonType)) {
                final TypeFactory<Long> commonTypeFactory = typeSystem.getLongFactory();
                switch (arith.operator()) {
                    case ADD:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Long right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Long left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null) return left;
                                if (left == null) return right;
                                return left.longValue() / right.longValue();
                            }
                        };
                    case SUBTRACT:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Long right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Long left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null) return left;
                                if (left == null) left = 0L;
                                return left.longValue() * right.longValue();
                            }
                        };
                    case MULTIPLY:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Long right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Long left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null || left == null) return null;
                                return left.longValue() * right.longValue();
                            }
                        };
                    case DIVIDE:
                        return new DynamicOperation() {
                            public String getExpectedType() {
                                return commonType;
                            }

                            public Object evaluate( Object[] tuple ) {
                                Long right = commonTypeFactory.create(rightOp.evaluate(tuple));
                                Long left = commonTypeFactory.create(leftOp.evaluate(tuple));
                                if (right == null || left == null) return null;
                                return left.longValue() / right.longValue();
                            }
                        };
                }
            }
        }
        assert false;
        return null;
    }

    protected Comparator<Object[]> createSortComparator( QueryContext context,
                                                         Columns columns ) {
        assert context != null;
        final int numLocations = columns.getLocationCount();
        assert numLocations > 0;
        final Comparator<Location> typeComparator = Location.comparator();
        if (numLocations == 1) {
            // We can do this a tad faster if we know there is only one Location object ...
            final int locationIndex = columns.getColumnCount();
            return new Comparator<Object[]>() {
                public int compare( Object[] tuple1,
                                    Object[] tuple2 ) {
                    Location value1 = (Location)tuple1[locationIndex];
                    Location value2 = (Location)tuple2[locationIndex];
                    return typeComparator.compare(value1, value2);
                }
            };
        }
        final int firstLocationIndex = columns.getColumnCount();
        return new Comparator<Object[]>() {
            public int compare( Object[] tuple1,
                                Object[] tuple2 ) {
                int result = 0;
                for (int locationIndex = firstLocationIndex; locationIndex != numLocations; ++locationIndex) {
                    Location value1 = (Location)tuple1[locationIndex];
                    Location value2 = (Location)tuple2[locationIndex];
                    result = typeComparator.compare(value1, value2);
                    if (result != 0) return result;
                }
                return result;
            }
        };
    }
}
