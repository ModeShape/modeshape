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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
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
import org.apache.jackrabbit.test.api.SetValueInputStreamTest;
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
import org.apache.jackrabbit.test.api.WorkspaceManagementTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveReferenceableTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveSameNameSibsTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveTest;
import org.apache.jackrabbit.test.api.WorkspaceMoveVersionableTest;
import org.apache.jackrabbit.test.api.WorkspaceReadMethodsTest;
import org.apache.jackrabbit.test.api.WorkspaceTest;
import org.apache.jackrabbit.test.api.nodetype.NodeTypeCreationTest;
import org.apache.jackrabbit.test.api.observation.AddEventListenerTest;
import org.apache.jackrabbit.test.api.observation.EventIteratorTest;
import org.apache.jackrabbit.test.api.observation.EventJournalTest;
import org.apache.jackrabbit.test.api.observation.EventTest;
import org.apache.jackrabbit.test.api.observation.GetDateTest;
import org.apache.jackrabbit.test.api.observation.GetIdentifierTest;
import org.apache.jackrabbit.test.api.observation.GetInfoTest;
import org.apache.jackrabbit.test.api.observation.GetRegisteredEventListenersTest;
import org.apache.jackrabbit.test.api.observation.GetUserDataTest;
import org.apache.jackrabbit.test.api.observation.LockingTest;
import org.apache.jackrabbit.test.api.observation.NodeAddedTest;
import org.apache.jackrabbit.test.api.observation.NodeMovedTest;
import org.apache.jackrabbit.test.api.observation.NodeRemovedTest;
import org.apache.jackrabbit.test.api.observation.NodeReorderTest;
import org.apache.jackrabbit.test.api.observation.PropertyAddedTest;
import org.apache.jackrabbit.test.api.observation.PropertyChangedTest;
import org.apache.jackrabbit.test.api.observation.PropertyRemovedTest;
import org.apache.jackrabbit.test.api.observation.WorkspaceOperationTest;
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
import org.apache.jackrabbit.test.api.query.SQLJoinTest;
import org.apache.jackrabbit.test.api.query.SQLOrderByTest;
import org.apache.jackrabbit.test.api.query.SQLPathTest;
import org.apache.jackrabbit.test.api.query.SQLQueryLevel2Test;
import org.apache.jackrabbit.test.api.query.SaveTest;
import org.apache.jackrabbit.test.api.query.SetLimitTest;
import org.apache.jackrabbit.test.api.query.SetOffsetTest;
import org.apache.jackrabbit.test.api.query.SimpleSelectionTest;
import org.apache.jackrabbit.test.api.query.TextNodeTest;
import org.apache.jackrabbit.test.api.query.XPathDocOrderTest;
import org.apache.jackrabbit.test.api.query.XPathJcrPathTest;
import org.apache.jackrabbit.test.api.query.XPathOrderByTest;
import org.apache.jackrabbit.test.api.query.XPathPosIndexTest;
import org.apache.jackrabbit.test.api.query.XPathQueryLevel2Test;
import org.apache.jackrabbit.test.api.query.qom.AndConstraintTest;
import org.apache.jackrabbit.test.api.query.qom.BindVariableValueTest;
import org.apache.jackrabbit.test.api.query.qom.ChildNodeJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.ChildNodeTest;
import org.apache.jackrabbit.test.api.query.qom.ColumnTest;
import org.apache.jackrabbit.test.api.query.qom.DescendantNodeJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.DescendantNodeTest;
import org.apache.jackrabbit.test.api.query.qom.EquiJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.FullTextSearchScoreTest;
import org.apache.jackrabbit.test.api.query.qom.GetQueryTest;
import org.apache.jackrabbit.test.api.query.qom.LengthTest;
import org.apache.jackrabbit.test.api.query.qom.NodeLocalNameTest;
import org.apache.jackrabbit.test.api.query.qom.NodeNameTest;
import org.apache.jackrabbit.test.api.query.qom.NotConstraintTest;
import org.apache.jackrabbit.test.api.query.qom.OrConstraintTest;
import org.apache.jackrabbit.test.api.query.qom.OrderingTest;
import org.apache.jackrabbit.test.api.query.qom.PropertyExistenceTest;
import org.apache.jackrabbit.test.api.query.qom.PropertyValueTest;
import org.apache.jackrabbit.test.api.query.qom.QueryObjectModelFactoryTest;
import org.apache.jackrabbit.test.api.query.qom.RowTest;
import org.apache.jackrabbit.test.api.query.qom.SameNodeJoinConditionTest;
import org.apache.jackrabbit.test.api.query.qom.SameNodeTest;
import org.apache.jackrabbit.test.api.query.qom.SelectorTest;
import org.apache.jackrabbit.test.api.query.qom.UpperLowerCaseTest;
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
import org.apache.jackrabbit.test.api.version.MergeActivityTest;
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
import org.apache.jackrabbit.test.api.version.RestoreTest;
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
     * Wrapper so that the Jackrabbit TCK test suite gets picked up by the ModeShape Maven test target.
     * 
     * @return a new instance of {@link org.apache.jackrabbit.test.JCRTestSuite}.
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
     * @return a new instance of {@link org.apache.jackrabbit.test.JCRTestSuite}.
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
        suite.addTestSuite(RepositoryLoginTest.class);
        suite.addTestSuite(SessionReadMethodsTest.class);
        suite.addTestSuite(WorkspaceReadMethodsTest.class);
        suite.addTestSuite(ReferenceableRootNodesTest.class);

        suite.addTestSuite(ExportSysViewTest.class);
        suite.addTestSuite(ExportDocViewTest.class);

        // The tests in this suite are level one, with the exception of NodeTypeCreationTest
        suite.addTest(org.apache.jackrabbit.test.api.nodetype.TestAll.suite());

        return suite;
    }

    /**
     * Test suite that includes the Level 2 JCR TCK API tests from the Jackrabbit project.
     * 
     * @return a new test suite
     */
    private static TestSuite levelTwoSuite() {
        TestSuite suite = new TestSuite("JCR Level 2 API Tests");

        suite.addTestSuite(AddNodeTest.class);
        suite.addTestSuite(NamespaceRegistryTest.class);
        suite.addTestSuite(SessionTest.class);
        suite.addTestSuite(NodeOrderableChildNodesTest.class);

        suite.addTestSuite(PropertyTest.class);

        suite.addTestSuite(SetValueBinaryTest.class);
        suite.addTestSuite(SetValueBooleanTest.class);
        suite.addTestSuite(SetValueDateTest.class);
        suite.addTestSuite(SetValueDecimalTest.class);
        suite.addTestSuite(SetValueDoubleTest.class);
        suite.addTestSuite(SetValueLongTest.class);
        suite.addTestSuite(SetValueReferenceTest.class);
        suite.addTestSuite(SetValueInputStreamTest.class);

        suite.addTestSuite(SetValueStringTest.class);
        suite.addTestSuite(SetValueConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetValueValueFormatExceptionTest.class);

        suite.addTestSuite(SetPropertyBooleanTest.class);
        suite.addTestSuite(SetPropertyCalendarTest.class);
        suite.addTestSuite(SetPropertyDecimalTest.class);
        suite.addTestSuite(SetPropertyDoubleTest.class);
        suite.addTestSuite(SetPropertyInputStreamTest.class);
        suite.addTestSuite(SetPropertyLongTest.class);
        suite.addTestSuite(SetPropertyStringTest.class);
        suite.addTestSuite(SetPropertyValueTest.class);
        suite.addTestSuite(SetPropertyConstraintViolationExceptionTest.class);
        suite.addTestSuite(SetPropertyAssumeTypeTest.class);

        suite.addTestSuite(SetValueVersionExceptionTest.class);
        suite.addTestSuite(SetPropertyNodeTest.class);

        suite.addTestSuite(NodeItemIsModifiedTest.class);
        suite.addTestSuite(NodeItemIsNewTest.class);

        suite.addTestSuite(PropertyItemIsModifiedTest.class);
        suite.addTestSuite(PropertyItemIsNewTest.class);

        suite.addTestSuite(NodeAddMixinTest.class);
        suite.addTestSuite(NodeCanAddMixinTest.class);
        suite.addTestSuite(NodeRemoveMixinTest.class);

        suite.addTestSuite(NodeSetPrimaryTypeTest.class);

        suite.addTestSuite(NameTest.class);
        suite.addTestSuite(PathTest.class);

        suite.addTestSuite(ImpersonateTest.class);
        suite.addTestSuite(CheckPermissionTest.class);

        suite.addTestSuite(DocumentViewImportTest.class);

        suite.addTestSuite(SerializationTest.class);
        suite.addTestSuite(ValueFactoryTest.class);
        suite.addTestSuite(NodeTypeCreationTest.class);

        // new Session features
        suite.addTestSuite(SessionRemoveItemTest.class);
        suite.addTestSuite(HasPermissionTest.class);

        // new Workspace features
        suite.addTestSuite(WorkspaceTest.class);

        // repository factory
        suite.addTestSuite(RepositoryFactoryTest.class);

        // lifecycle management
        suite.addTestSuite(LifecycleTest.class);
        suite.addTestSuite(ReferencesTest.class);
        suite.addTestSuite(SessionUUIDTest.class);
        suite.addTestSuite(NodeTest.class);
        suite.addTestSuite(NodeUUIDTest.class);
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
        suite.addTestSuite(WorkspaceManagementTest.class);
        suite.addTestSuite(WorkspaceMoveSameNameSibsTest.class);
        suite.addTestSuite(WorkspaceMoveVersionableTest.class);
        suite.addTestSuite(WorkspaceMoveReferenceableTest.class);
        suite.addTestSuite(WorkspaceMoveTest.class);

        suite.addTestSuite(GetWeakReferencesTest.class);

        // suite.addTestSuite(ShareableNodeTest.class);
        // TODO author=Randall Hauch date=7/2/12 description=https://issues.apache.org/jira/browse/JCR-3370 (get path)
        // TODO author=Randall Hauch date=7/2/12 description=https://issues.apache.org/jira/browse/JCR-3371 (remove mixin)
        // TODO author=Randall Hauch date=7/9/12 description=https://issues.apache.org/jira/browse/JCR-3380 (move)
        suite.addTestSuite(excludeTests(ShareableNodeTest.class,
                                        "testGetPath",
                                        "testRemoveMixin",
                                        "testMoveShareableNode",
                                        "testTransientMoveShareableNode",
                                        "testModifyDescendantAndSave",
                                        "testModifyDescendantAndRemoveShareAndSave"));

        return suite;
    }

    /**
     * Test suite that includes the Optional JCR TCK API tests from the Jackrabbit project.
     */
    private static class OptionalFeatureTests extends TestSuite {
        protected OptionalFeatureTests() {
            super("JCR Optional API Tests");
            // We currently don't pass the tests in those suites that are commented out

            addTest(new ObservationTests());
            // addTest(org.apache.jackrabbit.test.api.observation.TestAll.suite());

            addTest(new FullVersioningTests());
            // addTest(org.apache.jackrabbit.test.api.version.TestAll.suite());

            addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());
            addTest(org.apache.jackrabbit.test.api.util.TestAll.suite());
            addTest(org.apache.jackrabbit.test.api.retention.TestAll.suite());
            addTest(org.apache.jackrabbit.test.api.security.TestAll.suite());

            addTest(new QueryTests());
            // addTest(org.apache.jackrabbit.test.api.query.TestAll.suite());
            addTest(new QueryObjectModelTests());
            // addTest(org.apache.jackrabbit.test.api.query.qom.TestAll.suite());
        }
    }

    private static class QueryTests extends TestSuite {
        @SuppressWarnings( "synthetic-access" )
        protected QueryTests() {
            super("JCR Query Tests");

            // these are the tests included in observation.TestAll.suite()
            addTestSuite(SaveTest.class);
            addTestSuite(SQLOrderByTest.class);

            addTestSuite(SQLJoinTest.class);
            addTestSuite(SQLJcrPathTest.class);
            // TODO author=Horia Chiorean date=7/16/12 description=https://issues.apache.org/jira/browse/JCR-3376
            addTestSuite(excludeTests(SQLPathTest.class, "testChildAxisRoot"));
            addTestSuite(XPathDocOrderTest.class);
            addTestSuite(XPathOrderByTest.class);
            addTestSuite(XPathJcrPathTest.class);

            addTestSuite(DerefQueryLevel1Test.class);
            addTestSuite(ElementTest.class);

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

            addTestSuite(SQLQueryLevel2Test.class);
            addTestSuite(XPathPosIndexTest.class);
            addTestSuite(XPathQueryLevel2Test.class);
            addTestSuite(TextNodeTest.class);
        }
    }

    private static class QueryObjectModelTests extends TestSuite {
        @SuppressWarnings( "synthetic-access" )
        protected QueryObjectModelTests() {
            super("QOM Tests");

            addTestSuite(AndConstraintTest.class);
            addTestSuite(BindVariableValueTest.class);
            addTestSuite(ChildNodeJoinConditionTest.class);
            addTestSuite(ChildNodeTest.class);
            // TODO author=Randall Hauch date=5/17/12 description=https://issues.apache.org/jira/browse/JCR-3313
            addTestSuite(excludeTests(ColumnTest.class, "testExpandColumnsForNodeType"));
            addTestSuite(DescendantNodeJoinConditionTest.class);
            addTestSuite(DescendantNodeTest.class);
            addTestSuite(EquiJoinConditionTest.class);
            addTestSuite(FullTextSearchScoreTest.class);
            addTestSuite(GetQueryTest.class);
            addTestSuite(LengthTest.class);
            addTestSuite(NodeLocalNameTest.class);
            addTestSuite(NodeNameTest.class);
            addTestSuite(NotConstraintTest.class);
            addTestSuite(OrConstraintTest.class);
            // TODO author=Randall Hauch date=5/17/12 description=https://issues.apache.org/jira/browse/MODE-1095
            addTestSuite(excludeTests(OrderingTest.class, "testMultipleSelectors"));
            addTestSuite(PropertyExistenceTest.class);
            addTestSuite(PropertyValueTest.class);
            addTestSuite(QueryObjectModelFactoryTest.class);
            addTestSuite(RowTest.class);
            addTestSuite(SameNodeJoinConditionTest.class);
            addTestSuite(SameNodeTest.class);
            addTestSuite(SelectorTest.class);
            addTestSuite(UpperLowerCaseTest.class);
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
            addTestSuite(NodeRemovedTest.class);
            addTestSuite(NodeMovedTest.class);
            addTestSuite(NodeReorderTest.class);
            addTestSuite(PropertyAddedTest.class);
            addTestSuite(PropertyChangedTest.class);
            addTestSuite(PropertyRemovedTest.class);
            addTestSuite(AddEventListenerTest.class);
            addTestSuite(WorkspaceOperationTest.class);
            //
            // JCR 2.0
            addTestSuite(EventJournalTest.class);
            addTestSuite(GetDateTest.class);
            addTestSuite(GetIdentifierTest.class);
            addTestSuite(GetInfoTest.class);
            addTestSuite(GetUserDataTest.class);
        }
    }

    private static class FullVersioningTests extends TestSuite {
        @SuppressWarnings( "synthetic-access" )
        protected FullVersioningTests() {
            super("JCR Full Versioning Tests");

            addTestSuite(VersionTest.class);
            addTestSuite(VersionStorageTest.class);
            addTestSuite(VersionLabelTest.class);
            addTestSuite(CheckoutTest.class);
            addTestSuite(CheckinTest.class);
            addTestSuite(VersionGraphTest.class);
            addTestSuite(VersionHistoryTest.class);
            addTestSuite(RemoveVersionTest.class);

            addTestSuite(CopyTest.class);

            // TODO author=Horia Chiorean date=4/19/12 description=https://issues.apache.org/jira/browse/JCR-2666
            addTestSuite(excludeTests(RestoreTest.class, "testRestoreNameJcr2"));

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
            addTestSuite(MergeNonVersionableSubNodeTest.class);
            addTestSuite(MergeSubNodeTest.class);
            addTestSuite(MergeActivityTest.class);

            // JCR 2.0
            addTestSuite(ActivitiesTest.class);
            addTestSuite(ConfigurationsTest.class);
        }
    }

    /**
     * Uses javaassist to create a subclass for a TestCase from the original TCK suite and change it's body so that a simple
     * message is printed out. Since we're not using JUnit 4, we can't use @Ignore
     * 
     * @param testCase
     * @param testNames
     * @return the classes to exclude
     */
    @SuppressWarnings( "unchecked" )
    private static Class<? extends TestCase> excludeTests( Class<? extends TestCase> testCase,
                                                           String... testNames ) {
        List<String> excludedTests = Arrays.asList(testNames);

        try {
            ClassPool pool = ClassPool.getDefault();
            CtClass cc = pool.getCtClass(testCase.getName());
            String subclassName = "org.modeshape.jcr.tck." + testCase.getSimpleName();

            CtClass subclass = pool.makeClass(subclassName, cc);

            for (Method m : testCase.getDeclaredMethods()) {
                CtMethod ctMethod = cc.getDeclaredMethod(m.getName());
                String methodName = ctMethod.getName();
                if (excludedTests.contains(methodName)) {
                    CtMethod subclassMethod = new CtMethod(ctMethod, subclass, null);
                    subclassMethod.setBody("{System.out.println(\"" + methodName + " ignored !\");}");
                    subclass.addMethod(subclassMethod);
                }
            }
            return subclass.toClass();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
