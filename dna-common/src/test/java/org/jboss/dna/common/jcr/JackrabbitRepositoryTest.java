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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jcr;

import javax.jcr.Repository;
import javax.jcr.Session;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Randall Hauch
 */
public abstract class JackrabbitRepositoryTest {

    public static final String TESTDATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTDATA_PATH + "jackrabbitInMemoryTestRepositoryConfig.xml";

    public static final String WORKSPACE_NAME = "default";

    protected static Repository repository;

    @BeforeClass
    public static void beforeAll() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // Set up the transient repository ...
        repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        try {
            JackrabbitRepository jackrabbit = (JackrabbitRepository)repository;
            jackrabbit.shutdown();
        } finally {
            // Clean up the test data ...
            FileUtil.delete(JACKRABBIT_DATA_PATH);
        }
    }

    /** Used to keep at least one session open during each test; when last session is closed, all data is cleaned up */
    private Session keepAliveSession;

    public void startRepository() throws Exception {
        keepAliveSession = repository.login();
    }

    @After
    public void shutdownRepository() throws Exception {
        if (keepAliveSession != null) {
            Logger.getLogger(this.getClass()).info("Shutting down repository");
            keepAliveSession.logout();
        }
    }
}
