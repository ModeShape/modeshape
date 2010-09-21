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
package org.modeshape.web.jcr.rest.client.json;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.jcr.nodetype.NodeType;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.IoUtil;
import org.modeshape.web.jcr.rest.client.domain.Repository;
import org.modeshape.web.jcr.rest.client.domain.Server;
import org.modeshape.web.jcr.rest.client.domain.Workspace;

public class NodeTypeNodeTest {

    private NodeTypeNode factory;
    private Repository repository;
    private Workspace workspace;
    private boolean print;

    @Before
    public void beforeEach() throws Exception {
        print = false;
        Server server = new Server("file:/tmp/temp.txt/resources", "user", "pswd");
        repository = new Repository("repo", server);
        workspace = new Workspace("myWorkspace", repository);
        factory = new NodeTypeNode(workspace);
    }

    @Test
    public void shouldParseJSONResponseContainingNodeTypes() throws Exception {
        String response = IoUtil.read(new File("src/test/resources/jcr_nodeTypes.txt"));
        Map<String, NodeType> nodeTypes = factory.getNodeTypes(response);
        // print = true;
        printNodeTypes(nodeTypes);
    }

    protected void printNodeTypes( Map<String, NodeType> nodeTypes ) {
        if (!print) return;
        List<NodeType> types = new LinkedList<NodeType>(nodeTypes.values());
        Collections.sort(types, new Comparator<NodeType>() {
            @Override
            public int compare( NodeType o1,
                                NodeType o2 ) {
                String name1 = o1.getName();
                String name2 = o2.getName();
                if (name1.startsWith("jcr:")) return -1;
                if (name2.startsWith("jcr:")) return 1;
                if (name1.startsWith("mix:")) return -1;
                if (name2.startsWith("mix:")) return 1;
                if (name1.startsWith("nt:")) return -1;
                if (name2.startsWith("nt:")) return 1;
                if (name1.startsWith("mode:")) return -1;
                if (name2.startsWith("mode:")) return 1;
                if (name1.startsWith("modeint:")) return -1;
                if (name2.startsWith("modeint:")) return 1;
                return o1.getName().compareTo(o2.getName());
            }
        });
        printNodeTypes(types);
    }

    protected void printNodeTypes( Iterable<NodeType> nodeTypes ) {
        if (print) {
            for (NodeType nodeType : nodeTypes) {
                System.out.println(nodeType);
            }
        }
    }

}
