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
package org.modeshape.test.integration;

import static org.junit.Assert.assertEquals;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import javax.ejb.EJB;
import javax.enterprise.concurrent.ManagedExecutorService;
import javax.inject.Inject;
import javax.jcr.Session;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.JcrRepository;

/**
 * Integration test for various transactional operations involving the server & repository.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@RunWith( Arquillian.class)
public class TransactionsTest {

    @Deployment
    public static WebArchive createWarDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "transactions-test.war")
                                       .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                                       .addClasses(RepositoryProvider.class,
                                                   StartupRepositoryProvider1.class,
                                                   StartupRepositoryProvider2.class,
                                                   RepositoryOperation.class,
                                                   TransactionalOperationExecutor.class);
                                    
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @Inject
    protected TransactionalOperationExecutor<Void> operationExecutor;

    @Resource(mappedName = "java:jboss/ee/concurrency/executor/default")
    private ManagedExecutorService managedExecutorService;

    @Resource( mappedName = "java:/jcr/sample" )
    protected JcrRepository repository;


    @EJB//@FixFor( "MODE-2596" ) at startup this bean will run its post-construct code
    private StartupRepositoryProvider1 startupBean1;

    @EJB//@FixFor( "MODE-2596" ) at startup this bean will run its post-construct code
    private StartupRepositoryProvider2 startupBean2;

    @Test
    @FixFor( "MODE-2352" )
    public void shouldSupportConcurrentWritersUpdatingTheSameNodeWithSeparateUserTransactions() throws Exception {
        // add a test root
        Session session = repository.login();
        session.getRootNode().addNode("testNode");
        session.save();
        session.logout();

        try {
            // fire a number of concurrent threads which use a SLSB with separate transactions on each call and insert nodes under the parent
            int writersCount = 10;
            List<Future<Void>> results = new ArrayList<>(writersCount);
            for (int i = 0; i < writersCount; i++) {
                final int idx = i + 1;
                //submit a new thread which should run in its own EJB transaction
                results.add(managedExecutorService.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        operationExecutor.execute(repository, new RepositoryOperation<Void>() {
                            @Override
                            public Void execute( JcrRepository repository ) throws Exception {
                                Session session = repository.login();
                                session.getNode("/testNode").addNode("node_" + idx);
                                session.save();
                                return null;
                            }
                        });
                        return null;
                    }
                }));
            }

            //wait for the tasks to finish
            for (Future<Void> result : results) {
                result.get(10, TimeUnit.SECONDS);
            }

            //verify that all of the children were added
            session = repository.login();
            assertEquals("Not all children were inserted", writersCount, session.getNode("/testNode").getNodes().getSize());
            session.logout();
        } finally {
            session = repository.login();
            session.getNode("/testNode").remove();
            session.save();
            session.logout();
        }
    }
}
