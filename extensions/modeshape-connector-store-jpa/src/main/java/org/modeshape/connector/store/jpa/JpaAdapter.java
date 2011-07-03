package org.modeshape.connector.store.jpa;

import java.util.Map;
import javax.persistence.EntityManager;
import javax.persistence.Persistence;

/**
 * Provides a way for individual JPA implementations to properly set their implementation-specific properties to accurately map
 * the {@link JpaSource} properties to the JPA implementation.
 */
public interface JpaAdapter {

    /**
     * Maps the {@link JpaSource} properties to the corresponding properties in the JPA implementation
     * 
     * @param source the JPA source to map; may not be null
     * @return a map of properties that can be passed to {@link Persistence#createEntityManagerFactory(String, Map)}; never null
     */
    Map<String, String> getProperties( JpaSource source );

    /**
     * Determines the dialect in an implementation-specific manager
     * 
     * @param entityManager an open and valid {@link EntityManager}; may not be null
     * @return a string describing the dialect that will be used to {@link JpaSource#setDialect(String) set the dialect property
     *         on the source}; null indicates that the dialect could not be determined
     */
    String determineDialect( EntityManager entityManager );

}
