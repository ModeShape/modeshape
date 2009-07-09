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
import org.apache.jackrabbit.test.api.AddNodeTest;
import org.apache.jackrabbit.test.api.CheckPermissionTest;
import org.apache.jackrabbit.test.api.ImpersonateTest;
import org.apache.jackrabbit.test.api.NamespaceRegistryTest;
import org.apache.jackrabbit.test.api.NodeAddMixinTest;
import org.apache.jackrabbit.test.api.NodeCanAddMixinTest;
import org.apache.jackrabbit.test.api.NodeItemIsModifiedTest;
import org.apache.jackrabbit.test.api.NodeItemIsNewTest;
import org.apache.jackrabbit.test.api.NodeOrderableChildNodesTest;
import org.apache.jackrabbit.test.api.NodeRemoveMixinTest;
import org.apache.jackrabbit.test.api.NodeTest;
import org.apache.jackrabbit.test.api.PropertyItemIsModifiedTest;
import org.apache.jackrabbit.test.api.PropertyItemIsNewTest;
import org.apache.jackrabbit.test.api.PropertyTest;
import org.apache.jackrabbit.test.api.RepositoryLoginTest;
import org.apache.jackrabbit.test.api.SetPropertyAssumeTypeTest;
import org.apache.jackrabbit.test.api.SetPropertyBooleanTest;
import org.apache.jackrabbit.test.api.SetPropertyCalendarTest;
import org.apache.jackrabbit.test.api.SetPropertyConstraintViolationExceptionTest;
import org.apache.jackrabbit.test.api.SetPropertyDoubleTest;
import org.apache.jackrabbit.test.api.SetPropertyInputStreamTest;
import org.apache.jackrabbit.test.api.SetPropertyLongTest;
import org.apache.jackrabbit.test.api.SetPropertyNodeTest;
import org.apache.jackrabbit.test.api.SetPropertyStringTest;
import org.apache.jackrabbit.test.api.SetPropertyValueTest;
import org.apache.jackrabbit.test.api.SetValueBinaryTest;
import org.apache.jackrabbit.test.api.SetValueBooleanTest;
import org.apache.jackrabbit.test.api.SetValueConstraintViolationExceptionTest;
import org.apache.jackrabbit.test.api.SetValueDateTest;
import org.apache.jackrabbit.test.api.SetValueDoubleTest;
import org.apache.jackrabbit.test.api.SetValueLongTest;
import org.apache.jackrabbit.test.api.SetValueReferenceTest;
import org.apache.jackrabbit.test.api.SetValueStringTest;
import org.apache.jackrabbit.test.api.SetValueValueFormatExceptionTest;
import org.apache.jackrabbit.test.api.SetValueVersionExceptionTest;
import org.apache.jackrabbit.test.api.ValueFactoryTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopySameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveVersionableTest;

/**
 * Test suite to wrap Apache Jackrabbit JCR technology compatibility kit (TCK) unit tests. Note that technically these are not the
 * actual TCK, but these are unit tests that happen to be similar to (or provided the basis for) a subset of the TCK.
 */
public class JcrTckTest {

    /**
     * 
     */
    public JcrTckTest() {
    }

    /**
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the DNA Maven test target.
     * 
     * @return a new instance of {@link JCRTestSuite}.
     */
    public static Test suite() {
        // Uncomment this to execute all tests
        // return new JCRTestSuite();

        // Or uncomment the following lines to execute the different sets/suites of tests ...
        TestSuite suite = new TestSuite("JCR 1.0 API tests");

        suite.addTest(new LevelOneFeatureTests());
        suite.addTest(new LevelTwoFeatureTests());
        suite.addTest(new OptionalFeatureTests());

        return suite;
    }

    /**
     * Test suite that includes the Level 1 JCR TCK API tests from the Jackrabbit project.
     */
    private static class LevelOneFeatureTests extends TestSuite {
        protected LevelOneFeatureTests() {
            super("JCR Level 1 API Tests");
            // We currently don't pass the tests in those suites that are commented out
            // See https://jira.jboss.org/jira/browse/DNA-285

            addTestSuite(org.apache.jackrabbit.test.api.RootNodeTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NodeReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.PropertyTypeTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.BinaryPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.BooleanPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.DatePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.DoublePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.LongPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NamePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.PathPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.ReferencePropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.StringPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.UndefinedPropertyTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NamespaceRemappingTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.NodeIteratorTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.PropertyReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.RepositoryDescriptorTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.SessionReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.ReferenceableRootNodesTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.ExportSysViewTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.ExportDocViewTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.RepositoryLoginTest.class);

            // These might not all be level one tests
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathPosIndexTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathDocOrderTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathOrderByTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.XPathJcrPathTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test.class);
            addTestSuite(org.apache.jackrabbit.test.api.query.GetLanguageTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.query.GetPersistentQueryPathLevel1Test.class);
            addTestSuite(org.apache.jackrabbit.test.api.query.GetStatementTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.GetPropertyNamesTest.class);
            addTestSuite(org.apache.jackrabbit.test.api.query.PredicatesTest.class);
            // addTestSuite(org.apache.jackrabbit.test.api.query.SimpleSelectionTest.class);

            // The tests in this suite are level one
            addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());
        }
    }

    /**
     * Test suite that includes the Level 2 JCR TCK API tests from the Jackrabbit project.
     */
    private static class LevelTwoFeatureTests extends TestSuite {
        protected LevelTwoFeatureTests() {
            super("JCR Level 2 API Tests");
            // We currently don't pass the tests in those suites that are commented out
            // See https://jira.jboss.org/jira/browse/DNA-285

            // level 2 tests
            addTestSuite(AddNodeTest.class);
            addTestSuite(NamespaceRegistryTest.class);
            // addTestSuite(ReferencesTest.class);
            // dna-466 addTestSuite(SessionTest.class);
            // addTestSuite(SessionUUIDTest.class);
            addTestSuite(NodeTest.class);
            // addTestSuite(NodeUUIDTest.class);
            addTestSuite(NodeOrderableChildNodesTest.class);
            addTestSuite(PropertyTest.class);

            addTestSuite(SetValueBinaryTest.class);
            addTestSuite(SetValueBooleanTest.class);
            addTestSuite(SetValueDateTest.class);
            addTestSuite(SetValueDoubleTest.class);
            addTestSuite(SetValueLongTest.class);
            addTestSuite(SetValueReferenceTest.class);
            addTestSuite(SetValueStringTest.class);
            addTestSuite(SetValueConstraintViolationExceptionTest.class);
            addTestSuite(SetValueValueFormatExceptionTest.class);
            addTestSuite(SetValueVersionExceptionTest.class);

            addTestSuite(SetPropertyBooleanTest.class);
            addTestSuite(SetPropertyCalendarTest.class);
            addTestSuite(SetPropertyDoubleTest.class);
            addTestSuite(SetPropertyInputStreamTest.class);
            addTestSuite(SetPropertyLongTest.class);
            addTestSuite(SetPropertyNodeTest.class);
            addTestSuite(SetPropertyStringTest.class);
            addTestSuite(SetPropertyValueTest.class);
            addTestSuite(SetPropertyConstraintViolationExceptionTest.class);
            addTestSuite(SetPropertyAssumeTypeTest.class);

            addTestSuite(NodeItemIsModifiedTest.class);
            addTestSuite(NodeItemIsNewTest.class);
            addTestSuite(PropertyItemIsModifiedTest.class);
            addTestSuite(PropertyItemIsNewTest.class);

            addTestSuite(NodeAddMixinTest.class);
            addTestSuite(NodeCanAddMixinTest.class);
            addTestSuite(NodeRemoveMixinTest.class);

            // dna-466 addTestSuite(WorkspaceCloneReferenceableTest.class);
            addTestSuite(WorkspaceCloneSameNameSibsTest.class);
            // addTestSuite(WorkspaceCloneTest.class);
            addTestSuite(WorkspaceCloneVersionableTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesReferenceableTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesSameNameSibsTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesVersionableTest.class);
            addTestSuite(WorkspaceCopyReferenceableTest.class);
            addTestSuite(WorkspaceCopySameNameSibsTest.class);
            // addTestSuite(WorkspaceCopyTest.class);
            addTestSuite(WorkspaceCopyVersionableTest.class);
            // dna-466 addTestSuite(WorkspaceMoveReferenceableTest.class);
            addTestSuite(WorkspaceMoveSameNameSibsTest.class);
            // addTestSuite(WorkspaceMoveTest.class);
            addTestSuite(WorkspaceMoveVersionableTest.class);

            addTestSuite(RepositoryLoginTest.class);
            addTestSuite(ImpersonateTest.class);
            addTestSuite(CheckPermissionTest.class);

            // dna-466 addTestSuite(DocumentViewImportTest.class);
            // dna-466 addTestSuite(SerializationTest.class);

            addTestSuite(ValueFactoryTest.class);
        }
    }

    /**
     * Test suite that includes the Optional JCR TCK API tests from the Jackrabbit project.
     */
    private static class OptionalFeatureTests extends TestSuite {
        protected OptionalFeatureTests() {
            super("JCR Optional API Tests");
            // We currently don't pass the tests in those suites that are commented out
            // See https://jira.jboss.org/jira/browse/DNA-285

            // addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());
            // addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
            // addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
            addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());
        }
    }
}
