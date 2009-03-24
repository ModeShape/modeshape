/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jcr;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.jackrabbit.test.JCRTestSuite;

/**
 * Test suite to wrap Apache Jackrabbit JCR technology compatibility kit (TCK) unit tests for the compliance of the optional
 * version feature. Note that technically these are not the actual TCK, but these are unit tests that happen to be similar to (or
 * provided the basis for) a subset of the TCK.
 */
public class Jsr170OptionalVersionTckTest {

    /**
     * 
     */
    public Jsr170OptionalVersionTckTest() {
    }

    /**
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the DNA Maven test target.
     * 
     * @return a new instance of {@link JCRTestSuite}.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("JCR 1.0 Optional Feature (Version) Compliance tests");

        suite.addTest(new OptionalVersionFeatureTests());

        return suite;
    }

    /**
     * Test suite for the version tests.
     */
    private static class OptionalVersionFeatureTests extends TestSuite {
        protected OptionalVersionFeatureTests() {
            super("JCR Optional Feature (Version) Tests");

            addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
        }
    }

}
