/*
 *
 */
package org.jboss.dna.services.rules;

import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author John Verhaeg
 */
public final class RuleEngineConfig {

    private String serviceProviderClassName;
    private String serviceProviderUri;
    private Map<String, Object> properties;
    private Set<Reader> rules = new HashSet<Reader>();
    private Map<String, Object> globals;
    private ClassLoader classLoader;

    public RuleEngineConfig() {
        setClassLoader(null);
    }

    public void addGlobal( String name, Object object ) {
        assert name != null;
        assert object != null;
        if (globals == null) {
            globals = new HashMap<String, Object>();
        }
        globals.put(name, object);
    }

    public void addRules( Reader rule ) {
        assert rule != null;
        rules.add(rule);
    }

    public RuleEngine createRuleEngine() throws Exception {

        // Set defaults for unspecified configuration properties
        if (serviceProviderClassName == null) {
            serviceProviderClassName = "org.drools.jsr94.rules.RuleServiceProviderImpl";
        }
        if (serviceProviderUri == null) {
            serviceProviderUri = "http://drools.org/";
        }
        if (classLoader == null) {
            throw new RuntimeException("Classloader must be set.");
        }

        return new RuleEngine(this);
    }

    /**
     * @return classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public RuleEngineConfig setClassLoader( ClassLoader loader ) {
        if (loader == null) loader = Thread.currentThread().getContextClassLoader();
        if (loader == null) loader = this.getClassLoader();
        classLoader = loader;
        return this;
    }

    /**
     * @return globals
     */
    public Map<String, Object> getGlobals() {
        return Collections.unmodifiableMap(globals);
    }

    /**
     * @return properties
     */
    public Map<String, Object> getProperties() {
        return properties;
    }

    /**
     * @return rules
     */
    public Set<Reader> getRules() {
        return rules;
    }

    /**
     * @return serviceProviderClassName
     */
    public String getServiceProviderClassName() {
        return serviceProviderClassName;
    }

    /**
     * @return serviceProviderClassName
     */
    public String getServiceProviderUri() {
        return serviceProviderUri;
    }

    public void setProperties( Map<String, Object> properties ) {
        this.properties = properties;
    }

    public void setProperty( String property, Object value ) {
        assert property != null;
        if (properties == null) {
            properties = new HashMap<String, Object>();
        }
        properties.put(property, value);
    }

    public void setServiceProviderClassName( String serviceProviderClassName ) {
        this.serviceProviderClassName = serviceProviderClassName;
    }

    public void setServiceProviderUri( String serviceProviderUri ) {
        this.serviceProviderUri = serviceProviderUri;
    }
}
