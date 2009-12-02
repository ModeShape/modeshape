package org.jboss.dna.connector.store.jpa.model.simple;

import org.jboss.dna.connector.store.jpa.JpaConnectorCreateWorkspacesTest;
import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.connector.store.jpa.TestEnvironment;
import org.jboss.dna.graph.connector.RepositorySource;

public class SimpleCreateWorkspacesTest extends JpaConnectorCreateWorkspacesTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        predefinedWorkspaces = new String[] {"workspace1", "workspace1a"};

        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);
        source.setModel(JpaSource.Models.SIMPLE.getName());

        // Override the inherited properties, since that's the focus of these tests ...
        source.setCreatingWorkspacesAllowed(true);
        source.setPredefinedWorkspaceNames(predefinedWorkspaces);
        source.setDefaultWorkspaceName(predefinedWorkspaces[0]);

        return source;
    }

}
