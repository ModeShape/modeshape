package org.jboss.dna.connector.store.jpa.model.simple;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.util.UUID;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import org.hibernate.ejb.Ejb3Configuration;
import org.jboss.dna.connector.store.jpa.model.common.NamespaceEntity;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.PropertyType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class NodeEntityTest {

    private static final Boolean SHOW_SQL = false;
    private static final Boolean USE_CACHE = false;

    private ExecutionContext context;
    private EntityManagerFactory factory;
    private EntityManager manager;
    private SimpleModel model;

    @Before
    public void beforeEach() throws Exception {
        model = new SimpleModel();

        // Connect to the database ...
        Ejb3Configuration configurator = new Ejb3Configuration();
        model.configure(configurator);
        configurator.setProperty("hibernate.dialect", "org.hibernate.dialect.HSQLDialect");
        configurator.setProperty("hibernate.connection.driver_class", "org.hsqldb.jdbcDriver");
        configurator.setProperty("hibernate.connection.username", "sa");
        configurator.setProperty("hibernate.connection.password", "");
        configurator.setProperty("hibernate.connection.url", "jdbc:hsqldb:mem:.");
        configurator.setProperty("hibernate.show_sql", SHOW_SQL.toString());
        configurator.setProperty("hibernate.format_sql", "true");
        configurator.setProperty("hibernate.use_sql_comments", "true");
        configurator.setProperty("hibernate.hbm2ddl.auto", "create");
        if (USE_CACHE) {
            configurator.setProperty("hibernate.cache.provider_class", "org.hibernate.cache.HashtableCacheProvider");

        }

        factory = configurator.buildEntityManagerFactory();
        manager = factory.createEntityManager();
        context = new ExecutionContext();
    }

    @After
    public void afterEach() throws Exception {
        try {
            if (manager != null) manager.close();
        } finally {
            manager = null;
            if (factory != null) {
                try {
                    factory.close();
                } finally {
                    factory = null;
                }
            }
        }
    }

    @Test
    public void shouldSaveAndReloadNode() {
        String rootUuid = UUID.randomUUID().toString();
        long workspaceId = 1L;

        manager.getTransaction().begin();

        NamespaceEntity namespace = new NamespaceEntity("");
        manager.persist(namespace);

        NodeEntity root = new NodeEntity(0, null, rootUuid, workspaceId, 1, namespace, "root");
        LargeValueEntity largeValue = LargeValueEntity.create("This is a nonsense string that I am typing.".getBytes(),
                                                              PropertyType.STRING,
                                                              false);
        root.getLargeValues().add(largeValue);
        manager.persist(root);
        manager.persist(largeValue);

        final int NUM_CHILDREN = 10;
        for (int i = 0; i < NUM_CHILDREN; i++) {
            NodeEntity child = new NodeEntity(0, root, UUID.randomUUID().toString(), workspaceId, 1, namespace, "child" + i);
            root.addChild(child);

            manager.persist(child);
        }


        manager.getTransaction().commit();
        manager.close();

        manager = factory.createEntityManager();

        Query query = manager.createNamedQuery("NodeEntity.findByNodeUuid");
        query.setParameter("workspaceId", workspaceId);
        query.setParameter("nodeUuidString", rootUuid);

        NodeEntity newRoot = (NodeEntity)query.getSingleResult();
        assertThat(newRoot, is(notNullValue()));
        assertThat(newRoot, is(root));
        assertThat(newRoot.getChildren().size(), is(NUM_CHILDREN));

        for (int i = 0; i < NUM_CHILDREN; i++) {
            assertThat(newRoot.getChildren().get(i).getChildName(), is("child" + i));
            // NodeEntity child = newRoot.getChildren().get(i);
            // System.out.println(child.getChildName() + " " + child.getIndexInParent());
        }

        root.getLargeValues().size();
    }

    @Test
    public void shouldDeleteRecursively() {
        String rootUuid = UUID.randomUUID().toString();
        long workspaceId = 1L;

        manager.getTransaction().begin();

        NamespaceEntity namespace = new NamespaceEntity("");
        manager.persist(namespace);

        NodeEntity root = new NodeEntity(0, null, rootUuid, workspaceId, 1, namespace, "root");
        manager.persist(root);

        final int DEPTH = 10;
        NodeEntity parent = root;

        for (int i = 0; i < DEPTH; i++) {
            NodeEntity child = new NodeEntity(0, parent, UUID.randomUUID().toString(), workspaceId, 1, namespace, "child" + i);
            root.addChild(child);

            manager.persist(child);
            parent = child;
        }

        manager.getTransaction().commit();
        manager.close();

        manager = factory.createEntityManager();
        manager.getTransaction().begin();

        SubgraphQuery subgraph = SubgraphQuery.create(context,
                                                      manager,
                                                      workspaceId,
                                                      UUID.fromString(rootUuid),
                                                      context.getValueFactories().getPathFactory().createRootPath(),
                                                      0);

        assertThat(subgraph.getNodeCount(false), is(10));

        subgraph.deleteSubgraph(true);
        subgraph.close();

        int count = (Integer)manager.createNativeQuery("SELECT count(*) FROM dna_simple_node").getSingleResult();
        assertThat(count, is(0));
        
        manager.getTransaction().commit();

    }
}
