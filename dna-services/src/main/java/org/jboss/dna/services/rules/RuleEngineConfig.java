/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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
