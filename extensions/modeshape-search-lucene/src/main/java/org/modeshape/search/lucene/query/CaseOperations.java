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
package org.modeshape.search.lucene.query;

/**
 * A set of functions that can be used to operate upon the case of a string stored in the indexes before being evaluated against
 * criteria from a query.
 */
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
