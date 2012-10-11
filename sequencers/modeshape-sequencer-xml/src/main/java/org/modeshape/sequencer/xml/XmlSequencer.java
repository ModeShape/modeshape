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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.xml;

import java.io.IOException;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A sequencer for XML files, which maintains DTD, entity, comments, and other content. Note that by default the sequencer uses
 * the {@link XmlSequencer.AttributeScoping#USE_DEFAULT_NAMESPACE default namespace} for unqualified attribute rather than
 * {@link XmlSequencer.AttributeScoping#INHERIT_ELEMENT_NAMESPACE inheriting the namespace from the element}. (See also
 * {@link InheritingXmlSequencer}.
 */
public class XmlSequencer extends Sequencer {

    public static final class MimeTypeConstants {
        public static final String WSDL = "application/wsdl+xml";
        public static final String APPLICATION_XML = "application/xml";
        public static final String TEXT_XML = "text/xml";
        public static final String HTML_XML = "application/xhtml+xml";
        public static final String XOP_XML = "application/xop+xml";
        public static final String XSLT = "application/xslt+xml";
        public static final String XSFP = "application/xsfp+xml";
        public static final String MXML = "application/xv+xml";
    }

    /**
     * The choices for how attributes that have no namespace prefix should be assigned a namespace.
     * 
     * @author Randall Hauch
     */
    public enum AttributeScoping {
        /**
         * The attribute's namespace is the default namespace
         */
        USE_DEFAULT_NAMESPACE,
        /**
         * The attribute's namespace is the same namespace as the containing element
         */
        INHERIT_ELEMENT_NAMESPACE
    }

    static final String DECL_HANDLER_FEATURE = "http://xml.org/sax/properties/declaration-handler";
    static final String ENTITY_RESOLVER_2_FEATURE = "http://xml.org/sax/features/use-entity-resolver2";
    static final String LEXICAL_HANDLER_FEATURE = "http://xml.org/sax/properties/lexical-handler";
    static final String RESOLVE_DTD_URIS_FEATURE = "http://xml.org/sax/features/resolve-dtd-uris";
    static final String LOAD_EXTERNAL_DTDS_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private AttributeScoping scoping = AttributeScoping.USE_DEFAULT_NAMESPACE;

    /**
     * @param scoping Sets scoping to the specified value.
     */
    protected void setAttributeScoping( AttributeScoping scoping ) {
        this.scoping = scoping;
    }

    @Override
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        super.registerNodeTypes("xml.cnd", nodeTypeManager, true);
        registerDefaultMimeTypes(MimeTypeConstants.APPLICATION_XML,
                                 MimeTypeConstants.TEXT_XML,
                                 MimeTypeConstants.HTML_XML,
                                 MimeTypeConstants.XOP_XML,
                                 MimeTypeConstants.XSLT,
                                 MimeTypeConstants.XSFP,
                                 MimeTypeConstants.MXML);
    }

    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        if (!outputNode.isNew()) {
            outputNode = outputNode.addNode(XmlLexicon.DOCUMENT);
        }

        XmlSequencerHandler sequencingHandler = new XmlSequencerHandler(outputNode, scoping);
        // Create the reader ...
        XMLReader reader = XMLReaderFactory.createXMLReader();
        reader.setContentHandler(sequencingHandler);
        reader.setErrorHandler(sequencingHandler);
        // Ensure handler acting as entity resolver 2
        reader.setProperty(DECL_HANDLER_FEATURE, sequencingHandler);
        // Ensure handler acting as lexical handler
        reader.setProperty(LEXICAL_HANDLER_FEATURE, sequencingHandler);
        // Ensure handler acting as entity resolver 2
        setFeature(reader, ENTITY_RESOLVER_2_FEATURE, true);
        // Prevent loading of external DTDs
        setFeature(reader, LOAD_EXTERNAL_DTDS_FEATURE, false);
        // Prevent the resolving of DTD entities into fully-qualified URIS
        setFeature(reader, RESOLVE_DTD_URIS_FEATURE, false);
        // Parse XML document
        reader.parse(new InputSource(binaryValue.getStream()));
        return true;
    }

    /**
     * Sets the reader's named feature to the supplied value, only if the feature is not already set to that value. This method
     * does nothing if the feature is not known to the reader.
     * 
     * @param reader the reader; may not be null
     * @param featureName the name of the feature; may not be null
     * @param value the value for the feature
     */
    void setFeature( XMLReader reader,
                     String featureName,
                     boolean value ) {
        try {
            if (reader.getFeature(featureName) != value) {
                reader.setFeature(featureName, value);
            }
        } catch (SAXException e) {
            getLogger().warn(e, "Cannot set feature " + featureName);
        }
    }

}
