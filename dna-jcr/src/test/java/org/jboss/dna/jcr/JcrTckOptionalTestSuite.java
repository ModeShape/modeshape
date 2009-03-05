/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Test suite that includes the Optional JCR TCK API tests from the Jackrabbit project.
 * 
 * @see JcrTckLevelOneTestSuite
 * @see JcrTckLevelTwoTestSuite
 * @see JcrTckTest
 */
public class JcrTckOptionalTestSuite extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this package.
     * 
     * @return a <code>Test</code> suite that executes all tests inside this package.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("JCR 1.0 Optional API tests");

        // We currently don't pass the tests in those suites that are commented out
        // See https://jira.jboss.org/jira/browse/DNA-285

        suite.addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());

        return suite;
    }
}
