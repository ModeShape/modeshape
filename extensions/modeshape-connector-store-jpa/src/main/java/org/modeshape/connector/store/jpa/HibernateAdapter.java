package org.modeshape.connector.store.jpa;

import java.util.Map;
import java.util.Properties;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.hibernate.cfg.Environment;
import org.hibernate.ejb.Ejb3Configuration;
import org.hibernate.ejb.HibernateEntityManager;
import org.hibernate.engine.SessionFactoryImplementor;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.Logger;
import org.modeshape.connector.store.jpa.model.common.NamespaceEntity;
import org.modeshape.connector.store.jpa.model.common.WorkspaceEntity;
import org.modeshape.connector.store.jpa.model.simple.LargeValueEntity;
import org.modeshape.connector.store.jpa.model.simple.NodeEntity;
import org.modeshape.connector.store.jpa.model.simple.SubgraphNodeEntity;
import org.modeshape.connector.store.jpa.model.simple.SubgraphQueryEntity;
import org.modeshape.connector.store.jpa.util.StoreOptionEntity;
import org.modeshape.graph.ExecutionContext;

/**
 * Hibernate 3-specifc implementation of JpaAdapter
 */
public class HibernateAdapter implements JpaAdapter {

    private static final Logger LOGGER = Logger.getLogger(HibernateAdapter.class);

    @Override
    public Properties getProperties( JpaSource source ) {
        Properties jpaProperties = new Properties();

        // Set the Hibernate properties used in all situations ...
        if (source.getDialect() != null) {
            // The dialect may be auto-determined ...
            setProperty(jpaProperties, Environment.DIALECT, source.getDialect());
        }
        if (source.getIsolationLevel() != null) {
            setProperty(jpaProperties, Environment.ISOLATION, source.getIsolationLevel());
        }
        if (source.getSchemaName() != null) {
            setProperty(jpaProperties, Environment.DEFAULT_SCHEMA, source.getSchemaName());
        }

        // Configure additional properties, which may be overridden by subclasses ...
        String showSql = String.valueOf(source.getShowSql());
        setProperty(jpaProperties, Environment.SHOW_SQL, showSql); // writes all SQL statements to console
        setProperty(jpaProperties, Environment.FORMAT_SQL, showSql);
        setProperty(jpaProperties, Environment.USE_SQL_COMMENTS, showSql);
        if (!JpaSource.AUTO_GENERATE_SCHEMA_DISABLE.equalsIgnoreCase(source.getAutoGenerateSchema())) {
            setProperty(jpaProperties, Environment.HBM2DDL_AUTO, source.getAutoGenerateSchema());
        }

        if (source.getDataSourceJndiName() != null) {
            setProperty(jpaProperties, Environment.DATASOURCE, source.getDataSourceJndiName());
        } else {
            // Set the context class loader, so that the driver could be found ...
            if (source.getRepositoryContext() != null && source.getDriverClassloaderName() != null) {
                try {
                    ExecutionContext context = source.getRepositoryContext().getExecutionContext();
                    ClassLoader loader = context.getClassLoader(source.getDriverClassloaderName());
                    if (loader != null) {
                        Thread.currentThread().setContextClassLoader(loader);
                    }
                } catch (Throwable t) {
                    I18n msg = JpaConnectorI18n.errorSettingContextClassLoader;
                    Logger.getLogger(getClass()).error(t, msg, source.getName(), source.getDriverClassloaderName());
                }
            }
            // Set the connection properties ...
            setProperty(jpaProperties, Environment.DRIVER, source.getDriverClassName());
            setProperty(jpaProperties, Environment.USER, source.getUsername());
            setProperty(jpaProperties, Environment.PASS, source.getPassword());
            setProperty(jpaProperties, Environment.URL, source.getUrl());
            setProperty(jpaProperties, Environment.MAX_FETCH_DEPTH, JpaSource.DEFAULT_MAXIMUM_FETCH_DEPTH);
            setProperty(jpaProperties, Environment.POOL_SIZE, 0); // don't use the built-in pool
            if (source.getMaximumConnectionsInPool() > 0) {
                // Set the connection pooling properties (to use C3P0) ...
                setProperty(jpaProperties, Environment.CONNECTION_PROVIDER, "org.hibernate.connection.C3P0ConnectionProvider");
                setProperty(jpaProperties, Environment.C3P0_MAX_SIZE, source.getMaximumConnectionsInPool());
                setProperty(jpaProperties, Environment.C3P0_MIN_SIZE, source.getMinimumConnectionsInPool());
                setProperty(jpaProperties, Environment.C3P0_TIMEOUT, source.getMaximumConnectionIdleTimeInSeconds());
                setProperty(jpaProperties, Environment.C3P0_MAX_STATEMENTS, source.getMaximumSizeOfStatementCache());
                setProperty(jpaProperties,
                            Environment.C3P0_IDLE_TEST_PERIOD,
                            source.getIdleTimeInSecondsBeforeTestingConnections());
                setProperty(jpaProperties, Environment.C3P0_ACQUIRE_INCREMENT, source.getNumberOfConnectionsToAcquireAsNeeded());
                setProperty(jpaProperties, "hibernate.c3p0.validate", "false");
            }
        }

        if (source.getCacheProviderClassName() != null) {

            setProperty(jpaProperties, Environment.USE_QUERY_CACHE, "true");
            setProperty(jpaProperties, Environment.CACHE_PROVIDER, source.getCacheProviderClassName());

            String cacheConcurrencyStrategy = source.getCacheConcurrencyStrategy();
            setProperty(jpaProperties,
                        "hibernate.ejb.classcache.org.modeshape.connector.store.jpa.model.common.WorkspaceEntity",
                        cacheConcurrencyStrategy);
            setProperty(jpaProperties,
                        "hibernate.ejb.classcache.org.modeshape.connector.store.jpa.model.common.NamespaceEntity",
                        cacheConcurrencyStrategy);
            setProperty(jpaProperties,
                        "hibernate.ejb.classcache.org.modeshape.connector.store.jpa.model.simple.NodeEntity",
                        cacheConcurrencyStrategy);
            setProperty(jpaProperties,
                        "hibernate.ejb.classcache.org.modeshape.connector.store.jpa.model.simple.LargeValueEntity",
                        cacheConcurrencyStrategy);
            setProperty(jpaProperties,
                        "hibernate.ejb.collectioncache.org.modeshape.connector.store.jpa.model.simple.NodeEntity.children",
                        cacheConcurrencyStrategy);
            setProperty(jpaProperties,
                        "hibernate.ejb.collectioncache.org.modeshape.connector.store.jpa.model.simple.NodeEntity.largeValues",
                        cacheConcurrencyStrategy);
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Properties for Hibernate configuration used for ModeShape JPA Source {0}:", source.getName());
            for (Map.Entry<Object, Object> entry : jpaProperties.entrySet()) {
                String propName = entry.getKey().toString();
                if (propName.startsWith("hibernate")) {
                    LOGGER.debug("  {0} = {1}", propName, entry.getValue());
                }
            }
        }

        return jpaProperties;
    }

    protected void setProperty( Properties configurator,
                                String propertyName,
                                String propertyValue ) {
        assert configurator != null;
        assert propertyName != null;
        assert propertyName.trim().length() != 0;
        if (propertyValue != null) {
            configurator.put(propertyName, propertyValue.trim());
        }
    }

    protected void setProperty( Properties configurator,
                                String propertyName,
                                int propertyValue ) {
        assert configurator != null;
        assert propertyName != null;
        assert propertyName.trim().length() != 0;
        configurator.put(propertyName, Integer.toString(propertyValue));
    }

    @Override
    public String determineDialect( EntityManager entityManager ) {
        // We need the connection in order to determine the dialect ...
        HibernateEntityManager em = (HibernateEntityManager)entityManager;
        SessionFactoryImplementor sessionFactory = (SessionFactoryImplementor)em.getSession().getSessionFactory();
        return sessionFactory.getDialect().toString();
    }

    @Override
    public EntityManagerFactory getEntityManagerFactory( JpaSource source ) {
        return new Ejb3Configuration().addAnnotatedClass(StoreOptionEntity.class)
                                      .addAnnotatedClass(NamespaceEntity.class)
                                      .addAnnotatedClass(WorkspaceEntity.class)
                                      .addAnnotatedClass(LargeValueEntity.class)
                                      .addAnnotatedClass(NodeEntity.class)
                                      .addAnnotatedClass(SubgraphNodeEntity.class)
                                      .addAnnotatedClass(SubgraphQueryEntity.class)
                                      .addProperties(getProperties(source))
                                      .buildEntityManagerFactory();

        // return Persistence.createEntityManagerFactory(persistenceUnitName, getProperties(source));
    }
}
