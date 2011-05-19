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
package org.modeshape.sequencer.xsd;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import org.eclipse.emf.common.util.AbstractEnumerator;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xsd.XSDAnnotation;
import org.eclipse.xsd.XSDAttributeDeclaration;
import org.eclipse.xsd.XSDAttributeGroupContent;
import org.eclipse.xsd.XSDAttributeGroupDefinition;
import org.eclipse.xsd.XSDAttributeUse;
import org.eclipse.xsd.XSDComplexTypeContent;
import org.eclipse.xsd.XSDComplexTypeDefinition;
import org.eclipse.xsd.XSDCompositor;
import org.eclipse.xsd.XSDConcreteComponent;
import org.eclipse.xsd.XSDDerivationMethod;
import org.eclipse.xsd.XSDElementDeclaration;
import org.eclipse.xsd.XSDEnumerationFacet;
import org.eclipse.xsd.XSDFacet;
import org.eclipse.xsd.XSDImport;
import org.eclipse.xsd.XSDInclude;
import org.eclipse.xsd.XSDModelGroup;
import org.eclipse.xsd.XSDModelGroupDefinition;
import org.eclipse.xsd.XSDParticle;
import org.eclipse.xsd.XSDParticleContent;
import org.eclipse.xsd.XSDPatternFacet;
import org.eclipse.xsd.XSDProcessContents;
import org.eclipse.xsd.XSDProhibitedSubstitutions;
import org.eclipse.xsd.XSDRedefine;
import org.eclipse.xsd.XSDRepeatableFacet;
import org.eclipse.xsd.XSDSchema;
import org.eclipse.xsd.XSDSimpleFinal;
import org.eclipse.xsd.XSDSimpleTypeDefinition;
import org.eclipse.xsd.XSDTypeDefinition;
import org.eclipse.xsd.XSDWildcard;
import org.eclipse.xsd.util.XSDParser;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.HashMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.SizeMeasuringInputStream;
import org.modeshape.common.util.SizeMeasuringReader;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.sramp.SrampLexicon;
import org.modeshape.sequencer.xsd.XsdResolvers.SymbolSpace;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * A class that can parse XML Schema Documents, derive a graph structure from the content, and output that graph structure to a
 * supplied {@link SequencerOutput}.
 * <p>
 * This class can be subclassed and any of the 'process' methods overridden to customize the dervied graph structure.
 * </p>
 */
@NotThreadSafe
public class XsdReader {

    public static final String UNBOUNDED = "unbounded";

    protected final SequencerOutput output;
    protected final StreamSequencerContext context;
    protected final Logger logger = Logger.getLogger(getClass());
    protected final Map<Path, Multimap<Name, Integer>> namesByParentPath = new HashMap<Path, Multimap<Name, Integer>>();
    protected final XsdResolvers resolvers = new XsdResolvers();
    protected List<ResolveFuture> resolveFutures = new LinkedList<ResolveFuture>();

    public XsdReader( SequencerOutput output,
                      StreamSequencerContext context ) {
        this.output = output;
        this.context = context;
    }

    /**
     * Get the sequencing context in which this reader is being used.
     * 
     * @return context the context; never null
     */
    public StreamSequencerContext getContext() {
        return context;
    }

    /**
     * Read the XML Schema Document from the supplied string, and produce the derived content. Any problems or exceptions are
     * written to the {@link #getContext() context's} {@link StreamSequencerContext#getProblems() problems}.
     * 
     * @param xsdContent the stream containing the XSD content; may not be null
     * @param docPath the path at which the derived content for the XSD should be written (usually this path represents the XSD
     *        file itself); may not be null
     */
    public void read( String xsdContent,
                      Path docPath ) {
        read(new InputSource(new StringReader(xsdContent)), docPath);
    }

    /**
     * Read the XML Schema Document from the supplied stream, and produce the derived content. Any problems or exceptions are
     * written to the {@link #getContext() context's} {@link StreamSequencerContext#getProblems() problems}.
     * 
     * @param stream the stream containing the XSD content; may not be null
     * @param docPath the path at which the derived content for the XSD should be written (usually this path represents the XSD
     *        file itself); may not be null
     */
    public void read( InputStream stream,
                      Path docPath ) {
        read(new InputSource(stream), docPath);
    }

    /**
     * Read the XML Schema Document from the supplied source, and produce the derived content. Any problems or exceptions are
     * written to the {@link #getContext() context's} {@link StreamSequencerContext#getProblems() problems}.
     * 
     * @param source the input source containing the XSD content; may not be null
     * @param docPath the path at which the derived content for the XSD should be written (usually this path represents the XSD
     *        file itself); may not be null
     */
    public void read( InputSource source,
                      Path docPath ) {
        logger.trace("Processing XSD '{0}'", string(docPath));
        Reader reader = null;
        InputStream stream = null;
        try {
            // Parse the XSD, measuring the number of bytes as we read ...
            Map<?, ?> options = new HashMap<Object, Object>();
            XSDParser parser = new XSDParser(options);
            AtomicLong contentSize = new AtomicLong();
            if (source.getCharacterStream() != null) {
                reader = new SizeMeasuringReader(source.getCharacterStream(), contentSize);
                source = new InputSource(reader);
            } else {
                stream = new SizeMeasuringInputStream(source.getByteStream(), contentSize);
                source = new InputSource(stream);
            }
            parser.parse(source);

            // Get some metadata about the XSD ...
            String encoding = parser.getEncoding();
            String mimeType = context.getMimeType();

            // Convert the XSD to content ...
            XSDSchema schema = parser.getSchema();
            process(schema, encoding, mimeType, contentSize.get(), docPath);

        } catch (Exception e) {
            String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
            context.getProblems().addError(e, XsdI18n.errorReadingXsdFile, location, e.getMessage());
        } finally {
            assert (reader != null && stream == null) || (reader == null && stream != null);
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
                context.getProblems().addError(e, XsdI18n.errorClosingXsdFile, location, e.getMessage());
            } finally {
                try {
                    if (stream != null) stream.close();
                } catch (Exception e) {
                    String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
                    context.getProblems().addError(e, XsdI18n.errorClosingXsdFile, location, e.getMessage());
                }
            }
        }
    }

    /**
     * Read an XSDSchema instance.
     * 
     * @param schema
     * @param encoding
     * @param mimeType
     * @param contentSize
     * @param path the desired path of the derived node representing the schema; may not be null
     * @return the path of the derived node representing the schema; never null
     */
    protected Path process( XSDSchema schema,
                            String encoding,
                            String mimeType,
                            long contentSize,
                            Path path ) {
        assert schema != null;

        logger.trace("Target namespace: '{0}'", schema.getTargetNamespace());
        output.setProperty(path, SrampLexicon.CONTENT_TYPE, "application/xsd");
        if (encoding != null) {
            output.setProperty(path, SrampLexicon.CONTENT_ENCODING, encoding);
        }
        output.setProperty(path, SrampLexicon.CONTENT_SIZE, contentSize);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.SCHEMA_DOCUMENT);

        // Parse the annotations first to aggregate them all into a single 'sramp:description' property ...
        @SuppressWarnings( "unchecked" )
        List<XSDAnnotation> annotations = schema.getAnnotations();
        process(annotations, path);
        processNonSchemaAttributes(schema, path);

        // Parse the objects ...
        for (EObject obj : schema.eContents()) {
            if (obj instanceof XSDSimpleTypeDefinition) {
                process((XSDSimpleTypeDefinition)obj, path);
            } else if (obj instanceof XSDComplexTypeDefinition) {
                process((XSDComplexTypeDefinition)obj, path);
            } else if (obj instanceof XSDElementDeclaration) {
                process((XSDElementDeclaration)obj, path);
            } else if (obj instanceof XSDAttributeDeclaration) {
                process((XSDAttributeDeclaration)obj, path, false);
            } else if (obj instanceof XSDImport) {
                process((XSDImport)obj, path);
            } else if (obj instanceof XSDInclude) {
                process((XSDInclude)obj, path);
            } else if (obj instanceof XSDRedefine) {
                process((XSDRedefine)obj, path);
            } else if (obj instanceof XSDAttributeGroupDefinition) {
                process((XSDAttributeGroupDefinition)obj, path);
            } else if (obj instanceof XSDAnnotation) {
                // already processed above ...
            }
        }

        // Resolve any outstanding, unresolved references ...
        resolveReferences();
        return path;
    }

    protected Path process( XSDImport xsdImport,
                            Path parentPath ) {
        logger.trace("Import: '{0}' with location '{1}' ", xsdImport.getNamespace(), xsdImport.getSchemaLocation());
        Path path = nextPath(parentPath, XsdLexicon.IMPORT);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.IMPORT);
        output.setProperty(path, XsdLexicon.NAMESPACE, xsdImport.getNamespace());
        output.setProperty(path, XsdLexicon.SCHEMA_LOCATION, xsdImport.getSchemaLocation());
        processNonSchemaAttributes(xsdImport, path);
        return path;
    }

    protected Path process( XSDInclude xsdInclude,
                            Path parentPath ) {
        logger.trace("Include: '{0}' ", xsdInclude.getSchemaLocation());
        Path path = nextPath(parentPath, XsdLexicon.INCLUDE);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.INCLUDE);
        output.setProperty(path, XsdLexicon.SCHEMA_LOCATION, xsdInclude.getSchemaLocation());
        processNonSchemaAttributes(xsdInclude, path);
        return path;
    }

    protected Path process( XSDRedefine redefine,
                            Path parentPath ) {
        logger.trace("Include: '{0}' ", redefine.getSchemaLocation());
        Path path = nextPath(parentPath, XsdLexicon.REDEFINE);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.REDEFINE);
        output.setProperty(path, XsdLexicon.SCHEMA_LOCATION, redefine.getSchemaLocation());
        processNonSchemaAttributes(redefine, path);
        return path;
    }

    protected Path process( XSDSimpleTypeDefinition type,
                            Path parentPath ) {
        assert type.getName() != null;
        // This is a normal simple type definition ...
        logger.trace("Simple type: '{0}' in ns '{1}' ", type.getName(), type.getTargetNamespace());
        Path path = nextPath(parentPath, name(type.getName()));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.SIMPLE_TYPE_DEFINITION);
        output.setProperty(path, XsdLexicon.NC_NAME, type.getName());
        output.setProperty(path, XsdLexicon.NAMESPACE, type.getTargetNamespace());
        UUID uuid = setUuid(path);
        resolvers.get(SymbolSpace.TYPE_DEFINITIONS).register(type.getTargetNamespace(), type.getName(), path, uuid);
        processFacetsOf(type, path, type.getBaseType());
        processNonSchemaAttributes(type, path);
        return path;
    }

    protected Path processFacetsOf( XSDSimpleTypeDefinition type,
                                    Path path,
                                    XSDTypeDefinition baseType ) {
        if (baseType == null) baseType = type.getBaseType();
        if (baseType == type) {
            // The base type is the anytype ...
            baseType = type.getSchema()
                           .getSchemaForSchema()
                           .resolveSimpleTypeDefinition("http://www.w3.org/2001/XMLSchema", "anyType");
        }
        if (baseType != null) {
            output.setProperty(path, XsdLexicon.BASE_TYPE_NAME, baseType.getName());
            output.setProperty(path, XsdLexicon.BASE_TYPE_NAMESPACE, baseType.getTargetNamespace());
            setReference(path,
                         XsdLexicon.BASE_TYPE_REFERENCE,
                         SymbolSpace.TYPE_DEFINITIONS,
                         baseType.getTargetNamespace(),
                         baseType.getName());
        }
        // if ( type.getFacetContents() != null ) {
        // for ( Object facet : type.getFacetContents() ) {
        //
        // }
        // }
        process(type.getEffectiveMaxLengthFacet(), path, XsdLexicon.MAX_LENGTH, PropertyType.LONG);
        process(type.getMaxLengthFacet(), path, XsdLexicon.MAX_LENGTH, PropertyType.LONG);
        process(type.getEffectiveMinLengthFacet(), path, XsdLexicon.MIN_LENGTH, PropertyType.LONG);
        process(type.getMinLengthFacet(), path, XsdLexicon.MIN_LENGTH, PropertyType.LONG);
        process(type.getEffectiveMaxFacet(), path, XsdLexicon.MAX_VALUE_EXCLUSIVE, PropertyType.LONG);
        process(type.getMaxExclusiveFacet(), path, XsdLexicon.MAX_VALUE_EXCLUSIVE, PropertyType.LONG);
        process(type.getEffectiveMinFacet(), path, XsdLexicon.MIN_VALUE_EXCLUSIVE, PropertyType.LONG);
        process(type.getMinExclusiveFacet(), path, XsdLexicon.MIN_VALUE_EXCLUSIVE, PropertyType.LONG);
        process(type.getMaxInclusiveFacet(), path, XsdLexicon.MAX_VALUE_INCLUSIVE, PropertyType.LONG);
        process(type.getMinInclusiveFacet(), path, XsdLexicon.MIN_VALUE_INCLUSIVE, PropertyType.LONG);
        process(type.getEffectiveTotalDigitsFacet(), path, XsdLexicon.TOTAL_DIGITS, PropertyType.LONG);
        process(type.getTotalDigitsFacet(), path, XsdLexicon.TOTAL_DIGITS, PropertyType.LONG);
        process(type.getEffectiveFractionDigitsFacet(), path, XsdLexicon.FRACTION_DIGITS, PropertyType.LONG);
        process(type.getFractionDigitsFacet(), path, XsdLexicon.FRACTION_DIGITS, PropertyType.LONG);

        process(type.getEffectiveWhiteSpaceFacet(), path, XsdLexicon.WHITESPACE, PropertyType.STRING);
        process(type.getWhiteSpaceFacet(), path, XsdLexicon.WHITESPACE, PropertyType.STRING);

        process(type.getEffectivePatternFacet(), path, XsdLexicon.PATTERN, PropertyType.STRING);
        @SuppressWarnings( "unchecked" )
        List<XSDPatternFacet> patternFacets = type.getPatternFacets();
        process(patternFacets, path, XsdLexicon.PATTERN, PropertyType.STRING);

        process(type.getEffectiveEnumerationFacet(), path, XsdLexicon.ENUMERATED_VALUES, PropertyType.STRING);
        @SuppressWarnings( "unchecked" )
        List<XSDEnumerationFacet> enumFacets = type.getEnumerationFacets();
        process(enumFacets, path, XsdLexicon.ENUMERATED_VALUES, PropertyType.STRING);

        @SuppressWarnings( "unchecked" )
        List<XSDSimpleFinal> finalFacets2 = type.getFinal();
        processEnumerators(finalFacets2, path, XsdLexicon.FINAL);

        process(type.getAnnotation(), path);
        return path;
    }

    protected Path process( XSDComplexTypeDefinition type,
                            Path parentPath ) {
        logger.trace("Complex type: '{0}' in ns '{1}' ", type.getName(), type.getTargetNamespace());
        Path path = nextPath(parentPath, name(type.getName()));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.COMPLEX_TYPE_DEFINITION);
        output.setProperty(path, XsdLexicon.NC_NAME, type.getName());
        output.setProperty(path, XsdLexicon.NAMESPACE, type.getTargetNamespace());
        UUID uuid = setUuid(path);
        resolvers.get(SymbolSpace.TYPE_DEFINITIONS).register(type.getTargetNamespace(), type.getName(), path, uuid);
        XSDTypeDefinition baseType = type.getBaseType();
        if (baseType == type) {
            // The base type is the anytype ...
            baseType = type.getSchema()
                           .getSchemaForSchema()
                           .resolveComplexTypeDefinition("http://www.w3.org/2001/XMLSchema", "anyType");
        }
        if (baseType != null) {
            output.setProperty(path, XsdLexicon.BASE_TYPE_NAME, baseType.getName());
            output.setProperty(path, XsdLexicon.BASE_TYPE_NAMESPACE, baseType.getTargetNamespace());
        }
        output.setProperty(path, XsdLexicon.ABSTRACT, type.isAbstract());
        output.setProperty(path, XsdLexicon.MIXED, type.isMixed());

        @SuppressWarnings( "unchecked" )
        List<XSDProhibitedSubstitutions> blocks = type.getBlock();
        processEnumerators(blocks, path, XsdLexicon.BLOCK);

        @SuppressWarnings( "unchecked" )
        List<XSDSimpleFinal> finalFacets = type.getFinal();
        processEnumerators(finalFacets, path, XsdLexicon.FINAL);

        process(type.getContent(), path);

        process(type.getAnnotation(), path);
        processNonSchemaAttributes(type, path);
        return path;
    }

    protected Path process( XSDElementDeclaration decl,
                            Path parentPath ) {
        if (decl == null) return null;
        logger.trace("Element declaration: '{0}' in ns '{1}' ", decl.getName(), decl.getTargetNamespace());
        Path path = null;
        if (decl.getName() != null) {
            // Normal element declaration ...
            logger.trace("Element declaration: '{0}' in ns '{1}' ", decl.getName(), decl.getTargetNamespace());
            path = nextPath(parentPath, name(decl.getName()));
            output.setProperty(path, XsdLexicon.NC_NAME, decl.getName());
            output.setProperty(path, XsdLexicon.NAMESPACE, decl.getTargetNamespace());
        } else {
            assert decl.isFeatureReference() : "expected element reference";
            XSDElementDeclaration resolved = decl.getResolvedElementDeclaration();
            logger.trace("Element reference to '{0}' in ns '{1}' ", resolved.getName(), resolved.getTargetNamespace());
            path = nextPath(parentPath, name(resolved.getName()));
            output.setProperty(path, XsdLexicon.REF_NAME, resolved.getName());
            output.setProperty(path, XsdLexicon.REF_NAMESPACE, resolved.getTargetNamespace());
            setReference(path, XsdLexicon.REF, SymbolSpace.ELEMENT_DECLARATION, resolved.getTargetNamespace(), resolved.getName());
        }
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.ELEMENT_DECLARATION);
        if (decl.isGlobal()) {
            UUID uuid = setUuid(path);
            resolvers.get(SymbolSpace.ELEMENT_DECLARATION).register(decl.getTargetNamespace(), decl.getName(), path, uuid);
        }

        output.setProperty(path, XsdLexicon.ABSTRACT, decl.isAbstract());
        output.setProperty(path, XsdLexicon.NILLABLE, decl.isNillable());

        XSDTypeDefinition type = decl.getType();
        if (type != null) {
            output.setProperty(path, XsdLexicon.TYPE_NAME, type.getName());
            output.setProperty(path, XsdLexicon.TYPE_NAMESPACE, type.getTargetNamespace());
            setReference(path, XsdLexicon.TYPE_REFERENCE, SymbolSpace.TYPE_DEFINITIONS, type.getTargetNamespace(), type.getName());
        }
        processEnumerator(decl.getForm(), path, XsdLexicon.FORM);

        @SuppressWarnings( "unchecked" )
        List<XSDProhibitedSubstitutions> finals = decl.getLexicalFinal();
        processEnumerators(finals, path, XsdLexicon.FINAL);

        @SuppressWarnings( "unchecked" )
        List<XSDProhibitedSubstitutions> blocks = decl.getBlock();
        processEnumerators(blocks, path, XsdLexicon.BLOCK);

        process(decl.getAnnotation(), path);
        processNonSchemaAttributes(type, path);
        return path;
    }

    protected Path process( XSDAttributeDeclaration decl,
                            Path parentPath,
                            boolean isUse ) {
        if (decl == null) return null;
        logger.trace("Attribute declaration: '{0}' in ns '{1}' ", decl.getName(), decl.getTargetNamespace());
        Path path = nextPath(parentPath, name(decl.getName()));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.ATTRIBUTE_DECLARATION);
        output.setProperty(path, XsdLexicon.NC_NAME, decl.getName());
        output.setProperty(path, XsdLexicon.NAMESPACE, decl.getTargetNamespace());
        if (decl.isGlobal() && !isUse) {
            UUID uuid = setUuid(path);
            resolvers.get(SymbolSpace.ATTRIBUTE_DECLARATIONS).register(decl.getTargetNamespace(), decl.getName(), path, uuid);
        }
        XSDTypeDefinition type = decl.getType();
        if (type != null) {
            output.setProperty(path, XsdLexicon.TYPE_NAME, type.getName());
            output.setProperty(path, XsdLexicon.TYPE_NAMESPACE, type.getTargetNamespace());
        }
        process(decl.getAnnotation(), path);
        processNonSchemaAttributes(type, path);
        return path;
    }

    protected Path process( XSDComplexTypeContent content,
                            Path parentPath ) {
        if (content == null) return null;

        XSDComplexTypeDefinition owner = (XSDComplexTypeDefinition)content.eContainer();

        Path resultPath = null;
        if (content instanceof XSDParticle) {
            resultPath = process((XSDParticle)content, parentPath);
        } else if (content instanceof XSDSimpleTypeDefinition) {
            Path simpleContentPath = nextPath(parentPath, XsdLexicon.SIMPLE_CONTENT);
            output.setProperty(simpleContentPath, JcrLexicon.PRIMARY_TYPE, XsdLexicon.SIMPLE_CONTENT);
            processFacetsOf((XSDSimpleTypeDefinition)content, simpleContentPath, owner.getBaseTypeDefinition());
            resultPath = simpleContentPath;
        }

        XSDDerivationMethod method = owner.getDerivationMethod();
        if (method != null) {
            output.setProperty(parentPath, XsdLexicon.METHOD, method.getLiteral());
        }

        @SuppressWarnings( "unchecked" )
        List<XSDAttributeGroupContent> attributeGroupContents = owner.getAttributeContents();
        if (attributeGroupContents != null) {
            for (XSDAttributeGroupContent attributeGroup : attributeGroupContents) {
                process(attributeGroup, parentPath);
            }
        }
        @SuppressWarnings( "unchecked" )
        List<XSDAttributeUse> attributeUses = owner.getAttributeUses();
        if (attributeUses != null) {
            for (XSDAttributeUse attributeUse : attributeUses) {
                process(attributeUse, parentPath);
            }
        }
        XSDWildcard wildcard = owner.getAttributeWildcard();
        process(wildcard, parentPath);
        processNonSchemaAttributes(owner, parentPath);
        return resultPath;
    }

    protected Path process( XSDParticle content,
                            Path parentPath ) {
        if (content == null) return null;
        XSDParticleContent particle = content.getContent();
        Path path = null;
        if (particle instanceof XSDModelGroupDefinition) {
            path = process((XSDModelGroupDefinition)particle, parentPath);
        } else if (particle instanceof XSDElementDeclaration) {
            path = process((XSDElementDeclaration)particle, parentPath);
        } else if (particle instanceof XSDModelGroup) {
            path = process((XSDModelGroup)particle, parentPath);
        } else if (particle instanceof XSDWildcard) {
            path = process((XSDWildcard)particle, parentPath);
        }
        if (path != null) {
            long minOccurs = content.getMinOccurs();
            long maxOccurs = content.getMaxOccurs();
            output.setProperty(path, XsdLexicon.MIN_OCCURS, minOccurs);
            if (maxOccurs >= 0) {
                output.setProperty(path, XsdLexicon.MAX_OCCURS, maxOccurs);
            } else {
                // unbounded ...
            }
        }
        return path;
    }

    protected Path process( XSDModelGroupDefinition defn,
                            Path parentPath ) {
        if (defn == null) return null;
        XSDModelGroup group = defn.getModelGroup();
        processNonSchemaAttributes(defn, parentPath);
        return process(group, parentPath);
    }

    protected Path process( XSDModelGroup group,
                            Path parentPath ) {
        if (group == null) return null;
        XSDCompositor compositor = group.getCompositor();
        Name primaryTypeName = null;
        switch (compositor.getValue()) {
            case XSDCompositor.ALL:
                primaryTypeName = XsdLexicon.ALL;
                break;
            case XSDCompositor.CHOICE:
                primaryTypeName = XsdLexicon.CHOICE;
                break;
            case XSDCompositor.SEQUENCE:
                primaryTypeName = XsdLexicon.SEQUENCE;
                break;
            default:
                assert false : "should not get here";
        }
        assert primaryTypeName != null;
        Path path = nextPath(parentPath, primaryTypeName);
        // Path path = nextPath(parentPath, name(primaryTypeName.getLocalName()));
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, primaryTypeName);
        @SuppressWarnings( "unchecked" )
        List<XSDParticle> particles = group.getParticles();
        for (XSDParticle particle : particles) {
            process(particle, path);
        }
        processNonSchemaAttributes(group, path);
        return path;
    }

    protected Path process( XSDAttributeGroupContent content,
                            Path parentPath ) {
        if (content == null) return null;
        if (content instanceof XSDAttributeGroupDefinition) {
            return process((XSDAttributeGroupDefinition)content, parentPath);
        }
        if (content instanceof XSDAttributeUse) {
            return process((XSDAttributeUse)content, parentPath);
        }
        assert false : "should not get here";
        return null;
    }

    protected Path process( XSDAttributeGroupDefinition defn,
                            Path parentPath ) {
        if (defn == null) return null;
        Path path = null;
        if (defn.isAttributeGroupDefinitionReference()) {
            XSDAttributeGroupDefinition resolved = defn.getResolvedAttributeGroupDefinition();
            logger.trace("Attribute Group definition (ref): '{0}' in ns '{1}' ",
                         resolved.getName(),
                         resolved.getTargetNamespace());
            path = nextPath(parentPath, name(resolved.getName()));
            output.setProperty(path, XsdLexicon.REF_NAME, resolved.getName());
            output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.ATTRIBUTE_GROUP);
            setReference(path,
                         XsdLexicon.REF,
                         SymbolSpace.ATTRIBUTE_GROUP_DEFINITIONS,
                         resolved.getTargetNamespace(),
                         resolved.getName());
        } else {
            logger.trace("Attribute Group definition: '{0}' in ns '{1}' ", defn.getName(), defn.getTargetNamespace());
            path = nextPath(parentPath, name(defn.getName()));
            UUID uuid = setUuid(path);
            resolvers.get(SymbolSpace.ATTRIBUTE_GROUP_DEFINITIONS)
                     .register(defn.getTargetNamespace(), defn.getName(), path, uuid);
            output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.ATTRIBUTE_GROUP);
            output.setProperty(path, XsdLexicon.NC_NAME, defn.getName());
            output.setProperty(path, XsdLexicon.NAMESPACE, defn.getTargetNamespace());

            for (Object child : defn.getContents()) {
                if (child instanceof XSDAttributeUse) {
                    process((XSDAttributeUse)child, path);
                } else if (child instanceof XSDWildcard) {
                    process((XSDWildcard)child, path);
                }
            }
        }
        process(defn.getAnnotation(), path);
        processNonSchemaAttributes(defn, path);
        return path;
    }

    protected Path process( XSDWildcard wildcard,
                            Path parentPath ) {
        if (wildcard == null) return null;
        logger.trace("Any Attribute");
        Path path = nextPath(parentPath, XsdLexicon.ANY_ATTRIBUTE);
        output.setProperty(path, JcrLexicon.PRIMARY_TYPE, XsdLexicon.ANY_ATTRIBUTE);
        @SuppressWarnings( "unchecked" )
        EList<String> nsConstraints = wildcard.getNamespaceConstraint();
        if (nsConstraints != null && !nsConstraints.isEmpty()) {
            Set<String> values = new HashSet<String>();
            for (String nsConstraint : nsConstraints) {
                if (nsConstraint == null) continue;
                nsConstraint = nsConstraint.trim();
                if (nsConstraint.length() == 0) continue;
                values.add(nsConstraint);
            }
            if (!values.isEmpty()) {
                output.setProperty(path, XsdLexicon.NAMESPACE, values.toArray(new Object[values.size()]));
            }
        }
        if (wildcard.getProcessContents() != null) {
            XSDProcessContents processContents = wildcard.getProcessContents();
            output.setProperty(path, XsdLexicon.PROCESS_CONTENTS, processContents.getLiteral());
        }
        process(wildcard.getAnnotation(), path);
        processNonSchemaAttributes(wildcard, path);
        return path;
    }

    protected Path process( XSDAttributeUse use,
                            Path parentPath ) {
        // Process the attribute declaration ...
        Path path = process(use.getAttributeDeclaration(), parentPath, true);
        if (use.getUse() != null) {
            output.setProperty(path, XsdLexicon.USE, use.getUse().getLiteral());
        }
        processNonSchemaAttributes(use, path);
        return path;
    }

    protected Path processNonSchemaAttributes( XSDConcreteComponent component,
                                               Path path ) {
        if (component == null) return null;
        Element element = component.getElement();
        if (element == null) return null;
        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) return null;
        for (int i = 0, len = attributes.getLength(); i != len; ++i) {
            Node attribute = attributes.item(i);
            if (attribute.getNodeType() != Node.ATTRIBUTE_NODE) continue;
            String namespaceUri = attribute.getNamespaceURI();
            if (!XsdLexicon.Namespace.URI.equals(namespaceUri)) {
                // Record any attribute that is not in the XSD namespace ...
                String localName = attribute.getLocalName();
                String value = attribute.getNodeValue();
                if (value == null) continue;
                if (namespaceUri != null) {
                    prefixForNamespace(namespaceUri, attribute.getPrefix());
                    output.setProperty(path, name(namespaceUri, localName), value);
                } else {
                    output.setProperty(path, name(localName), value);
                }
            }
        }
        return path;
    }

    protected String prefixForNamespace( String namespaceUri,
                                         String defaultPrefix ) {
        NamespaceRegistry registry = context.getNamespaceRegistry();
        String prefix = registry.getPrefixForNamespaceUri(namespaceUri, false);
        if (prefix == null) {
            if (defaultPrefix == null) {
                prefix = registry.getPrefixForNamespaceUri(namespaceUri, true);
            } else {
                int counter = 2;
                String proposedPrefix = defaultPrefix;
                while (registry.getNamespaceForPrefix(proposedPrefix) != null) {
                    proposedPrefix = defaultPrefix + counter++;
                }
                prefix = registry.register(proposedPrefix, namespaceUri);
            }
        }
        return prefix;
    }

    protected Path process( XSDAnnotation annotation,
                            Path path ) {
        if (annotation != null) {
            StringBuilder sb = new StringBuilder();
            for (Object obj : annotation.getUserInformation()) {
                Element element = (Element)obj;
                if (element.getLocalName().equals("documentation")) {
                    String content = element.getTextContent();
                    if (content != null) sb.append(content);
                }
            }
            if (sb.length() != 0) {
                String content = sb.toString();
                content = content.trim();
                if (content.length() != 0) {
                    output.setProperty(path, SrampLexicon.DESCRIPTION, content);
                }
            }
        }
        return path;
    }

    protected Path process( Iterable<XSDAnnotation> annotations,
                            Path path ) {
        if (annotations != null) {
            StringBuilder sb = new StringBuilder();
            for (XSDAnnotation annotation : annotations) {
                for (Object obj : annotation.getUserInformation()) {
                    Element element = (Element)obj;
                    if (element.getLocalName().equals("documentation")) {
                        String content = element.getTextContent();
                        if (content != null) sb.append(content);
                    }
                }
                sb.append("\n");
            }
            if (sb.length() != 0) {
                String content = sb.toString();
                content = content.trim();
                if (content.length() != 0) {
                    output.setProperty(path, SrampLexicon.DESCRIPTION, content);
                }
            }
        }
        return path;
    }

    protected void process( XSDFacet facet,
                            Path path,
                            Name propertyName,
                            PropertyType type ) {
        if (facet != null) {
            String lexicalValue = facet.getLexicalValue();
            if (lexicalValue != null) {
                Object value = context.getValueFactories().getValueFactory(type).create(facet.getLexicalValue());
                output.setProperty(path, propertyName, value);
            } else if (facet instanceof XSDRepeatableFacet) {
                Set<String> values = new HashSet<String>();
                EList<?> facetValues = null;
                if (facet instanceof XSDPatternFacet) facetValues = ((XSDPatternFacet)facet).getValue();
                else if (facet instanceof XSDEnumerationFacet) facetValues = ((XSDEnumerationFacet)facet).getValue();
                if (facetValues != null && !facetValues.isEmpty()) {
                    for (Object enumValue : facetValues) {
                        values.add(string(enumValue));
                    }
                }
                if (!values.isEmpty()) {
                    output.setProperty(path, propertyName, values.toArray(new Object[values.size()]));
                }
            }
        }
    }

    protected <Facet extends XSDFacet> void process( Iterable<Facet> facets,
                                                     Path path,
                                                     Name propertyName,
                                                     PropertyType type ) {
        if (facets != null) {
            Set<String> values = new HashSet<String>();
            for (XSDFacet facet : facets) {
                String lexicalValue = facet.getLexicalValue();
                if (lexicalValue != null) {
                    values.add(string(facet.getLexicalValue()));
                } else if (facet instanceof XSDRepeatableFacet) {
                    EList<?> facetValues = null;
                    if (facet instanceof XSDPatternFacet) facetValues = ((XSDPatternFacet)facet).getValue();
                    else if (facet instanceof XSDEnumerationFacet) facetValues = ((XSDEnumerationFacet)facet).getValue();
                    if (facetValues != null && !facetValues.isEmpty()) {
                        for (Object enumValue : facetValues) {
                            values.add(string(enumValue));
                        }
                    }
                }
            }
            if (!values.isEmpty()) {
                output.setProperty(path, propertyName, values.toArray(new Object[values.size()]));
            }
        }
    }

    protected <Enumerator extends AbstractEnumerator> void processEnumerators( Iterable<Enumerator> enumerators,
                                                                               Path path,
                                                                               Name propertyName ) {
        if (enumerators != null) {
            Set<String> values = new HashSet<String>();
            for (Enumerator enumValue : enumerators) {
                String value = enumValue.getLiteral();
                if (value != null) {
                    values.add(value);
                }
            }
            if (!values.isEmpty()) {
                output.setProperty(path, propertyName, values.toArray(new Object[values.size()]));
            }
        }
    }

    protected <Enumerator extends AbstractEnumerator> void processEnumerator( Enumerator enumerator,
                                                                              Path path,
                                                                              Name propertyName ) {
        if (enumerator != null) {
            String value = enumerator.getLiteral();
            if (value != null) {
                output.setProperty(path, propertyName, value);
            }
        }
    }

    protected Path nextPath( Path parentPath,
                             Name name ) {
        Multimap<Name, Integer> names = namesByParentPath.get(parentPath);
        int sns = 1;
        if (names == null) {
            names = HashMultimap.create();
            names.put(name, sns);
            namesByParentPath.put(parentPath, names);
        } else {
            sns = names.get(name).size() + 1;
            names.put(name, 1);
        }
        return context.getValueFactories().getPathFactory().create(parentPath, name, sns);
    }

    protected Path path( Path parentPath,
                         Name segment ) {
        return context.getValueFactories().getPathFactory().create(parentPath, segment);
    }

    protected Path path( Path parentPath,
                         String segment ) {
        return context.getValueFactories().getPathFactory().create(parentPath, segment);
    }

    protected Path path( String path ) {
        return context.getValueFactories().getPathFactory().create(path);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    protected Name name( String namespaceUri,
                         String name ) {
        return context.getValueFactories().getNameFactory().create(namespaceUri, name);
    }

    protected String string( Object value ) {
        return context.getValueFactories().getStringFactory().create(value);
    }

    protected UUID setUuid( Path path ) {
        UUID uuid = context.getValueFactories().getUuidFactory().create();
        output.setProperty(path, JcrLexicon.UUID, uuid);
        return uuid;
    }

    protected void resolveReferences() {
        if (resolveFutures.isEmpty()) return;

        List<ResolveFuture> futures = resolveFutures;
        resolveFutures = new LinkedList<ResolveFuture>();
        for (ResolveFuture future : futures) {
            future.resolve(); // anything not resolved goes back on the new 'resolvedFutures' list ...
        }
    }

    protected UUID setReference( Path path,
                                 Name propertyName,
                                 SymbolSpace kind,
                                 String namespace,
                                 String name ) {
        UUID typeUuid = resolvers.get(kind).lookup(namespace, name);
        if (typeUuid != null) {
            // The referenced object was already processed ...
            output.setProperty(path, propertyName, typeUuid);
        } else {
            // The referenced object may not have been processed, so put it in the queue ...
            resolveFutures.add(new ResolveFuture(path, propertyName, kind, namespace, name));
        }
        return typeUuid;
    }

    protected class ResolveFuture {
        private final Path path;
        private final Name propertyName;
        private final SymbolSpace refKind;
        private final String refNamespace;
        private final String refName;

        protected ResolveFuture( Path path,
                                 Name propertyName,
                                 SymbolSpace kind,
                                 String namespace,
                                 String name ) {
            this.path = path;
            this.propertyName = propertyName;
            this.refKind = kind;
            this.refNamespace = namespace;
            this.refName = name;
        }

        protected UUID resolve() {
            return setReference(path, propertyName, refKind, refNamespace, refName);
        }
    }

}
