/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.query.JcrQuery;

/**
 * Unit test for various operations around large unorderable collections.
 */
public class JcrUnorderedCollectionsTest extends MultiUseAbstractTest {
    
    @BeforeClass
    public static final void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();
        // Import the node types and the data ...
        registerNodeTypes("cnd/large-collections.cnd");
    }

    @AfterClass
    public static final void afterAll() throws Exception {
        MultiUseAbstractTest.afterAll();
    }

    @Override
    public void afterEach() throws Exception {
        NodeIterator nodeIterator = session.getRootNode().getNodes();
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            if (!JcrLexicon.SYSTEM.getString().equals(node.getName())) {
                node.remove();
            }
        }
        session.save();
        super.afterEach();
    }

    @Test
    @FixFor( "MODE-2109 ")
    public void shouldSupportBasicNodeOperations() throws Exception {
        Node tinyCollection = session.getRootNode().addNode("tinyCol", "test:tinyCollection");
        int childCount = 11;
        Set<String> allChildren = new HashSet<>(childCount);
        for (int i = 0; i < childCount; i++) {
            String childName = "child_" + i;
            // add a top level child
            Node child = tinyCollection.addNode(childName);
            
            // add a regular sub child
            child.addNode("test");
            allChildren.add("/tinyCol/" + childName);             
        }
        session.save(); 
        //get by path each child
        for (String childPath : allChildren) {
            Node child = assertNode(childPath);
            assertEquals(child, session.getNodeByIdentifier(child.getIdentifier()));
            Node subchild = assertNode(childPath + "/test");
            assertEquals(subchild, session.getNodeByIdentifier(subchild.getIdentifier()));
        }
            
        //iterate the parent
        tinyCollection = session.getNode("/tinyCol");
        NodeIterator nodeIterator = tinyCollection.getNodes();
        assertEquals(childCount, nodeIterator.getSize());
        
        // search by id
        String id = tinyCollection.getIdentifier();
        tinyCollection = session.getNodeByIdentifier(id);
        
        //check insertion order
        Set<String> iteratedChildren = new LinkedHashSet<>(childCount);
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            iteratedChildren.add(node.getPath());
        }
        assertEquals("Incorrect iteration result", allChildren, iteratedChildren);
        
        //search using regexp
        assertEquals("Incorrect regexp iterator result", childCount, tinyCollection.getNodes("child*").getSize());
        assertEquals(2, tinyCollection.getNodes("child_1|child_2").getSize());   
        assertEquals(0, tinyCollection.getNodes("child1|child2").getSize());

        //query the children
        JcrQueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(
                "select node.[jcr:path] from [nt:unstructured] as node where node.[jcr:name] like 'child%'",
                JcrQuery.JCR_SQL2);
        QueryResult rs = query.execute();
        NodeIterator nodes = rs.getNodes();
        assertEquals(childCount, nodes.getSize());
        Set<String> results = new HashSet<>();
        while (nodes.hasNext()) {
            results.add(nodes.nextNode().getPath());
        }
        assertEquals("Incorrect query result", allChildren, results);

        //remove half the nodes
        int removeCount = childCount / 2;
        for (int i = 0; i < removeCount; i++) {
            String childName = "/tinyCol/child_" + i;
            session.getNode(childName).remove();
            allChildren.remove(childName);
        } 
        session.save();
        tinyCollection = session.getNode("/tinyCol");
        //iterate after removal
        nodeIterator = tinyCollection.getNodes();
        assertEquals(childCount - removeCount, nodeIterator.getSize());
        iteratedChildren = new LinkedHashSet<>(childCount);
        while (nodeIterator.hasNext()) {
            Node node = nodeIterator.nextNode();
            iteratedChildren.add(node.getPath());
        }
        assertEquals("Incorrect iteration result", allChildren, iteratedChildren);
        
        //remove the entire parent
        session.getNode("/tinyCol").remove();
        session.save();
    }

    @Test
    @FixFor( "MODE-2109 ")
    public void shouldSupportTransientNodeOperations() throws Exception {
        Set<String> allChildrenPaths = new HashSet<>();
        Node col = session.getRootNode().addNode("smallCol", "test:smallCollection");
        allChildrenPaths.add(col.addNode("child1").getPath());
        session.save();
        // add a transient node and iterate
        col = session.getNode("/smallCol");
        allChildrenPaths.add(col.addNode("child2").getPath());
        NodeIterator nodeIterator = col.getNodes();
        assertEquals(allChildrenPaths.size(), nodeIterator.getSize());
        Set<String> iterableChildren = new HashSet<>();
        while (nodeIterator.hasNext()) {
            iterableChildren.add(nodeIterator.nextNode().getPath());
        }
        assertEquals("Incorrect iteration result", allChildrenPaths, iterableChildren);
        assertEquals(allChildrenPaths.size(), col.getNodes("child*").getSize());
        assertEquals(2, col.getNodes("child1|child2").getSize());
        assertEquals(0, col.getNodes("child_1|child_2").getSize());

        session.save();
        
        // remove a node and iterate before saving
        AbstractJcrNode child1 = session.getNode("/smallCol/child1");
        String child1Path = child1.getPath();

        child1.remove();
        allChildrenPaths.remove(child1Path);
        nodeIterator = session.getNode("/smallCol").getNodes();
        assertEquals(allChildrenPaths.size(), nodeIterator.getSize());
        iterableChildren = new HashSet<>();
        while (nodeIterator.hasNext()) {
            iterableChildren.add(nodeIterator.nextNode().getPath());
        }
        assertEquals("Incorrect iteration result", allChildrenPaths, iterableChildren);
        assertEquals(allChildrenPaths.size(), col.getNodes("child*").getSize());
        
        // discard the changes
        session.refresh(false);
        allChildrenPaths.add(child1Path);
        // reiterate
        nodeIterator = session.getNode("/smallCol").getNodes();
        assertEquals(allChildrenPaths.size(), nodeIterator.getSize());
        iterableChildren = new HashSet<>();
        while (nodeIterator.hasNext()) {
            iterableChildren.add(nodeIterator.nextNode().getPath());
        }
        assertEquals("Incorrect iteration result", allChildrenPaths, iterableChildren);
        assertEquals(allChildrenPaths.size(), col.getNodes("child*").getSize());
    }
    
    @Test
    @FixFor( "MODE-2109 ")
    public void shouldNotSupportSNS() throws Exception {
        Node col = session.getRootNode().addNode("smallCol", "test:smallCollection");
        col.addNode("child");
        
        try {
            col.addNode("child");
            fail("Unorderable collections should not support SNS");
        } catch (javax.jcr.ItemExistsException e) {
            //expected
        }
        session.refresh(false);
        col = session.getRootNode().addNode("smallCol", "test:smallCollection");
        col.addNode("child");
        session.save();
        col = session.getRootNode().getNode("smallCol");
        
        try {
            col.addNode("child");
            fail("Unorderable collections should not support SNS");
        } catch (javax.jcr.ItemExistsException e) {
            //expected
        }
        
        col.getNode("child").remove();
        Node child = col.addNode("child");
        child.setProperty("test", "test");
        session.save();
        
        NodeIterator nodes = session.getNode("/smallCol").getNodes();
        assertEquals(1, nodes.getSize());
        child = nodes.nextNode();
        assertEquals("test", child.getProperty("test").getString());
    }

    @Test
    @FixFor( "MODE-2109 ")
    public void shouldNotSupportReorderings() throws Exception {
        Node col = session.getRootNode().addNode("smallCol", "test:smallCollection");
        col.addNode("child1");    
        col.addNode("child2");
        session.save();

        col = session.getNode("/smallCol");
        try {
            col.orderBefore("child1", null);
            fail("Reorderings should not be supported for large collections");
        } catch (UnsupportedRepositoryOperationException e) {
            // expected
        }
    } 
    
    @Test
    @FixFor( "MODE-2109 ")
    public void shouldNotSupportRenamingsButShouldSupportMove() throws Exception {
        Node col1 = session.getRootNode().addNode("col1", "test:smallCollection");
        session.getRootNode().addNode("col2", "test:smallCollection");
        col1.addNode("child1");    
        col1.addNode("child2");
        session.save();

        try {
            // reject renamings
            session.move("/col1/child1", "/col1/child3");
            fail("Renamings should not be supported for large collections");
        } catch (ConstraintViolationException e) {
            // expected
        }
        
        // but allow a normal move
        session.move("/col1/child1", "/col2/child1");
        session.save();
        NodeIterator nodes = session.getNode("/col1").getNodes();
        assertEquals(1, nodes.getSize());
        assertEquals("/col1/child2", nodes.nextNode().getPath());
        nodes = session.getNode("/col2").getNodes();
        assertEquals(1, nodes.getSize());
        assertEquals("/col2/child1", nodes.nextNode().getPath());
    }
    
    @Test(expected = ConstraintViolationException.class)
    @FixFor( "MODE-2109 ")
    public void shouldNotAllowVersioning() throws Exception {
        Node col1 = session.getRootNode().addNode("col1", "test:smallCollection");
        col1.addMixin("mix:versionable");
        session.save();
    }


    @Test
    @FixFor( "MODE-2109 ")
    public void shouldOnlyAllowCloningInSomeCases() throws Exception {
        session.getWorkspace().createWorkspace("other");
      
        try {
            session.getRootNode().addNode("col1", "test:smallCollection");
            Node regular = session.getRootNode().addNode("regular");
            regular.addNode("regular1");
            session.save();

            // cloning a large collection is not allowed
            JcrWorkspace workspace = session.getWorkspace();
            try {
                workspace.clone(workspace.getName(), "/col1", "/regular", false);
                fail("Should not allow cloning");
            } catch (ConstraintViolationException e) {
                //expected
            }

            //clone a regular node into a large collection
            JcrSession otherSession = repository.login("other");
            Node col2 = otherSession.getRootNode().addNode("col2", "test:smallCollection");
            col2.addNode("child1");
            otherSession.save();

            otherSession.getWorkspace().clone(workspace.getName(), "/regular", "/col2/regular", false);
            NodeIterator nodes = otherSession.getNode("/col2").getNodes();
            assertEquals(2, nodes.getSize());
        } finally {
            session.getWorkspace().deleteWorkspace("other");    
        }
    }
    
    @Test
    @FixFor( "MODE-2109" )
    public void shouldOnlyAllowCopyingInSomeCases() throws Exception {
        session.getWorkspace().createWorkspace("other");

        try {
            session.getRootNode().addNode("col1", "test:smallCollection");
            Node regular = session.getRootNode().addNode("regular");
            regular.addNode("regular1");
            session.save();

            // cloning a large collection is not allowed
            JcrWorkspace workspace = session.getWorkspace();
            try {
                workspace.copy("/col1", "/col3");
                fail("Should not allow copying");
            } catch (ConstraintViolationException e) {
                //expected
            }

            //copy a regular node into a large collection into this ws
            workspace.copy("/regular", "/col1/regular");
            NodeIterator nodes = session.getNode("/col1").getNodes();
            assertEquals(1, nodes.getSize());

            JcrSession otherSession = repository.login("other");
            Node col2 = otherSession.getRootNode().addNode("col2", "test:smallCollection");
            col2.addNode("child1");
            otherSession.save();

            //copy a regular node into a large collection into another ws
            otherSession.getWorkspace().copy(workspace.getName(), "/regular", "/col2/regular");
            nodes = otherSession.getNode("/col2").getNodes();
            assertEquals(2, nodes.getSize());
        } finally {
            session.getWorkspace().deleteWorkspace("other");
        }
    }
    
    @Test
    @FixFor( "MODE-2109" )
    public void shouldExportImport() throws Exception {
        Node col1 = session.getRootNode().addNode("col1", "test:smallCollection");
        int childCount = 10;
        Set<String> childPaths = new HashSet<>();
        for (int i = 0; i < childCount; i++) {
            Node child = col1.addNode("child_" + i);
            childPaths.add(child.getPath());
            for (int j = 0; j < childCount; j++) {
                child.addNode("child_" + j);               
            }
        }
        session.save();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        session.exportSystemView("/", os, true, false);
        session.getNode("/col1").remove();
        session.save();
       
        session.importXML("/", new ByteArrayInputStream(os.toByteArray()), ImportUUIDBehavior.IMPORT_UUID_COLLISION_THROW);
        session.save();
        
        col1 = session.getNode("/col1");
        for (int i = 0; i < childCount; i++) {
            assertNode("/col1/child_" + i);
            assertNode("/col1/child_" + i + "/child_" + i);
        }
        NodeIterator nodeIterator = col1.getNodes();
        assertEquals(childCount, nodeIterator.getSize());
        while (nodeIterator.hasNext()) {
            childPaths.remove(nodeIterator.nextNode().getPath());
        }
        assertTrue("Not all nodes imported correctly", childPaths.isEmpty());
    }
    
    @Test(expected = ConstraintViolationException.class)
    @FixFor( "MODE-2109" )
    public void shouldNotAllowProjections() throws Exception {
        session.getRootNode().addNode("col", "test:smallCollection");
        session.save();
        session.getWorkspace().getFederationManager().createProjection("/col", "dummy", "/", "dummy");
    }

    @Test
    @FixFor( "MODE-2109" )
    public void shouldAllowAddingAndRemovingMixinsOnlyIfNodeIsEmpty() throws Exception {
        Node collection = session.getRootNode().addNode("collection");
        collection.addNode("child");
        session.save();
        String mixin = ModeShapeLexicon.LARGE_UNORDERED_COLLECTION.getString();
        try {
            collection.addMixin(mixin);
            fail("Unordered collection mixin should not be allowed if node has children");
        } catch (ConstraintViolationException e) {
            //expected
        }
        session.getNode("/collection/child").remove();
        session.save();
        
        collection.addMixin(mixin);
        collection.addNode("child1");
        collection.addNode("child2");
        session.save();
        NodeIterator children = collection.getNodes();
        assertEquals(2, children.getSize());
        try {
            collection.removeMixin(mixin);
            fail("Unordered collection mixin should not be removed if node has children");
        } catch (ConstraintViolationException e) {
            //expected
        }

        try {
            collection.addMixin("mix:versionable");
            fail("Unordered collections should not be versionable");
        } catch (ConstraintViolationException e) {
            //expected
        }
        while (children.hasNext()) {
            children.nextNode().remove();
        }
        session.save();
        try {
            collection.addMixin(ModeShapeLexicon.SMALL_UNORDERED_COLLECTION.getString());
            fail("Second unordered collection mixin should not be allowed");
        } catch (ConstraintViolationException e) {
           //expected
        }
        session.save();
    }
}
