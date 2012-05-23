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

package org.modeshape.jcr;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite to wrap Apache Jackrabbit JCR technology compatibility kit (TCK) unit tests. Note that technically these are not the
 * actual TCK, but these are unit tests that happen to be similar to (or provided the basis for) a subset of the TCK.
 */
public class JcrTckTest {

    /**
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the ModeShape Maven test target.
     * 
     * @return a new instance of {@link org.apache.jackrabbit.test.JCRTestSuite}.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("JCR API tests");
        suite.addTest(org.apache.jackrabbit.test.api.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.query.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.query.qom.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
        //TODO author=Horia Chiorean date=5/23/12 description=Should we include the simple versioning tests - some of the tests are incompatible with full versioning
        suite.addTest(org.apache.jackrabbit.test.api.version.simple.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.retention.TestAll.suite());
        suite.addTest(org.apache.jackrabbit.test.api.security.TestAll.suite());

        return suite;
   }
}
