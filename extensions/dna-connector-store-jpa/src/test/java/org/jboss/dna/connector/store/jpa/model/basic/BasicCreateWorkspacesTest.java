package org.jboss.dna.connector.store.jpa.model.basic;

import org.jboss.dna.connector.store.jpa.JpaConnectorCreateWorkspacesTest;
import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.connector.store.jpa.TestEnvironment;
import org.jboss.dna.graph.connector.RepositorySource;

public class BasicCreateWorkspacesTest extends JpaConnectorCreateWorkspacesTest {

    @Override
    protected RepositorySource setUpSource() {
        predefinedWorkspaces = new String[] {"workspace1", "workspace1a"};

        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);

        // Override the inherited properties, since that's the focus of these tests ...
        source.setCreatingWorkspacesAllowed(true);
        source.setPredefinedWorkspaceNames(predefinedWorkspaces);

        return source;
    }

}
