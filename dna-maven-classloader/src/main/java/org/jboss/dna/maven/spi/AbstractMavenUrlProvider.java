/*
 *
 */
package org.jboss.dna.maven.spi;

import java.util.Properties;
import org.jboss.dna.common.collection.UnmodifiableProperties;

/**
 * @author Randall Hauch
 */
public abstract class AbstractMavenUrlProvider implements IMavenUrlProvider {

    private Properties properties = new Properties();

    public AbstractMavenUrlProvider() {
    }

    /**
     * {@inheritDoc}
     */
    public void configure( Properties properties ) {
        this.properties = new UnmodifiableProperties(properties);
    }

    /**
     * Get the properties for this provider.
     * @return the properties
     */
    public Properties getProperties() {
        return this.properties;
    }

    /**
     * @param properties Sets properties to the specified value.
     */
    public void setProperties( Properties properties ) {
        this.properties = properties != null ? properties : new Properties();
    }

}
