package org.modeshape.test.integration;

import java.io.File;
import javax.ejb.EJB;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ArchivePaths;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( Arquillian.class )
public class StatelessBeanManagedTransactionIntegrationTest {

    @Deployment
    public static WebArchive createDeployment() {
        WebArchive archive = ShrinkWrap.create(WebArchive.class, "stateless-bmt-test.war")
                                       .addAsWebInfResource(EmptyAsset.INSTANCE, ArchivePaths.create("beans.xml"))
                                       .addClasses(StatelessBeanManagedTransactionBean.class);
        // Add our custom Manifest, which has the additional Dependencies entry ...
        archive.setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
        return archive;
    }

    @EJB
    private StatelessBeanManagedTransactionBean bean;

    @Test
    public void testCreatingNodesUsingMultipleTransactions() throws Exception {
        final String path = "/stateless-bmt-test";
        try {
            bean.createNodes(path, 10, true);
            bean.verifyNodesInTransaction(path, 10, true);
        } finally {
            bean.cleanup(path, false);
        }
    }

}
