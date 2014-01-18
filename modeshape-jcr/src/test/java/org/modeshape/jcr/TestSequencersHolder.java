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
package org.modeshape.jcr;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;

/**
 * A simple sequencer that always fails with an exception.
 * 
 * @author Horia Chiorean
 */
public class TestSequencersHolder {

    public static final String DERIVED_NODE_NAME = "derivedNode";

    public static class FaultyDuringExecute extends Sequencer {
        @Override
        public boolean execute( Property inputProperty,
                                Node outputNode,
                                Sequencer.Context context ) throws Exception {
            throw new IllegalArgumentException("We're expecting to get this exception");
        }
    }

    public static class FaultyDuringInitialize extends Sequencer {
        public static final AtomicInteger EXECUTE_CALL_COUNTER = new AtomicInteger();

        @Override
        public void initialize( NamespaceRegistry registry,
                                NodeTypeManager nodeTypeManager ) {
            throw new RuntimeException("Expected during initialize");
        }

        @Override
        public boolean execute( Property inputProperty,
                                Node outputNode,
                                Context context ) throws Exception {
            EXECUTE_CALL_COUNTER.incrementAndGet();
            return true;
        }
    }

    /**
     * A simple sequencer that records the number of times all instances are {@link #execute executed}.
     */
    public static class DefaultSequencer extends Sequencer {

        @Override
        public boolean execute( Property inputProperty,
                                Node outputNode,
                                Context context ) throws Exception {
            CheckArg.isNotNull(inputProperty, "inputProperty");
            CheckArg.isNotNull(outputNode, "outputNode");
            CheckArg.isNotNull(context, "context");
            CheckArg.isNotNull(context.getTimestamp(), "context.getTimestamp()");
            outputNode.addNode(DERIVED_NODE_NAME);
            return true;
        }
    }

    /**
     * A sequencer which has different property types and is used to test the setting of property values.
     */
    @SuppressWarnings( "unused" )
    public static class SequencerWithProperties extends Sequencer {

        private List<Integer> intList;
        private Set<Integer> intSet;
        private Map<String, String> stringMap;

        private Integer intProp;
        private Integer[] integerArray;
        private int[] intArray;

        private Boolean booleanProp;
        private boolean[] booleanArray;

        private String stringProp;
        private String[] stringArray;

        private Long longProp;
        private Long[] longArray;

        private Double doubleProp;
        private Double[] doubleArray;

        private SequencerWithProperties subSequencer;
        // because of type erasure, the runtime instance will have List(Document) instances.
        private List<?> subSequencerList;

        /**
         * The only purpose of this sequencer is to validate that various property types can be set, hence all the null checks.
         * 
         * @see org.modeshape.jcr.SequencingTest#shouldSupportVariousPropertyTypes()
         */
        @Override
        public boolean execute( Property inputProperty,
                                Node outputNode,
                                Context context ) throws Exception {
            assertNotNull("intList not set", intList);
            assertNotNull("intSet not set", intSet);
            assertNotNull("stringMap not set", stringMap);
            assertNotNull("intProp not set", intProp);
            assertNotNull("integerArray not set", integerArray);
            assertNotNull("intArray not set", intArray);
            assertNotNull("intProp not set", intProp);
            assertNotNull("integerArray not set", integerArray);
            assertNotNull("booleanProp not set", booleanProp);
            assertNotNull("stringProp not set", stringProp);
            assertNotNull("stringArray not set", stringArray);
            assertNotNull("longProp not set", longProp);
            assertNotNull("longArray not set", longArray);
            assertNotNull("doubleProp not set", doubleProp);
            assertNotNull("doubleArray not set", doubleArray);
            assertNotNull("subSequener not set", subSequencer);
            assertNotNull("subSequencerList not set", subSequencerList);
            assertFalse(subSequencerList.isEmpty());

            outputNode.addNode(DERIVED_NODE_NAME);

            return true;
        }
    }

}
