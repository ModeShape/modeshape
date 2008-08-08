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
package org.jboss.dna.jcr;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.jcr.NamespaceException;
import javax.jcr.NamespaceRegistry;
import javax.jcr.UnsupportedRepositoryOperationException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;

/**
 * @author John Verhaeg
 * @author Randall Hauch
 */
@Immutable
class JcrNamespaceRegistry implements NamespaceRegistry {

    private Map<String, String> prefix2UriMap = new HashMap<String, String>();

    JcrNamespaceRegistry() {
        prefix2UriMap.put("", "");
        prefix2UriMap.put("dna", "http://www.jboss.org/dna/1.0");
        prefix2UriMap.put("jcr", "http://www.jcp.org/jcr/1.0");
        prefix2UriMap.put("mix", "http://www.jcp.org/jcr/mix/1.0");
        prefix2UriMap.put("nt", "http://www.jcp.org/jcr/nt/1.0");
        prefix2UriMap.put("xml", "http://www.w3.org/XML/1998/namespace");
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix( String uri ) throws NamespaceException {
        ArgCheck.isNotNull(uri, "uri");
        for (Entry<String, String> entry : prefix2UriMap.entrySet()) {
            if (uri.equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        throw new NamespaceException();
    }

    /**
     * {@inheritDoc}
     */
    public String[] getPrefixes() {
        String[] prefixes = prefix2UriMap.keySet().toArray(new String[prefix2UriMap.size()]);
        Arrays.sort(prefixes);
        return prefixes;
    }

    /**
     * {@inheritDoc}
     */
    public String getURI( String prefix ) throws NamespaceException {
        ArgCheck.isNotNull(prefix, "prefix");
        String uri = prefix2UriMap.get(prefix);
        if (uri == null) throw new NamespaceException();
        return uri;
    }

    /**
     * {@inheritDoc}
     */
    public String[] getURIs() {
        String[] uris = prefix2UriMap.values().toArray(new String[prefix2UriMap.size()]);
        Arrays.sort(uris);
        return uris;
    }

    /**
     * {@inheritDoc}
     */
    public void registerNamespace( String prefix,
                                   String uri ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void unregisterNamespace( String prefix ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }
}
