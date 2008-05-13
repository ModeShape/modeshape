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
package org.jboss.dna.spi.sequencers;

import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class MockSequencerOutput implements SequencerOutput {

    private final Map<String, Object[]> properties;
    private final Map<String, String[]> references;

    /**
     * 
     */
    public MockSequencerOutput() {
        this.properties = new HashMap<String, Object[]>();
        this.references = new HashMap<String, String[]>();
    }

    /**
     * {@inheritDoc}
     */
    public void setProperty( String nodePath, String property, Object... values ) {
        String key = getKey(nodePath, property);
        if (values == null || values.length == 0) {
            this.properties.remove(key);
        } else {
            this.properties.put(key, values);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void setReference( String nodePath, String property, String... paths ) {
        String key = getKey(nodePath, property);
        if (paths == null || paths.length == 0) {
            this.references.remove(key);
        } else {
            this.references.put(key, paths);
        }
    }

    public Object[] getPropertyValues( String nodePath, String property ) {
        String key = getKey(nodePath, property);
        return this.properties.get(key);
    }

    public String[] getReferenceValues( String nodePath, String property ) {
        String key = getKey(nodePath, property);
        return this.references.get(key);
    }

    public boolean hasProperty( String nodePath, String property ) {
        String key = nodePath + "@" + property;
        return this.properties.containsKey(key);
    }

    public boolean hasReference( String nodePath, String property ) {
        String key = nodePath + "@" + property;
        return this.references.containsKey(key);
    }

    public boolean hasProperties() {
        return this.properties.size() > 0;
    }

    public boolean hasReferences() {
        return this.references.size() > 0;
    }

    protected String getKey( String nodePath, String property ) {
        return nodePath + "@" + property;
    }

}
