package org.modeshape.jcr;

import org.modeshape.common.util.FileUtil;

import javax.jcr.Session;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

/**
 * @author evgeniy.shevchenko
 * @version 1.0 5/23/14
 */

public class NotClusteredRepositoryMoveTest extends ClusteredRepositoryMoveTest{
    /**
     * Start repositories.
     *
     * @throws Exception
     */
    @Override
    public void before() throws Exception {
        FileUtil.delete("../modeshape-jcr/target/single");

        repositories = new JcrRepository[1];
        repositories[0] = TestingUtil
                            .startRepositoryWithConfig("single/repo.json");
        assertNotNull(repositories[0]);
        assertThat("", repositories[0].getState(), is(ModeShapeEngine.State.RUNNING));
        Session session = repositories[RANDOM.nextInt(repositories.length)].login();
        sourceFolderSize = session.getNode(SOURCE_PATH).getNodes().getSize();
        session.logout();
    }
}
