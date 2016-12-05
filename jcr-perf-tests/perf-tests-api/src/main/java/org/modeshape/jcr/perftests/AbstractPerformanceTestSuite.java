/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.perftests;

import javax.jcr.Credentials;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Abstract base class for individual performance benchmarks.To create a new performance test suite, you should subclass this.
 *
 * @author Horia Chiorean
 */
public abstract class AbstractPerformanceTestSuite {

    protected SuiteConfiguration suiteConfiguration;

    /** holder for a list of active session created by this suite */
    private List<Session> sessions;

    /** executor which is used to fire up async jobs */
    private ExecutorService execService;

    /** flag used to signal to signal to the different (potential) threads created by the suite that the suite is active */
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
     * @throws Exception if an error occurs
     */
    public final void run() throws Exception {
        beforeTestRun();
        try {
            runTest();
        } finally {
            afterTestRun();
        }
    }

    /**
     * Cleans up after this performance benchmark.
     *
     * @throws Exception if the benchmark can not be cleaned up
     */
    public final void tearDown() throws Exception {
        running = false;
        execService.shutdown();

        afterSuite();
        closeSessions();

        this.execService = null;
        this.sessions = null;
        this.suiteConfiguration = null;
    }

    /**
     * Indicates if the test suite is compatible with the repository from the <code>SuiteConfiguration</code> object.
     * @return true of false depending on the operation(s) performed by the suite.
     */
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

    protected Session newSession(Credentials credentials) {
        try {
            Session session = suiteConfiguration.getRepository().login(credentials);
            sessions.add(session);
            return session;
        } catch (RepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    protected Session newSession() {
        return newSession(suiteConfiguration.getCredentials());
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

    protected void afterTestRun() throws Exception {}

    protected void beforeTestRun() throws Exception {}

    protected void beforeSuite() throws Exception {}

    protected void afterSuite() throws Exception {}
}
