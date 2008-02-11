/*
 *
 */
package org.jboss.dna.common.xml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.NamespaceContext;

/**
 * @author Randall Hauch
 */
public class SimpleNamespaceContext implements NamespaceContext {

    private final Map<String, String> prefixToNamespace = new HashMap<String, String>();

    /**
     * {@inheritDoc}
     */
    public String getNamespaceURI( String prefix ) {
        return this.prefixToNamespace.get(prefix);
    }

    /**
     * {@inheritDoc}
     */
    public String getPrefix( String namespaceURI ) {
        for (Map.Entry<String, String> entry : this.prefixToNamespace.entrySet()) {
            if (entry.getValue().equals(namespaceURI)) return entry.getKey();
        }
        return null;
    }

    public SimpleNamespaceContext setNamespace( String prefix, String namespaceUri ) {
        this.prefixToNamespace.put(prefix, namespaceUri);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public Iterator getPrefixes( String namespaceURI ) {
        return this.prefixToNamespace.keySet().iterator();
    }

}
