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

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Abstract base class for individual performance benchmarks.
 */
public abstract class AbstractPerformanceTestSuite {

    private Repository repository;
    private Credentials credentials;
    private List<Session> sessions;
    private ExecutorService execService;

    private volatile boolean running;

    /**
     * Prepares this performance benchmark.
     *
     * @param repository the repository to use
     * @param credentials credentials of a user with write access
     * @throws Exception if the benchmark can not be prepared
     */
    public void setUp(Repository repository, Credentials credentials)
            throws Exception {
        this.repository = repository;
        this.credentials = credentials;
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
    public long run() throws Exception {
        beforeTest();
        try {
            long start = System.nanoTime();
            runTest();
            return System.nanoTime() - start;
        } finally {
            afterTest();
        }
    }

    /**
     * Cleans up after this performance benchmark.
     *
     * @throws Exception if the benchmark can not be cleaned up
     */
    public void tearDown() throws Exception {
        running = false;
        execService.awaitTermination(10, TimeUnit.SECONDS);
        execService.shutdown();

        afterSuite();
        closeSessions();

        this.execService = null;
        this.sessions = null;
        this.credentials = null;
        this.repository = null;
    }

    private void closeSessions() {
        for (Session session : sessions) {
            if (session.isLive()) {
                session.logout();
            }
        }
    }

    protected void failOnRepositoryVersions(String... versions)
            throws RepositoryException {
        String repositoryVersion =
                repository.getDescriptor(Repository.REP_VERSION_DESC);
        for (String version : versions) {
            if (repositoryVersion.startsWith(version)) {
                throw new RepositoryException(
                        "Unable to run " + getClass().getName()
                        + " on repository version " + version);
            }
        }
    }

    protected Repository getRepository() {
        return repository;
    }

    protected Credentials getCredentials() {
        return credentials;
    }

    protected Session newSession() {
        try {
            Session session = repository.login(credentials);
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
    protected void addBackgroundJob(final Callable<?> job) {
        execService.submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                while(running) {
                    job.call();
                }
                return null;
            }
        });
    }

    protected abstract void runTest() throws Exception;

    public boolean isCompatibleWith( Repository repository ) {
        return true;
    }

    public void beforeSuite() throws Exception {
    }

    public void afterSuite() throws Exception {
    }

    public void beforeTest() throws Exception {
    }

    public void afterTest() throws Exception {
    }
}
