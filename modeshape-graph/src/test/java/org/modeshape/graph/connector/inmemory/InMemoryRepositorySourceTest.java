package org.modeshape.graph.connector.inmemory;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.request.GetWorkspacesRequest;

public class InMemoryRepositorySourceTest {

    private ExecutionContext context;
    private InMemoryRepositorySource source;
    private String[] predefinedWorkspaces = new String[] {"foo", "bar", "baz"};

    @Before
    public void beforeEach() {
        context = new ExecutionContext();

        source = new InMemoryRepositorySource();
        source.setName("In-Memory Repository Source");
        source.setPredefinedWorkspaceNames(predefinedWorkspaces);
        // Have to do this or the comparison later will be off when the default workspace is also created
        source.setDefaultWorkspaceName(predefinedWorkspaces[0]);
    }

    @Test
    public void shouldCreatePredefinedWorkspaces() {
        RepositoryConnection connection = source.getConnection();

        GetWorkspacesRequest request = new GetWorkspacesRequest();
        connection.execute(context, request);

        Set<String> workspaces = request.getAvailableWorkspaceNames();
        Set<String> graphWorkspaces = new HashSet<String>(Arrays.asList(predefinedWorkspaces));
        assertThat(workspaces, is(graphWorkspaces));

    }

}
