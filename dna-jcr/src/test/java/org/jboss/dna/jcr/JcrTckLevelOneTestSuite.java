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
 * Test suite that includes the Level 1 JCR TCK API tests from the Jackrabbit project.
 * 
 * @see JcrTckLevelTwoTestSuite
 * @see JcrTckOptionalTestSuite
 * @see JcrTckTest
 */
public class JcrTckLevelOneTestSuite extends TestCase {

    /**
     * Returns a <code>Test</code> suite that executes all tests inside this package.
     * 
     * @return a <code>Test</code> suite that executes all tests inside this package.
     */
    public static Test suite() {
        TestSuite suite = new TestSuite("JCR 1.0 Level 1 API tests");

        // We currently don't pass the tests in those suites that are commented out
        // See https://jira.jboss.org/jira/browse/DNA-285

        suite.addTestSuite(org.apache.jackrabbit.test.api.RootNodeTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.NodeReadMethodsTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.PropertyTypeTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.BinaryPropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.BooleanPropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.DatePropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.DoublePropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.LongPropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.NamePropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.PathPropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.ReferencePropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.StringPropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.UndefinedPropertyTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.NamespaceRemappingTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.NodeIteratorTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.PropertyReadMethodsTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.RepositoryDescriptorTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.SessionReadMethodsTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.ReferenceableRootNodesTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.ExportSysViewTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.ExportDocViewTest.class);
        suite.addTestSuite(org.apache.jackrabbit.test.api.RepositoryLoginTest.class);

        // These might not all be level one tests
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.XPathPosIndexTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.XPathDocOrderTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.XPathOrderByTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.XPathJcrPathTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.GetLanguageTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.GetPersistentQueryPathLevel1Test.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.GetStatementTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.GetPropertyNamesTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.PredicatesTest.class);
        // suite.addTestSuite(org.apache.jackrabbit.test.api.query.SimpleSelectionTest.class);

        // The tests in this suite are level one
        // suite.addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());

        return suite;
    }
}
