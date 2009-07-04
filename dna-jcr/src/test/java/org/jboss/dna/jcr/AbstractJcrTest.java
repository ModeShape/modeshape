package org.jboss.dna.jcr;

import static org.mockito.Mockito.stub;
import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.MockSecurityContext;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.jcr.nodetype.NodeTypeTemplate;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

public abstract class AbstractJcrTest {

    
    protected String workspaceName;
    protected ExecutionContext context;
    protected InMemoryRepositorySource source;
    protected JcrWorkspace workspace;
    protected JcrSession session;
    protected Graph graph;
    protected RepositoryConnectionFactory connectionFactory;
    protected RepositoryNodeTypeManager repoTypeManager;
    protected Map<String, Object> sessionAttributes;
    protected Map<JcrRepository.Option, String> options;
    protected NamespaceRegistry registry;
    @Mock
    protected JcrRepository repository;

    protected void beforeEach() throws Exception {
        MockitoAnnotations.initMocks(this);

        workspaceName = "workspace1";
        final String repositorySourceName = "repository";

        // Set up the source ...
        source = new InMemoryRepositorySource();
        source.setName(workspaceName);
        source.setDefaultWorkspaceName(workspaceName);

        // Set up the execution context ...
        context = new ExecutionContext();
        // Register the test namespace
        context.getNamespaceRegistry().register(TestLexicon.Namespace.PREFIX, TestLexicon.Namespace.URI);

        // Set up the initial content ...
        graph = Graph.create(source, context);
        initializeContent();
        
        // Stub out the connection factory ...
        connectionFactory = new RepositoryConnectionFactory() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.connector.RepositoryConnectionFactory#createConnection(java.lang.String)
             */
            @SuppressWarnings( "synthetic-access" )
            public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                return repositorySourceName.equals(sourceName) ? source.getConnection() : null;
            }
        };

        // Stub out the repository, since we only need a few methods ...
        repoTypeManager = new RepositoryNodeTypeManager(context);

        try {
            this.repoTypeManager.registerNodeTypes(new CndNodeTypeSource(new String[] {"/org/jboss/dna/jcr/jsr_170_builtins.cnd",
                "/org/jboss/dna/jcr/dna_builtins.cnd"}));
            this.repoTypeManager.registerNodeTypes(new NodeTemplateNodeTypeSource(getTestTypes()));

        } catch (RepositoryException re) {
            re.printStackTrace();
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new IllegalStateException("Could not access node type definition files", ioe);
        }

        stub(repository.getRepositoryTypeManager()).toReturn(repoTypeManager);
        stub(repository.getRepositorySourceName()).toReturn(repositorySourceName);
        stub(repository.getConnectionFactory()).toReturn(connectionFactory);

        initializeOptions();
        stub(repository.getOptions()).toReturn(options);

        // Set up the session attributes ...
        // Set up the session attributes ...
        sessionAttributes = new HashMap<String, Object>();
        sessionAttributes.put("attribute1", "value1");

        // Now create the workspace ...
        SecurityContext mockSecurityContext = new MockSecurityContext(null,
                                                                      Collections.singleton(JcrSession.DNA_WRITE_PERMISSION));
        workspace = new JcrWorkspace(repository, workspaceName, context.with(mockSecurityContext), sessionAttributes);

        // Create the session and log in ...
        session = (JcrSession)workspace.getSession();
        registry = session.getExecutionContext().getNamespaceRegistry();
    }

    protected List<NodeTypeTemplate> getTestTypes() {
        return Collections.emptyList();
    }

    protected void initializeContent() {

    }
    
    protected void initializeOptions() {
        // Stub out the repository options ...
        options = new EnumMap<JcrRepository.Option, String>(JcrRepository.Option.class);
        options.put(JcrRepository.Option.PROJECT_NODE_TYPES, Boolean.FALSE.toString());

    }
}
