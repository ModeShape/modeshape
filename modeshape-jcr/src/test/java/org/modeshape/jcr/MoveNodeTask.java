package org.modeshape.jcr;

import javax.jcr.Node;
import java.util.concurrent.Callable;

/**
 * @author evgeniy.shevchenko
 * @version 1.0 5/20/14
 */

class MoveNodeTask implements Callable<String> {

    private JcrRepository repository;
    private String sourceId;
    private String destinationPath;

    public MoveNodeTask(
            final JcrRepository repository,
            final String sourceId,
            final String destinationPath) {
        this.repository = repository;
        this.sourceId = sourceId;
        this.destinationPath = destinationPath;
    }

    /**
     * Move node from one folder to another.
     *
     * @return document identifier
     * @throws Exception if unable to compute a result
     */
    @Override
    public String call() throws Exception {
        JcrSession session = null;
        try {
            session = repository.login();
            final Node item = session.getNodeByIdentifier(sourceId);
            final Node sourceFolder = item.getParent();
            final Node destFolder = session.getNode(destinationPath);
            System.out.println(
                    String.format(
                            "Move node '%s' from '%s' to '%s'",
                            item.getIdentifier(),
                            destFolder.getIdentifier(),
                            sourceFolder.getIdentifier()));
            session.move(item.getPath(), destinationPath + "/" + item.getName());
            session.save();

            return item.getIdentifier();

        } finally {
            if (session != null) {
                session.logout();
            }
        }

    }
}