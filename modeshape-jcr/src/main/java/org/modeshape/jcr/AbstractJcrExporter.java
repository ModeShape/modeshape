/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.ItemVisitor;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.XmlNameEncoder;
import org.modeshape.common.xml.StreamingContentHandler;
import org.modeshape.graph.property.Name;
import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * Superclass of ModeShape JCR exporters, provides basic support for traversing through the nodes recursively (if needed),
 * exception wrapping (since {@link ItemVisitor} does not allow checked exceptions to be thrown from its visit* methods, and the
 * ability to wrap an {@link OutputStream} with a {@link ContentHandler}. <p /> Each exporter is only intended to be used once (by
 * calling <code>exportView</code>) and discarded. This class is <b>NOT</b> thread-safe.
 * 
 * @see JcrSystemViewExporter
 * @see JcrDocumentViewExporter
 */
@NotThreadSafe
abstract class AbstractJcrExporter {

    /**
     * Encoder to properly escape XML names.
     * 
     * @see XmlNameEncoder
     */
    private static final TextEncoder NAME_ENCODER = new XmlNameEncoder();

    /**
     * The list of XML namespaces that are predefined and should not be exported by the content handler.
     */
    private static final List<String> UNEXPORTABLE_NAMESPACES = Arrays.asList(new String[] {"", "xml", "xmlns"});

    /**
     * The session in which this exporter was created.
     */
    protected final JcrSession session;

    /**
     * The list of XML namespace prefixes that should never be exported.
     */
    private final Collection<String> restrictedPrefixes;

    /**
     * Cache from {@link Name}s to their rewritten version based on session uri mappings.
     */
    private final Map<Name, String> prefixedNames;

    /**
     * Creates the exporter
     * 
     * @param session the session in which the exporter is created
     * @param restrictedPrefixes the list of XML namespace prefixes that should not be exported
     */
    AbstractJcrExporter( JcrSession session,
                         Collection<String> restrictedPrefixes ) {
        this.session = session;
        this.restrictedPrefixes = restrictedPrefixes;
        this.prefixedNames = new HashMap<Name, String>();
    }

    /**
     * Returns the &quot;prefixed&quot; or rewritten version of <code>baseName</code> based on the URI mappings in the current
     * session. For example:</p> If the namespace &quot;http://www.example.com/JCR/example/1.0&quot; is mapped to the prefix
     * &quot;foo&quot; in the current session (or as a persistent mapping that has not been re-mapped in the current session),
     * this method will return the string &quot;foo:bar&quot; when passed a {@link Name} with uri
     * &quot;http://www.example.com/JCR/example/1.0&quot; and local name &quot;bar&quot;.</p> This method does manage and utilize
     * a {@link Name} to {@link String} cache at the instance scope.
     * 
     * @param baseName the name to be re-mapped into its prefixed version
     * @return the prefixed version of <code>baseName</code> based on the current session URI mappings (which include all
     *         persistent URI mappings by default).
     * @see #prefixedNames
     * @see javax.jcr.Session#setNamespacePrefix(String, String)
     * @see javax.jcr.Session#getNamespacePrefix(String)
     */
    protected String getPrefixedName( Name baseName ) {
        String prefixedName = prefixedNames.get(baseName);

        if (prefixedName == null) {
            prefixedName = baseName.getString(session.getExecutionContext().getNamespaceRegistry());

            prefixedNames.put(baseName, prefixedName);
        }

        return prefixedName;
    }

    /**
     * Exports <code>node</code> (or the subtree rooted at <code>node</code>) into an XML document by invoking SAX events on
     * <code>contentHandler</code>.
     * 
     * @param exportRootNode the node which should be exported. If <code>noRecursion</code> was set to <code>false</code> in the
     *        constructor, the entire subtree rooted at <code>node</code> will be exported.
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @param noRecurse if<code>true</code>, indicates that only the given node should be exported, otherwise a recursice export
     *        and not any of its child nodes.
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    public void exportView( Node exportRootNode,
                            ContentHandler contentHandler,
                            boolean skipBinary,
                            boolean noRecurse ) throws RepositoryException, SAXException {
        assert exportRootNode != null;
        assert contentHandler != null;
        session.checkLive();

        // Export the namespace mappings used in this session
        NamespaceRegistry registry = session.getWorkspace().getNamespaceRegistry();

        contentHandler.startDocument();
        String[] namespacePrefixes = registry.getPrefixes();
        for (int i = 0; i < namespacePrefixes.length; i++) {
            String prefix = namespacePrefixes[i];

            if (!restrictedPrefixes.contains(prefix)) {
                contentHandler.startPrefixMapping(prefix, registry.getURI(prefix));
            }
        }

        exportNode(exportRootNode, contentHandler, skipBinary, noRecurse);

        for (int i = 0; i < namespacePrefixes.length; i++) {
            if (!restrictedPrefixes.contains(namespacePrefixes[i])) {
                contentHandler.endPrefixMapping(namespacePrefixes[i]);
            }
        }

        contentHandler.endDocument();
    }

    /**
     * Exports <code>node</code> (or the subtree rooted at <code>node</code>) into an XML document that is written to
     * <code>os</code>.
     * 
     * @param node the node which should be exported. If <code>noRecursion</code> was set to <code>false</code> in the
     *        constructor, the entire subtree rooted at <code>node</code> will be exported.
     * @param os the {@link OutputStream} to which the XML document will be written
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @param noRecurse if<code>true</code>, indicates that only the given node should be exported, otherwise a recursive export
     *        and not any of its child nodes.
     * @throws RepositoryException if an exception occurs accessing the content repository, generating the XML document, or
     *         writing it to the output stream <code>os</code>.
     */
    public void exportView( Node node,
                            OutputStream os,
                            boolean skipBinary,
                            boolean noRecurse ) throws RepositoryException {
        try {
            exportView(node, new StreamingContentHandler(os, UNEXPORTABLE_NAMESPACES), skipBinary, noRecurse);
            os.flush();
        } catch (IOException ioe) {
            throw new RepositoryException(ioe);
        } catch (SAXException se) {
            throw new RepositoryException(se);
        }
    }

    /**
     * Exports <code>node</code> (or the subtree rooted at <code>node</code>) into an XML document by invoking SAX events on
     * <code>contentHandler</code>.
     * 
     * @param node the node which should be exported. If <code>noRecursion</code> was set to <code>false</code> in the
     *        constructor, the entire subtree rooted at <code>node</code> will be exported.
     * @param contentHandler the SAX content handler for which SAX events will be invoked as the XML document is created.
     * @param skipBinary if <code>true</code>, indicates that binary properties should not be exported
     * @param noRecurse if<code>true</code>, indicates that only the given node should be exported, otherwise a recursive export
     *        and not any of its child nodes.
     * @throws SAXException if an exception occurs during generation of the XML document
     * @throws RepositoryException if an exception occurs accessing the content repository
     */
    public abstract void exportNode( Node node,
                                     ContentHandler contentHandler,
                                     boolean skipBinary,
                                     boolean noRecurse ) throws RepositoryException, SAXException;

    /**
     * Convenience method to invoke the {@link ContentHandler#startElement(String, String, String, Attributes)} method on the
     * given content handler. The name will be encoded to properly escape invalid XML characters.
     * 
     * @param contentHandler the content handler on which the <code>startElement</code> method should be invoked.
     * @param name the un-encoded, un-prefixed name of the element to start
     * @param atts the attributes that should be created for the given element
     * @throws SAXException if there is an error starting the element
     */
    protected void startElement( ContentHandler contentHandler,
                                 Name name,
                                 Attributes atts ) throws SAXException {
        contentHandler.startElement(name.getNamespaceUri(),
                                    NAME_ENCODER.encode(name.getLocalName()),
                                    NAME_ENCODER.encode(getPrefixedName(name)),
                                    atts);
    }

    /**
     * Convenience method to invoke the {@link ContentHandler#endElement(String, String, String)} method on the given content
     * handler. The name will be encoded to properly escape invalid XML characters.
     * 
     * @param contentHandler the content handler on which the <code>endElement</code> method should be invoked.
     * @param name the un-encoded, un-prefixed name of the element to end
     * @throws SAXException if there is an error ending the element
     */
    protected void endElement( ContentHandler contentHandler,
                               Name name ) throws SAXException {
        contentHandler.endElement(name.getNamespaceUri(),
                                  NAME_ENCODER.encode(name.getLocalName()),
                                  NAME_ENCODER.encode(getPrefixedName(name)));
    }
}
