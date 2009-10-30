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
package org.jboss.dna.test.integration;

import junit.framework.Test;
import junit.framework.TestSuite;
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
import org.apache.jackrabbit.test.api.SerializationTest;
import org.apache.jackrabbit.test.api.SessionTest;
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

public class JpaRepositoryTckTest {

    public static Test suite() {
        TestSuite suite = AbstractRepositoryTckTest.readOnlyRepositorySuite("jpa");
        suite.addTest(new LevelTwoFeatureTests());
        // suite.addTest(org.apache.jackrabbit.test.api.lock.TestAll.suite());

        return suite;

    }

    private static class LevelTwoFeatureTests extends TestSuite {
        protected LevelTwoFeatureTests() {
            super("JCR Level 2 API Tests");
            // We currently don't pass the tests in those suites that are commented out
            // See https://jira.jboss.org/jira/browse/DNA-285

            // level 2 tests
            addTestSuite(AddNodeTest.class);
            addTestSuite(NamespaceRegistryTest.class);
            // addTestSuite(ReferencesTest.class);
            addTestSuite(SessionTest.class);
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

            addTestSuite(WorkspaceCloneReferenceableTest.class);
            addTestSuite(WorkspaceCloneSameNameSibsTest.class);
            addTestSuite(WorkspaceCloneTest.class);
            addTestSuite(WorkspaceCloneVersionableTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesReferenceableTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesSameNameSibsTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesTest.class);
            addTestSuite(WorkspaceCopyBetweenWorkspacesVersionableTest.class);
            addTestSuite(WorkspaceCopyReferenceableTest.class);
            addTestSuite(WorkspaceCopySameNameSibsTest.class);
            addTestSuite(WorkspaceCopyTest.class);
            addTestSuite(WorkspaceCopyVersionableTest.class);
            addTestSuite(WorkspaceMoveReferenceableTest.class);
            addTestSuite(WorkspaceMoveSameNameSibsTest.class);
            addTestSuite(WorkspaceMoveTest.class);
            addTestSuite(WorkspaceMoveVersionableTest.class);

            addTestSuite(RepositoryLoginTest.class);
            addTestSuite(ImpersonateTest.class);
            addTestSuite(CheckPermissionTest.class);

            // addTestSuite(DocumentViewImportTest.class);
            addTestSuite(SerializationTest.class);

            addTestSuite(ValueFactoryTest.class);
        }
    }

}
