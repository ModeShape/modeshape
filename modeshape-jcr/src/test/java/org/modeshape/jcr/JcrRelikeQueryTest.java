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

import javax.jcr.PathNotFoundException;
import javax.jcr.query.Query;
import org.junit.BeforeClass;
import org.junit.Test;

public class JcrRelikeQueryTest extends MultiUseAbstractTest {

    @BeforeClass
    public static void beforeAll() throws Exception {
        MultiUseAbstractTest.beforeAll();

        registerNodeTypes("cnd/relike.cnd");
    }

    protected void removeCars() throws Exception {
        for (int i = 1; i != 11; ++i) {
            String absPath = "/car" + i;
            try {
                session.getNode(absPath).remove();
            } catch (PathNotFoundException e) {
                // do nothing
            }
        }
        session.save();
    }

    @Test
    public void testRelikeWithoutPatternNotMatch() throws Exception {
        removeCars();
        AbstractJcrNode n1 = session.getRootNode().addNode("car1", "relike:Car");
        n1.setProperty("relike:maker", "avtovaz");
        n1.setProperty("relike:model", "lada");
        session.save();

        String queryString = "SELECT * from [relike:Car] as c where relike('no nodes with this text', c.[relike:maker])";
        assertNodesNotFound(queryString, Query.JCR_SQL2);
    }

    @Test
    public void testRelikeWithoutPatternMatch() throws Exception {
        removeCars();
        AbstractJcrNode n1 = session.getRootNode().addNode("car2", "relike:Car");
        n1.setProperty("relike:maker", "avtovaz");
        n1.setProperty("relike:model", "lada");

        AbstractJcrNode n2 = session.getRootNode().addNode("car3", "relike:Car");
        n2.setProperty("relike:maker", "ford");
        n2.setProperty("relike:model", "focus");

        session.save();

        String queryString = "SELECT * from [relike:Car] as c where relike('ford', c.[relike:maker])";
        assertNodesAreFound(queryString, Query.JCR_SQL2, "/car3");
    }

    @Test
    public void testRelikeWithPatternTypes() throws Exception {
        removeCars();
        AbstractJcrNode n1 = session.getRootNode().addNode("car4", "relike:Car");
        n1.setProperty("relike:maker", "avtovaz");
        n1.setProperty("relike:model", "lada");

        AbstractJcrNode n2 = session.getRootNode().addNode("car5", "relike:Car");
        n2.setProperty("relike:maker", "_ord australia");
        n2.setProperty("relike:model", "focus");

        AbstractJcrNode n3 = session.getRootNode().addNode("car6", "relike:Car");
        n3.setProperty("relike:maker", "%ford au%");
        n3.setProperty("relike:model", "Mustang");

        AbstractJcrNode n4 = session.getRootNode().addNode("car7", "relike:Car");
        n4.setProperty("relike:maker", "%ford australia");
        n4.setProperty("relike:model", "Sierra");

        AbstractJcrNode n5 = session.getRootNode().addNode("car8", "relike:Car");
        n5.setProperty("relike:maker", "_ord au%");
        n5.setProperty("relike:model", "torino");

        AbstractJcrNode n6 = session.getRootNode().addNode("car9", "relike:Car");
        n6.setProperty("relike:maker", "1_3_5_");
        n6.setProperty("relike:model", "torino");

        session.save();

        String queryString = "SELECT * from [relike:Car] as c where relike('ford australia', c.[relike:maker])";
        assertNodesAreFound(queryString, Query.JCR_SQL2, "/car5", "/car6", "/car7", "/car8");

        String queryString2 = "SELECT * from [relike:Car] as c where relike('avtovaz', c.[relike:maker])";
        assertNodesAreFound(queryString2, Query.JCR_SQL2, "/car4");

        String queryString3 = "SELECT * from [relike:Car] as c where relike('123456', c.[relike:maker])";
        assertNodesAreFound(queryString3, Query.JCR_SQL2, "/car9");

        String queryStringNotFound = "SELECT * from [relike:Car] as c where relike('idkfa', c.[relike:maker])";
        assertNodesNotFound(queryStringNotFound, Query.JCR_SQL2);
    }

    @Test
    public void testRelikeWithMultyContraints() throws Exception {
        removeCars();
        AbstractJcrNode n1 = session.getRootNode().addNode("car10", "relike:Car");
        n1.setProperty("relike:maker", "avtovaz");
        n1.setProperty("relike:model", "lada");

        AbstractJcrNode n2 = session.getRootNode().addNode("car11", "relike:Car");
        n2.setProperty("relike:maker", "123%");
        n2.setProperty("relike:model", "concept");

        session.save();

        String queryString = "SELECT * " + "from [relike:Car] as c " + "where " + "  relike('1234', c.[relike:maker]) or "
                             + "  relike('12345678', c.[relike:maker]) or " + "  relike('test', c.[relike:maker])";

        assertNodesAreFound(queryString, Query.JCR_SQL2, "/car11");

        String queryString2 = "SELECT * " + "from [relike:Car] as c " + "where " + " (relike('1234', c.[relike:maker]) or "
                              + "  relike('12345678', c.[relike:maker]) or " + "  relike('test', c.[relike:maker])) and "
                              + "  c.[relike:model] = 'lada'";

        assertNodesNotFound(queryString2, Query.JCR_SQL2);
    }
}
