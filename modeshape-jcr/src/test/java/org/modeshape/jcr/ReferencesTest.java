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

import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.ReferentialIntegrityException;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.apache.jackrabbit.test.AbstractJCRTest;
import org.modeshape.common.FixFor;

public class ReferencesTest extends AbstractJCRTest {

    @FixFor( "MODE-848" )
    public void testRemovingNodeThatIsUsedInWeakReference() throws RepositoryException {
        try {
            Node node1 = testRootNode.addNode("test1");
            Node node2 = testRootNode.addNode("test2");
            node2.addMixin("mix:referenceable");
            Value ref = superuser.getValueFactory().createValue(node2, true);
            node1.setProperty("ref", ref);
            superuser.save();
            node2.remove();
            superuser.save();
        } catch (ReferentialIntegrityException e) {
            fail("Should allow removing a node that has a WEAKREFERENCE pointing to it.");
        }
    }

    @FixFor( "MODE-848" )
    public void testRemovingNodeThatIsUsedInNonReference() throws RepositoryException {
        try {
            Node node1 = testRootNode.addNode("test1");
            Node node2 = testRootNode.addNode("test2");
            node2.addMixin("mix:referenceable");
            // Creates a STRING value
            Value ref = superuser.getValueFactory().createValue(node2.getIdentifier());
            // Set property using the "*" residual prop defn, which has a type of UNDEFINED
            // (meaning no value conversion is performed, thus we don't treat this as a REFERENCE)
            node1.setProperty("ref", ref);
            superuser.save();
            node2.remove();
            superuser.save();
        } catch (ReferentialIntegrityException e) {
            fail("Should allow removing a node that has a non-reference value pointing to it.");
        }
    }

    @FixFor( "MODE-848" )
    public void testRemovingNodeThatIsUsedInReferenceCreatedExplicitly() throws RepositoryException {
        try {
            Node node1 = testRootNode.addNode("test1");
            Node node2 = testRootNode.addNode("test2");
            node2.addMixin("mix:referenceable");
            Value ref = superuser.getValueFactory().createValue(node2, false);
            node1.setProperty("ref", ref);
            superuser.save();
            node2.remove();
            superuser.save();
            fail("Should not allow removing a node that has a REFERENCE pointing to it.");
        } catch (ReferentialIntegrityException e) {
            // expected
        }
    }

    @FixFor( "MODE-848" )
    public void testRemovingNodeThatIsUsedInReferenceCreatedImplicitly() throws RepositoryException {
        try {
            Node node1 = testRootNode.addNode("test1");
            Node node2 = testRootNode.addNode("test2");
            node2.addMixin("mix:referenceable");
            Value ref = superuser.getValueFactory().createValue(node2); // will create REFERENCE, not WEAKREFERENCE
            node1.setProperty("ref", ref);
            superuser.save();
            node2.remove();
            superuser.save();
            fail("Should not allow removing a node that has a REFERENCE pointing to it.");
        } catch (ReferentialIntegrityException e) {
            // expected
        }
    }

    @FixFor( "MODE-848" )
    public void testRemovingNodeThatIsUsedInReferenceCreatedWithExplicitTypeConversion() throws RepositoryException {
        try {
            Node node1 = testRootNode.addNode("test1");
            Node node2 = testRootNode.addNode("test2");
            node2.addMixin("mix:referenceable");
            Value ref = superuser.getValueFactory().createValue(node2.getIdentifier());
            node1.setProperty("ref", ref, PropertyType.REFERENCE);
            superuser.save();
            node2.remove();
            superuser.save();
            fail("Should not allow removing a node that has a REFERENCE pointing to it.");
        } catch (ReferentialIntegrityException e) {
            // expected
        }
    }

    @FixFor( "MODE-848" )
    public void testRemovingNodeThatIsUsedInWeakReferenceCreatedWithExplicitTypeConversion() throws RepositoryException {
        try {
            Node node1 = testRootNode.addNode("test1");
            Node node2 = testRootNode.addNode("test2");
            node2.addMixin("mix:referenceable");
            Value ref = superuser.getValueFactory().createValue(node2.getIdentifier());
            node1.setProperty("ref", ref, PropertyType.WEAKREFERENCE);
            superuser.save();
            node2.remove();
            superuser.save();
        } catch (ReferentialIntegrityException e) {
            fail("Should allow removing a node that has a WEAKREFERENCE pointing to it.");
        }
    }

    @FixFor( "MODE-877" )
    public void testRemovingReferencedNodeIfReferenceIsRemovedBeforeReferencedNodeInSameSessionSaveAction()
        throws RepositoryException {
        Node node1 = testRootNode.addNode("test1");
        Node node2 = testRootNode.addNode("test2");
        node2.addMixin("mix:referenceable");
        Value ref = superuser.getValueFactory().createValue(node2.getIdentifier());
        node1.setProperty("ref", ref, PropertyType.REFERENCE);
        superuser.save();
        // Now remove the reference and then the referenced node in the same 'save()' ...
        try {
            node1.getProperty("ref").remove();
            node2.remove();
            superuser.save();
        } catch (ReferentialIntegrityException e) {
            fail("Should allow removing a node that has a REFERENCE pointing to it if REFERENCE is removed in same session save().");
        }

    }

    @FixFor( "MODE-877" )
    public void testRemovingReferencedNodeIfReferenceIsRemovedAfterReferencedNodeInSameSessionSaveAction()
        throws RepositoryException {
        Node node1 = testRootNode.addNode("test1");
        Node node2 = testRootNode.addNode("test2");
        node2.addMixin("mix:referenceable");
        Value ref = superuser.getValueFactory().createValue(node2.getIdentifier());
        node1.setProperty("ref", ref, PropertyType.REFERENCE);
        superuser.save();
        // Now remove the reference and then the referenced node in the same 'save()' ...
        try {
            node2.remove();
            node1.getProperty("ref").remove();
            superuser.save();
        } catch (ReferentialIntegrityException e) {
            fail("Should allow removing a node that has a REFERENCE pointing to it if REFERENCE is removed in same session save().");
        }

    }
}
