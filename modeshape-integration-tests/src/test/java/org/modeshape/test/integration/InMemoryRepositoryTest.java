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

package org.modeshape.test.integration;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import static junit.framework.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.test.ModeShapeUnitTest;
import java.util.ArrayList;
import java.util.List;

/**
 * Test case with various in-memory repository configurations
 *
 * @author Horia Chiorean
 */
public class InMemoryRepositoryTest extends ModeShapeUnitTest {

    private JcrEngine engine = null;

    @Override
    public void afterEach() throws Exception {
        super.afterEach();
        if (engine != null) {
            engine.shutdown();
        }
    }

    @Test
    @FixFor( "MODE-1422")
    public void shouldBeAbleToRemoveNodeWithSNSAfterRemovingIncomingReference() throws Exception {
        engine = startEngineUsing("memory/configRepositoryReferences.xml");
        Session session = engine.getRepository("cimRepo").login();

        Node root = session.getRootNode();

        Node t1 = root.addNode("testNode", "test:test");
        t1.setProperty("myAttribute", "1");
        Node t2 = root.addNode("testNode", "test:test");
        t2.setProperty("myAttribute", "2");

        //this 3rd node caused the original bug
        Node t3 = root.addNode("testNode", "test:test");
        t3.setProperty("myAttribute", "3");

        t1.setProperty("ref", t2);
        t2.setProperty("ref", t1);

        session.save();

        String id1 = t1.getIdentifier();
        t1 = session.getNodeByIdentifier(id1);

        PropertyIterator pi = t1.getReferences();
        while (pi.hasNext()) {
            Property ref = pi.nextProperty();
            ref.remove();
        }
        t1.remove();
        session.save();
    }

    @Test
    @FixFor( "MODE-1553" )
    public void shouldStoreJcrUUIDPropertyForReferenceableNodes() throws Exception {
        engine = startEngineUsing("memory/configRepositoryReferences.xml");
        Session session = engine.getRepository("cimRepo").login();

        Node node = session.getRootNode().addNode("test", "test:second");
        node.setProperty("myAttribute", "Well");

        Node node2 = session.getRootNode().addNode("test", "test:test");
        node2.setProperty("myAttribute", "Well");
        node2.setProperty("second", node);

        Node node3 = session.getRootNode().addNode("test", "test:test");
        node3.setProperty("myAttribute", "Well");
        node3.setProperty("second", node);

        List<Value> refs = new ArrayList<Value>();
        refs.add(session.getValueFactory().createValue(node2, false));
        refs.add(session.getValueFactory().createValue(node3, false));
        node.setProperty("testEntities", refs.toArray(new Value[refs.size()]));

        session.save();

        String stmt = "SELECT a.a AS * FROM [test:test] AS a INNER JOIN [test:second] AS second ON a.second = second.[jcr:uuid]";
        Query q = session.getWorkspace().getQueryManager().createQuery(stmt, "JCR-SQL2");
        QueryResult qr = q.execute();
        assertEquals(qr.getRows().getSize(), 2);

        Property p = node.getProperty("testEntities");
        Value[] values = p.getValues();
        assertEquals(2, values.length);

        Node node4 = session.getRootNode().addNode("test", "test:test");
        node4.setProperty("myAttribute", "testVal");
        node4.setProperty("second", node);
        Node node5 = session.getRootNode().addNode("test", "test:test");
        node5.setProperty("myAttribute", "testVal");
        node5.setProperty("second", node);

        List<Value> refs2 = new ArrayList<Value>();
        refs2.add(session.getValueFactory().createValue(node4, false));
        refs2.add(session.getValueFactory().createValue(node5, false));
        refs.addAll(refs2);
        p = node.setProperty("testEntities", refs.toArray(new Value[refs.size()]));
        assertEquals(4, p.getValues().length);

        session.save();

        assertEquals(4, node.getProperty("testEntities").getValues().length);
        qr = q.execute();
        assertEquals(4, qr.getRows().getSize());
    }
}
