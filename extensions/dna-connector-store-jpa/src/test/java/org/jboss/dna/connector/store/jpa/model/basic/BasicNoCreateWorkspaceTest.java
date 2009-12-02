package org.jboss.dna.connector.store.jpa.model.basic;

import org.jboss.dna.connector.store.jpa.JpaConnectorNoCreateWorkspaceTest;
import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.connector.store.jpa.TestEnvironment;
import org.jboss.dna.graph.connector.RepositorySource;

public class BasicNoCreateWorkspaceTest extends JpaConnectorNoCreateWorkspaceTest {

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
