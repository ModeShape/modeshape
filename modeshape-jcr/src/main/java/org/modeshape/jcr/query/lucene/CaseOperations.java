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
package org.modeshape.jcr.query.lucene;

import org.modeshape.common.annotation.Immutable;

/**
 * A set of functions that can be used to operate upon the case of a string stored in the indexes before being evaluated against
 * criteria from a query.
 */
@Immutable
public class CaseOperations {

    /**
     * A function that can be used to operate upon the case of a string stored in the indexes before being evaluated against
     * criteria from a query.
     */
    public static interface CaseOperation {

        /**
         * Perform the operation.
         * 
         * @param input the input string; never null
         * @return the output string; never null
         */
        String execute( String input );
    }

    /**
     * The CaseOperation instance that leaves as is the string used within the indexes before being evaluated.
     */
    public static final CaseOperation AS_IS = NoOperation.INSTANCE;

    /**
     * The CaseOperation instance that lowercases the string used within the indexes before being evaluated.
     */
    public static final CaseOperation LOWERCASE = LowercaseOperation.INSTANCE;

    /**
     * The CaseOperation instance that uppercases the string used within the indexes before being evaluated.
     */
    public static final CaseOperation UPPERCASE = UppercaseOperation.INSTANCE;

    private final static class NoOperation implements CaseOperation {
        protected static final CaseOperation INSTANCE = new NoOperation();

        private NoOperation() {
        }

        @Override
        public String execute( String input ) {
            return input;
        }
    }

    private final static class UppercaseOperation implements CaseOperation {
        protected static final CaseOperation INSTANCE = new UppercaseOperation();

        private UppercaseOperation() {
        }

        @Override
        public String execute( String input ) {
            return input.toUpperCase();
        }
    }

    private final static class LowercaseOperation implements CaseOperation {
        protected static final CaseOperation INSTANCE = new LowercaseOperation();

        private LowercaseOperation() {
        }

        @Override
        public String execute( String input ) {
            return input.toLowerCase();
        }
    }

}
