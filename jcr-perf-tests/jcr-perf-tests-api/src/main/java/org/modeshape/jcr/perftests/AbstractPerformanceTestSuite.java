/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * Abstract base class for individual performance benchmarks.
 */
public abstract class AbstractPerformanceTestSuite {

    protected SuiteConfiguration suiteConfiguration;

    private List<Session> sessions;
    private ExecutorService execService;

    private volatile boolean running;

    public AbstractPerformanceTestSuite(SuiteConfiguration suiteConfiguration) {
        this.suiteConfiguration = suiteConfiguration;
    }

    /**
     * Prepares this performance benchmark.
     *
     * @throws Exception if the benchmark can not be prepared
     */
    public final void setUp()
            throws Exception {
        this.sessions = new LinkedList<Session>();
        this.execService = Executors.newCachedThreadPool();
        this.running = true;

        beforeSuite();
    }

    /**
     * Executes a single iteration of this test.
     *
     * @return number of milliseconds spent in this iteration
     * @throws Exception if an error occurs
     */
    public final long run() throws Exception {
        long start = System.nanoTime();
        runTest();
        return System.nanoTime() - start;
    }

    /**
     * Cleans up after this performance benchmark.
     *
     * @throws Exception if the benchmark can not be cleaned up
     */
    public final void tearDown() throws Exception {
        running = false;
        execService.awaitTermination(10, TimeUnit.SECONDS);
        execService.shutdown();

        afterSuite();
        closeSessions();

        this.execService = null;
        this.sessions = null;
        this.suiteConfiguration = null;
    }

    public boolean isCompatibleWithCurrentRepository() {
        return true;
    }

    private void closeSessions() {
        for (Session session : sessions) {
            if (session.isLive()) {
                session.logout();
            }
        }
    }

//    protected void failOnRepositoryVersions( String... versions )
//            throws RepositoryException {
//        String repositoryVersion = repository.getDescriptor(Repository.REP_VERSION_DESC);
//        for (String version : versions) {
//            if (repositoryVersion.startsWith(version)) {
//                throw new RepositoryException(
//                        "Unable to run " + getClass().getName()
//                                + " on repository version " + version);
//            }
//        }
//    }

    protected Session newSession() {
        try {
            Session session = suiteConfiguration.getRepository().login(suiteConfiguration.getCredentials());
            sessions.add(session);
            return session;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Adds a background thread that repeatedly executes the given job
     * until all the iterations of this test have been executed.
     *
     * @param job background job
     */
    protected void addBackgroundJob( final Callable<?> job ) {
        execService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while (running) {
                    job.call();
                }
                return null;
            }
        });
    }

    protected abstract void runTest() throws Exception;

    protected void beforeSuite() throws Exception {
    }

    protected void afterSuite() throws Exception {
    }
}
