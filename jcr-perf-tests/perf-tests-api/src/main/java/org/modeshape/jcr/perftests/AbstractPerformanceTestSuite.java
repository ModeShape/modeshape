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
