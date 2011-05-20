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
import org.apache.jackrabbit.test.JCRTestSuite;
import org.apache.jackrabbit.test.api.AddNodeTest;
import org.apache.jackrabbit.test.api.BinaryPropertyTest;
import org.apache.jackrabbit.test.api.BooleanPropertyTest;
import org.apache.jackrabbit.test.api.CheckPermissionTest;
import org.apache.jackrabbit.test.api.DatePropertyTest;
import org.apache.jackrabbit.test.api.DocumentViewImportTest;
import org.apache.jackrabbit.test.api.DoublePropertyTest;
import org.apache.jackrabbit.test.api.ExportDocViewTest;
import org.apache.jackrabbit.test.api.ExportSysViewTest;
import org.apache.jackrabbit.test.api.GetWeakReferencesTest;
import org.apache.jackrabbit.test.api.HasPermissionTest;
import org.apache.jackrabbit.test.api.ImpersonateTest;
import org.apache.jackrabbit.test.api.LifecycleTest;
import org.apache.jackrabbit.test.api.LongPropertyTest;
import org.apache.jackrabbit.test.api.NamePropertyTest;
import org.apache.jackrabbit.test.api.NameTest;
import org.apache.jackrabbit.test.api.NamespaceRegistryReadMethodsTest;
import org.apache.jackrabbit.test.api.NamespaceRegistryTest;
import org.apache.jackrabbit.test.api.NamespaceRemappingTest;
import org.apache.jackrabbit.test.api.NodeAddMixinTest;
import org.apache.jackrabbit.test.api.NodeCanAddMixinTest;
import org.apache.jackrabbit.test.api.NodeDiscoveringNodeTypesTest;
import org.apache.jackrabbit.test.api.NodeItemIsModifiedTest;
import org.apache.jackrabbit.test.api.NodeItemIsNewTest;
import org.apache.jackrabbit.test.api.NodeIteratorTest;
import org.apache.jackrabbit.test.api.NodeOrderableChildNodesTest;
import org.apache.jackrabbit.test.api.NodeReadMethodsTest;
import org.apache.jackrabbit.test.api.NodeRemoveMixinTest;
import org.apache.jackrabbit.test.api.NodeSetPrimaryTypeTest;
import org.apache.jackrabbit.test.api.NodeTest;
import org.apache.jackrabbit.test.api.NodeUUIDTest;
import org.apache.jackrabbit.test.api.PathPropertyTest;
import org.apache.jackrabbit.test.api.PathTest;
import org.apache.jackrabbit.test.api.PropertyItemIsModifiedTest;
import org.apache.jackrabbit.test.api.PropertyItemIsNewTest;
import org.apache.jackrabbit.test.api.PropertyReadMethodsTest;
import org.apache.jackrabbit.test.api.PropertyTest;
import org.apache.jackrabbit.test.api.PropertyTypeTest;
import org.apache.jackrabbit.test.api.ReferencePropertyTest;
import org.apache.jackrabbit.test.api.ReferenceableRootNodesTest;
import org.apache.jackrabbit.test.api.ReferencesTest;
import org.apache.jackrabbit.test.api.RepositoryDescriptorTest;
import org.apache.jackrabbit.test.api.RepositoryFactoryTest;
import org.apache.jackrabbit.test.api.RepositoryLoginTest;
import org.apache.jackrabbit.test.api.RootNodeTest;
import org.apache.jackrabbit.test.api.SerializationTest;
import org.apache.jackrabbit.test.api.SessionReadMethodsTest;
import org.apache.jackrabbit.test.api.SessionRemoveItemTest;
import org.apache.jackrabbit.test.api.SessionTest;
import org.apache.jackrabbit.test.api.SessionUUIDTest;
import org.apache.jackrabbit.test.api.SetPropertyAssumeTypeTest;
import org.apache.jackrabbit.test.api.SetPropertyBooleanTest;
import org.apache.jackrabbit.test.api.SetPropertyCalendarTest;
import org.apache.jackrabbit.test.api.SetPropertyConstraintViolationExceptionTest;
import org.apache.jackrabbit.test.api.SetPropertyDecimalTest;
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
import org.apache.jackrabbit.test.api.SetValueDecimalTest;
import org.apache.jackrabbit.test.api.SetValueDoubleTest;
import org.apache.jackrabbit.test.api.SetValueLongTest;
import org.apache.jackrabbit.test.api.SetValueReferenceTest;
import org.apache.jackrabbit.test.api.SetValueStringTest;
import org.apache.jackrabbit.test.api.SetValueValueFormatExceptionTest;
import org.apache.jackrabbit.test.api.SetValueVersionExceptionTest;
import org.apache.jackrabbit.test.api.ShareableNodeTest;
import org.apache.jackrabbit.test.api.StringPropertyTest;
import org.apache.jackrabbit.test.api.UndefinedPropertyTest;
import org.apache.jackrabbit.test.api.ValueFactoryTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneTest;
import org.apache.jackrabbit.test.api.WorkspaceCloneVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyBetweenWorkspacesVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceCopySameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyTest;
import org.apache.jackrabbit.test.api.WorkspaceCopyVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest;
import org.apache.jackrabbit.test.api.WorkspaceTest;
import org.apache.jackrabbit.test.api.nodetype.CanAddChildNodeCallWithoutNodeTypeTest;
import org.apache.jackrabbit.test.api.nodetype.CanRemoveItemTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyBinaryTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyBooleanTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyDateTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyDoubleTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyLongTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyMultipleTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyNameTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyPathTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyStringTest;
import org.apache.jackrabbit.test.api.nodetype.CanSetPropertyTest;
import org.apache.jackrabbit.test.api.nodetype.NodeDefTest;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeCreationTest;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeManagerTest;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeTest;
import org.apache.jackrabbit.test.api.nodetype.PredefinedNodeTypeTest;
import org.apache.jackrabbit.test.api.nodetype.PropertyDefTest;
import org.apache.jackrabbit.test.api.observation.AddEventListenerTest;
import org.apache.jackrabbit.test.api.observation.EventIteratorTest;
import org.apache.jackrabbit.test.api.observation.EventTest;
import org.apache.jackrabbit.test.api.observation.GetDateTest;
import org.apache.jackrabbit.test.api.observation.GetIdentifierTest;
import org.apache.jackrabbit.test.api.observation.GetInfoTest;
import org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest;
import org.apache.jackrabbit.test.api.observation.GetUserDataTest;
import org.apache.jackrabbit.test.api.observation.LockingTest;
import org.apache.jackrabbit.test.api.observation.NodeAddedTest;
import org.apache.jackrabbit.test.api.observation.NodeMovedTest;
import org.apache.jackrabbit.test.api.observation.NodeReorderTest;
import org.apache.jackrabbit.test.api.observation.PropertyAddedTest;
import org.apache.jackrabbit.test.api.observation.PropertyChangedTest;
import org.apache.jackrabbit.test.api.observation.PropertyRemovedTest;
import org.apache.jackrabbit.test.api.query.CreateQueryTest;
import org.apache.jackrabbit.test.api.query.DerefQueryLevel1Test;
import org.apache.jackrabbit.test.api.query.ElementTest;
import org.apache.jackrabbit.test.api.query.GetLanguageTest;
import org.apache.jackrabbit.test.api.query.GetPersistentQueryPathLevel1Test;
import org.apache.jackrabbit.test.api.query.GetPersistentQueryPathTest;
import org.apache.jackrabbit.test.api.query.GetPropertyNamesTest;
import org.apache.jackrabbit.test.api.query.GetStatementTest;
import org.apache.jackrabbit.test.api.query.GetSupportedQueryLanguagesTest;
import org.apache.jackrabbit.test.api.query.OrderByDateTest;
import org.apache.jackrabbit.test.api.query.OrderByDecimalTest;
import org.apache.jackrabbit.test.api.query.OrderByDoubleTest;
import org.apache.jackrabbit.test.api.query.OrderByLengthTest;
import org.apache.jackrabbit.test.api.query.OrderByLocalNameTest;
import org.apache.jackrabbit.test.api.query.OrderByLongTest;
import org.apache.jackrabbit.test.api.query.OrderByLowerCaseTest;
import org.apache.jackrabbit.test.api.query.OrderByMultiTypeTest;
import org.apache.jackrabbit.test.api.query.OrderByNameTest;
import org.apache.jackrabbit.test.api.query.OrderByStringTest;
import org.apache.jackrabbit.test.api.query.OrderByURITest;
import org.apache.jackrabbit.test.api.query.OrderByUpperCaseTest;
import org.apache.jackrabbit.test.api.query.PredicatesTest;
import org.apache.jackrabbit.test.api.query.QueryResultNodeIteratorTest;
import org.apache.jackrabbit.test.api.query.SQLJcrPathTest;
import org.apache.jackrabbit.test.api.query.SQLQueryLevel2Test;
import org.apache.jackrabbit.test.api.query.SaveTest;
import org.apache.jackrabbit.test.api.query.SetLimitTest;
import org.apache.jackrabbit.test.api.query.SetOffsetTest;
import org.apache.jackrabbit.test.api.query.SimpleSelectionTest;
import org.apache.jackrabbit.test.api.query.XPathDocOrderTest;
import org.apache.jackrabbit.test.api.query.XPathJcrPathTest;
import org.apache.jackrabbit.test.api.query.XPathOrderByTest;
import org.apache.jackrabbit.test.api.query.XPathPosIndexTest;
import org.apache.jackrabbit.test.api.query.XPathQueryLevel2Test;
import org.apache.jackrabbit.test.api.version.ActivitiesTest;
import org.apache.jackrabbit.test.api.version.CheckinTest;
import org.apache.jackrabbit.test.api.version.CheckoutTest;
import org.apache.jackrabbit.test.api.version.ConfigurationsTest;
import org.apache.jackrabbit.test.api.version.CopyTest;
import org.apache.jackrabbit.test.api.version.GetContainingHistoryTest;
import org.apache.jackrabbit.test.api.version.GetCreatedTest;
import org.apache.jackrabbit.test.api.version.GetPredecessorsTest;
import org.apache.jackrabbit.test.api.version.GetReferencesNodeTest;
import org.apache.jackrabbit.test.api.version.GetVersionableUUIDTest;
import org.apache.jackrabbit.test.api.version.MergeCancelMergeTest;
import org.apache.jackrabbit.test.api.version.MergeCheckedoutSubNodeTest;
import org.apache.jackrabbit.test.api.version.MergeDoneMergeTest;
import org.apache.jackrabbit.test.api.version.MergeNodeIteratorTest;
import org.apache.jackrabbit.test.api.version.MergeNodeTest;
import org.apache.jackrabbit.test.api.version.MergeNonVersionableSubNodeTest;
import org.apache.jackrabbit.test.api.version.MergeShallowTest;
import org.apache.jackrabbit.test.api.version.MergeSubNodeTest;
import org.apache.jackrabbit.test.api.version.OnParentVersionAbortTest;
import org.apache.jackrabbit.test.api.version.OnParentVersionComputeTest;
import org.apache.jackrabbit.test.api.version.OnParentVersionCopyTest;
import org.apache.jackrabbit.test.api.version.OnParentVersionIgnoreTest;
import org.apache.jackrabbit.test.api.version.OnParentVersionInitializeTest;
import org.apache.jackrabbit.test.api.version.RemoveVersionTest;
import org.apache.jackrabbit.test.api.version.SessionMoveVersionExceptionTest;
import org.apache.jackrabbit.test.api.version.VersionGraphTest;
import org.apache.jackrabbit.test.api.version.VersionHistoryTest;
import org.apache.jackrabbit.test.api.version.VersionLabelTest;
import org.apache.jackrabbit.test.api.version.VersionStorageTest;
import org.apache.jackrabbit.test.api.version.VersionTest;
import org.apache.jackrabbit.test.api.version.WorkspaceMoveVersionExceptionTest;
import org.apache.jackrabbit.test.api.version.WorkspaceRestoreTest;

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
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the ModeShape Maven test target.
     * 
     * @return a new instance of {@link JCRTestSuite}.
     */
    public static Test suite() {
        // Uncomment this to execute all tests
        // return new JCRTestSuite();

        // Or uncomment the following lines to execute the different sets/suites of tests ...
        TestSuite suite = new TestSuite("JCR 2.0 API tests");

        suite.addTest(levelOneSuite());
        suite.addTest(levelTwoSuite());
        suite.addTest(new OptionalFeatureTests());

        return suite;
    }

    /**
     * Wrapper for read-only tests
     * 
     * @return a new instance of {@link JCRTestSuite}.
     */
    public static Test readOnlySuite() {
        return levelOneSuite();
    }

    /**
     * Test suite that includes the Level 1 JCR TCK API tests from the Jackrabbit project.
     * 
     * @return a new test suite
     */
    private static TestSuite levelOneSuite() {
        TestSuite suite = new TestSuite("JCR Level 1 API Tests");

        // level 1 tests
        suite.addTestSuite(RootNodeTest.class);
        suite.addTestSuite(NodeReadMethodsTest.class);
        suite.addTestSuite(PropertyTypeTest.class);
        suite.addTestSuite(NodeDiscoveringNodeTypesTest.class);

        suite.addTestSuite(BinaryPropertyTest.class);
        suite.addTestSuite(BooleanPropertyTest.class);
        suite.addTestSuite(DatePropertyTest.class);
        suite.addTestSuite(DoublePropertyTest.class);
        suite.addTestSuite(LongPropertyTest.class);
        suite.addTestSuite(NamePropertyTest.class);
        suite.addTestSuite(PathPropertyTest.class);
        suite.addTestSuite(ReferencePropertyTest.class);
        suite.addTestSuite(StringPropertyTest.class);
        suite.addTestSuite(UndefinedPropertyTest.class);

        suite.addTestSuite(NamespaceRegistryReadMethodsTest.class);
        suite.addTestSuite(NamespaceRemappingTest.class);
        suite.addTestSuite(NodeIteratorTest.class);
        suite.addTestSuite(PropertyReadMethodsTest.class);
        suite.addTestSuite(RepositoryDescriptorTest.class);
        suite.addTestSuite(SessionReadMethodsTest.class);
        suite.addTestSuite(WorkspaceReadMethodsTest.class);
        suite.addTestSuite(ReferenceableRootNodesTest.class);

        suite.addTestSuite(ExportSysViewTest.class);
        suite.addTestSuite(ExportDocViewTest.class);

        // The tests in this suite are level one
        // suite.addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());

        suite.addTestSuite(NodeDefTest.class);
        suite.addTestSuite(NodeTypeManagerTest.class);
        suite.addTestSuite(NodeTypeTest.class);
        suite.addTestSuite(PropertyDefTest.class);

        suite.addTestSuite(PredefinedNodeTypeTest.class);

        suite.addTestSuite(CanSetPropertyBinaryTest.class);
        suite.addTestSuite(CanSetPropertyBooleanTest.class);
        suite.addTestSuite(CanSetPropertyDateTest.class);
        suite.addTestSuite(CanSetPropertyDoubleTest.class);
        suite.addTestSuite(CanSetPropertyLongTest.class);
        suite.addTestSuite(CanSetPropertyMultipleTest.class);
        suite.addTestSuite(CanSetPropertyNameTest.class);
        suite.addTestSuite(CanSetPropertyPathTest.class);
        suite.addTestSuite(CanSetPropertyStringTest.class);
        suite.addTestSuite(CanSetPropertyTest.class);

        // suite.addTestSuite(CanAddChildNodeCallWithNodeTypeTest.class);
        suite.addTestSuite(CanAddChildNodeCallWithoutNodeTypeTest.class);

        suite.addTestSuite(CanRemoveItemTest.class);

        // JCR 2.0

        return suite;
    }

    /**
     * Test suite that includes the Level 2 JCR TCK API tests from the Jackrabbit project.
     * 
     * @return a new test suite
     */
    private static TestSuite levelTwoSuite() {
        TestSuite suite = new TestSuite("JCR Level 2 API Tests");
        // level 2 tests
        suite.addTestSuite(AddNodeTest.class);
        suite.addTestSuite(NamespaceRegistryTest.class);
        suite.addTestSuite(ReferencesTest.class);
        suite.addTestSuite(SessionTest.class);
        suite.addTestSuite(SessionUUIDTest.class);
        suite.addTestSuite(NodeTest.class);
        suite.addTestSuite(NodeUUIDTest.class);
        suite.addTestSuite(NodeOrderableChildNodesTest.class);
        suite.addTestSuite(PropertyTest.class);

        suite.addTestSuite(SetValueBinaryTest.class);
        suite.addTestSuite(SetValueBooleanTest.class);
        suite.addTestSuite(SetValueDateTest.class);
        suite.addTestSuite(SetValueDecimalTest.class);
        suite.addTestSuite(SetValueDoubleTest.class);
        suite.addTestSuite(SetValueLongTest.class);
        suite.addTestSuite(SetValueReferenceTest.class);
        suite.addTestSuite(SetValueStringTest.class);
        suite.addTestSuite(SetValueConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetValueValueFormatExceptionTest.class);
        suite.addTestSuite(SetValueVersionExceptionTest.class);

        suite.addTestSuite(SetPropertyBooleanTest.class);
        suite.addTestSuite(SetPropertyCalendarTest.class);
        suite.addTestSuite(SetPropertyDecimalTest.class);
        suite.addTestSuite(SetPropertyDoubleTest.class);
        suite.addTestSuite(SetPropertyInputStreamTest.class);
        suite.addTestSuite(SetPropertyLongTest.class);
        suite.addTestSuite(SetPropertyNodeTest.class);
        suite.addTestSuite(SetPropertyStringTest.class);
        suite.addTestSuite(SetPropertyValueTest.class);
        suite.addTestSuite(SetPropertyConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetPropertyAssumeTypeTest.class);

        suite.addTestSuite(NodeItemIsModifiedTest.class);
        suite.addTestSuite(NodeItemIsNewTest.class);
        suite.addTestSuite(PropertyItemIsModifiedTest.class);
        suite.addTestSuite(PropertyItemIsNewTest.class);

        suite.addTestSuite(NodeAddMixinTest.class);
        suite.addTestSuite(NodeCanAddMixinTest.class);
        suite.addTestSuite(NodeRemoveMixinTest.class);

        suite.addTestSuite(NodeSetPrimaryTypeTest.class);

        // These two tests aren't marked as read-only, so they causes problems for read-only connectors with the L1 tests
        suite.addTestSuite(NameTest.class);
        suite.addTestSuite(PathTest.class);

        suite.addTestSuite(WorkspaceCloneReferenceableTest.class);
        suite.addTestSuite(WorkspaceCloneSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceCloneTest.class);
        suite.addTestSuite(WorkspaceCloneVersionableTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesReferenceableTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesTest.class);
        suite.addTestSuite(WorkspaceCopyBetweenWorkspacesVersionableTest.class);
        suite.addTestSuite(WorkspaceCopyReferenceableTest.class);
        suite.addTestSuite(WorkspaceCopySameNameSibsTest.class);
        suite.addTestSuite(WorkspaceCopyTest.class);
        suite.addTestSuite(WorkspaceCopyVersionableTest.class);
        suite.addTestSuite(WorkspaceMoveReferenceableTest.class);
        suite.addTestSuite(WorkspaceMoveSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceMoveTest.class);
        suite.addTestSuite(WorkspaceMoveVersionableTest.class);

        suite.addTestSuite(RepositoryLoginTest.class);
        suite.addTestSuite(ImpersonateTest.class);
        suite.addTestSuite(CheckPermissionTest.class);

        suite.addTestSuite(DocumentViewImportTest.class);
        suite.addTestSuite(SerializationTest.class);

        suite.addTestSuite(ValueFactoryTest.class);

        // JCR 2.0
        suite.addTestSuite(NodeTypeCreationTest.class);

        // // new node types
        suite.addTestSuite(GetWeakReferencesTest.class);

        // // new Session features
        suite.addTestSuite(SessionRemoveItemTest.class);
        suite.addTestSuite(HasPermissionTest.class);

        // // new Workspace features
        suite.addTestSuite(WorkspaceTest.class);

        // // shareable nodes
        suite.addTestSuite(ShareableNodeTest.class);

        // repository factory
        suite.addTestSuite(RepositoryFactoryTest.class);

        // lifecycle management
        suite.addTestSuite(LifecycleTest.class);
        return suite;
    }

    /**
     * Test suite that includes the Optional JCR TCK API tests from the Jackrabbit project.
     */
    private static class OptionalFeatureTests extends TestSuite {
        protected OptionalFeatureTests() {
            super("JCR Optional API Tests");
            // We currently don't pass the tests in those suites that are commented out
            // See https://jira.jboss.org/jira/browse/ModeShape-285

            addTest(new ShareableNodesTests());
            addTest(new QueryTests());
            addTest(new ObservationTests()); // remove this and the ObservationTests inner class when all tests pass and
            // uncomment observation.TestAll

            // addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());
            // addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());
            addTest(new FullVersioningTests());
            addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
            addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());
            // addTest(org.apache.jackrabbit.test.api.query.TestAll.suite());
        }
    }

    private static class ShareableNodesTests extends TestSuite {
        protected ShareableNodesTests() {
            super("JCR Shareable Nodes Tests");
            addTestSuite(ShareableNodeTest.class);
        }
    }

    private static class QueryTests extends TestSuite {
        protected QueryTests() {
            super("JCR Query Tests");

            // these are the tests included in observation.TestAll.suite()
            addTestSuite(SaveTest.class);
            // addTestSuite(SQLOrderByTest.class); // see MODE-760
            addTestSuite(SQLQueryLevel2Test.class);
            // addTestSuite(SQLJoinTest.class); // see https://issues.apache.org/jira/browse/JCR-2663
            addTestSuite(SQLJcrPathTest.class);
            // addTestSuite(SQLPathTest.class); // see MODE-760
            addTestSuite(XPathPosIndexTest.class);
            addTestSuite(XPathDocOrderTest.class);
            addTestSuite(XPathOrderByTest.class);
            addTestSuite(XPathQueryLevel2Test.class);
            addTestSuite(XPathJcrPathTest.class);

            addTestSuite(DerefQueryLevel1Test.class);
            addTestSuite(ElementTest.class);
            // addTestSuite(TextNodeTest.class); // see MODE-759
            addTestSuite(GetLanguageTest.class);
            addTestSuite(GetPersistentQueryPathLevel1Test.class);
            addTestSuite(GetPersistentQueryPathTest.class);
            addTestSuite(GetStatementTest.class);
            addTestSuite(GetSupportedQueryLanguagesTest.class);
            addTestSuite(CreateQueryTest.class);

            addTestSuite(QueryResultNodeIteratorTest.class);
            addTestSuite(GetPropertyNamesTest.class);
            addTestSuite(PredicatesTest.class);
            addTestSuite(SimpleSelectionTest.class);

            addTestSuite(OrderByDateTest.class);
            addTestSuite(OrderByDoubleTest.class);
            addTestSuite(OrderByLongTest.class);
            addTestSuite(OrderByMultiTypeTest.class);
            addTestSuite(OrderByStringTest.class);
            addTestSuite(OrderByLengthTest.class);
            addTestSuite(OrderByLocalNameTest.class);
            addTestSuite(OrderByNameTest.class);
            addTestSuite(OrderByLowerCaseTest.class);
            addTestSuite(OrderByUpperCaseTest.class);
            addTestSuite(OrderByDecimalTest.class);
            addTestSuite(OrderByURITest.class);
            addTestSuite(SetLimitTest.class);
            addTestSuite(SetOffsetTest.class);
        }
    }

    private static class ObservationTests extends TestSuite {
        protected ObservationTests() {
            super("JCR Observation Tests");

            // these are the tests included in observation.TestAll.suite()
            addTestSuite(EventIteratorTest.class);
            addTestSuite(EventTest.class);
            addTestSuite(GetRegisteredEventListenersTest.class);
            addTestSuite(LockingTest.class);
            addTestSuite(NodeAddedTest.class);
            // addTestSuite(NodeRemovedTest.class); // see https://issues.apache.org/jira/browse/JCR-2661
            addTestSuite(NodeMovedTest.class);
            addTestSuite(NodeReorderTest.class);
            addTestSuite(PropertyAddedTest.class);
            addTestSuite(PropertyChangedTest.class);
            addTestSuite(PropertyRemovedTest.class);
            addTestSuite(AddEventListenerTest.class);
            // addTestSuite(WorkspaceOperationTest.class); // see https://issues.apache.org/jira/browse/JCR-2661

            // JCR 2.0

            // addTestSuite(EventJournalTest.class); // see https://issues.apache.org/jira/browse/JCR-2662
            addTestSuite(GetDateTest.class);
            addTestSuite(GetIdentifierTest.class);
            addTestSuite(GetInfoTest.class);
            addTestSuite(GetUserDataTest.class);
        }
    }

    private static class FullVersioningTests extends TestSuite {
        protected FullVersioningTests() {
            super("JCR Full Versioning Tests");

            addTestSuite(VersionTest.class);
            addTestSuite(VersionHistoryTest.class);
            addTestSuite(VersionStorageTest.class);
            addTestSuite(VersionLabelTest.class);
            addTestSuite(CheckoutTest.class);
            addTestSuite(CheckinTest.class);
            addTestSuite(CopyTest.class);
            addTestSuite(VersionGraphTest.class);
            addTestSuite(RemoveVersionTest.class);
            // addTestSuite(RestoreTest.class);
            addTestSuite(WorkspaceRestoreTest.class);
            addTestSuite(OnParentVersionAbortTest.class);
            addTestSuite(OnParentVersionComputeTest.class);
            addTestSuite(OnParentVersionCopyTest.class);
            addTestSuite(OnParentVersionIgnoreTest.class);
            addTestSuite(OnParentVersionInitializeTest.class);
            addTestSuite(GetReferencesNodeTest.class);
            addTestSuite(GetPredecessorsTest.class);
            addTestSuite(GetCreatedTest.class);
            addTestSuite(GetContainingHistoryTest.class);
            addTestSuite(GetVersionableUUIDTest.class);
            addTestSuite(SessionMoveVersionExceptionTest.class);
            addTestSuite(WorkspaceMoveVersionExceptionTest.class);
            addTestSuite(MergeCancelMergeTest.class);
            addTestSuite(MergeCheckedoutSubNodeTest.class);
            addTestSuite(MergeDoneMergeTest.class);
            addTestSuite(MergeNodeIteratorTest.class);
            addTestSuite(MergeNodeTest.class);
            addTestSuite(MergeShallowTest.class);
            // addTestSuite(MergeActivityTest.class);
            addTestSuite(MergeNonVersionableSubNodeTest.class);
            addTestSuite(MergeSubNodeTest.class);

            // JCR 2.0

            addTestSuite(ActivitiesTest.class);
            addTestSuite(ConfigurationsTest.class);
        }
    }

}
