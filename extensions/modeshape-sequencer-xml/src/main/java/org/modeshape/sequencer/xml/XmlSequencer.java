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
package org.modeshape.sequencer.xml;

import java.io.InputStream;
import org.modeshape.common.text.TextDecoder;
import org.modeshape.graph.JcrNtLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.xml.sax.InputSource;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.XMLReaderFactory;

/**
 * A sequencer for XML files, which maintains DTD, entity, comments, and other content. Note that by default the sequencer uses
 * the {@link XmlSequencer.AttributeScoping#USE_DEFAULT_NAMESPACE default namespace} for unqualified attribute rather than
 * {@link XmlSequencer.AttributeScoping#INHERIT_ELEMENT_NAMESPACE inheriting the namespace from the element}. (See also
 * {@link InheritingXmlSequencer}.
 */
public class XmlSequencer implements StreamSequencer {

    /**
     * The choices for how attributes that have no namespace prefix should be assigned a namespace.
     * 
     * @author Randall Hauch
     */
    public enum AttributeScoping {
        /** The attribute's namespace is the default namespace */
        USE_DEFAULT_NAMESPACE,
        /** The attribute's namespace is the same namespace as the containing element */
        INHERIT_ELEMENT_NAMESPACE;
    }

    /*package*/static final String DEFAULT_PRIMARY_TYPE = "nt:unstructured";
    /*package*/static final String DECL_HANDLER_FEATURE = "http://xml.org/sax/properties/declaration-handler";
    /*package*/static final String ENTITY_RESOLVER_2_FEATURE = "http://xml.org/sax/features/use-entity-resolver2";
    /*package*/static final String LEXICAL_HANDLER_FEATURE = "http://xml.org/sax/properties/lexical-handler";
    /*package*/static final String RESOLVE_DTD_URIS_FEATURE = "http://xml.org/sax/features/resolve-dtd-uris";
    /*package*/static final String LOAD_EXTERNAL_DTDS_FEATURE = "http://apache.org/xml/features/nonvalidating/load-external-dtd";

    private AttributeScoping scoping = AttributeScoping.USE_DEFAULT_NAMESPACE;

    /**
     * @param scoping Sets scoping to the specified value.
     */
    public void setAttributeScoping( AttributeScoping scoping ) {
        this.scoping = scoping;
    }

    /**
     * @return scoping
     */
    public AttributeScoping getAttributeScoping() {
        return scoping;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(InputStream, SequencerOutput, StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        XMLReader reader;
        try {
            // Set up the XML handler ...
            Name primaryType = JcrNtLexicon.UNSTRUCTURED;
            Name nameAttribute = null;
            TextDecoder decoder = null;
            XmlSequencerHandler handler = new XmlSequencerHandler(output, context, nameAttribute, primaryType, decoder, scoping);
            // Create the reader ...
            reader = XMLReaderFactory.createXMLReader();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            // Ensure handler acting as entity resolver 2
            reader.setProperty(DECL_HANDLER_FEATURE, handler);
            // Ensure handler acting as lexical handler
            reader.setProperty(LEXICAL_HANDLER_FEATURE, handler);
            // Ensure handler acting as entity resolver 2
            setFeature(reader, ENTITY_RESOLVER_2_FEATURE, true);
            // Prevent loading of external DTDs
            setFeature(reader, LOAD_EXTERNAL_DTDS_FEATURE, false);
            // Prevent the resolving of DTD entities into fully-qualified URIS
            setFeature(reader, RESOLVE_DTD_URIS_FEATURE, false);
            // Parse XML document
            reader.parse(new InputSource(stream));
        } catch (Exception error) {
            context.getLogger(getClass()).error(error, XmlSequencerI18n.fatalErrorSequencingXmlDocument, error);
            context.getProblems().addError(error, XmlSequencerI18n.fatalErrorSequencingXmlDocument, error);
        }
    }

    /**
     * Sets the reader's named feature to the supplied value, only if the feature is not already set to that value. This method
     * does nothing if the feature is not known to the reader.
     * 
     * @param reader the reader; may not be null
     * @param featureName the name of the feature; may not be null
     * @param value the value for the feature
     */
    /*package*/static void setFeature( XMLReader reader,
                                        String featureName,
                                        boolean value ) {
        try {
            if (reader.getFeature(featureName) != value) {
                reader.setFeature(featureName, value);
            }
        } catch (SAXNotRecognizedException meansFeatureNotRecognized) {
        } catch (SAXNotSupportedException meansFeatureNotSupported) {
        }
    }

}
