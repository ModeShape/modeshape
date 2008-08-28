/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR MyClass PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.jackrabbitcdnsupport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.jcr.Repository;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.nodetype.NodeType;
import org.apache.jackrabbit.api.JackrabbitNodeTypeManager;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.util.FileUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * jackrabbitCNDSupportsTest tests some CND (Compact Node Type Definition) features of jackrabbbit.
 * 
 * @author serge.pagop@innoq.com
 */
public class jackrabbitCNDSupportsTest {

    public static final String TESTATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTATA_PATH + "jackrabbitInMemoryTestRepositoryConfig.xml";
    public static final String USERNAME = "jsmith";
    public static final char[] PASSWORD = "secret".toCharArray();

    // private Logger logger;
    private Repository repository;
    private Session session;

    @Before
    public void beforeEach() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // logger = Logger.getLogger(jackrabbitCNDSupportsTest.class);

        // Set up the transient repository ...
        this.repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);

        SimpleCredentials creds = new SimpleCredentials(USERNAME, PASSWORD);
        session = this.repository.login(creds);
        assertNotNull(session);
    }

    @After
    public void afterEach() {
        try {
            if (session != null) session.logout();
        } finally {
            session = null;
            // No matter what, clean up the test data ...
            FileUtil.delete(JACKRABBIT_DATA_PATH);
        }
    }

    @Test
    public void shouldSupportCNDNodeTypes() throws Exception {
        JackrabbitNodeTypeManager ntm = (JackrabbitNodeTypeManager)session.getWorkspace().getNodeTypeManager();
        assertNotNull(ntm);
        FileInputStream cndFile = new FileInputStream(TESTATA_PATH + "java-source-artifact.cnd");
        assertNotNull(cndFile);
        NodeType[] nodeTypes = ntm.registerNodeTypes(cndFile, JackrabbitNodeTypeManager.TEXT_X_JCR_CND);
        assertTrue(nodeTypes.length > 0);
        for (NodeType nodeType : nodeTypes) {
            System.out.println("node type name: " + nodeType.getName());
        }
    }

    @Test
    public void testMap() {
        Map<String, String> errors = new HashMap<String, String>();
        errors.put("login", "field.isNull");
        if (!errors.containsKey("login")) errors.put("login", "field.regexp");

        Collection<String> keys = errors.keySet();
        assertTrue(keys.size() == 1);
        Collection<String> values = errors.values();
        for (Iterator<String> i = values.iterator(); i.hasNext();) {
            System.out.println(i.next());

        }

    }
}
