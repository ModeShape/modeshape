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
package org.modeshape.sequencer.xsd;

import static org.modeshape.sequencer.sramp.SrampLexicon.DESCRIPTION;
import static org.modeshape.sequencer.xsd.XsdLexicon.IMPORT;
import java.io.InputStream;
import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
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
import org.modeshape.common.util.SizeMeasuringInputStream;
import org.modeshape.common.util.SizeMeasuringReader;
import org.modeshape.jcr.api.mimetype.MimeTypeConstants;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.sramp.AbstractResolvingReader;
import org.modeshape.sequencer.sramp.SrampLexicon;
import org.modeshape.sequencer.sramp.SymbolSpace;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.InputSource;

/**
 * A class that can parse XML Schema Documents and create a node structure based on the schema information.
 * <p>
 * This class can be subclassed and any of the 'process' methods overridden to customize the derived graph structure.
 * </p>
 */
@NotThreadSafe
public class XsdReader extends AbstractResolvingReader {

    /**
     * In XML Schema, there is a distinct symbol space within each target namespace for each kind of <a
     * href="http://www.w3.org/TR/xmlschema-1/#concepts-data-model">declaration and definition component</a>, except that within a
     * target namespace the simple type definitions and complex type definitions share a single symbol space. See the <a
     * href="http://www.w3.org/TR/xmlschema-1/#concepts-nameSymbolSpaces">specification</a> for details.
     */
    public static final SymbolSpace ATTRIBUTE_DECLARATIONS = new SymbolSpace("AttributeDeclarations");
    public static final SymbolSpace ELEMENT_DECLARATION = new SymbolSpace("ElementDeclarations");
    public static final SymbolSpace TYPE_DEFINITIONS = new SymbolSpace("TypeDeclarations");
    public static final SymbolSpace ATTRIBUTE_GROUP_DEFINITIONS = new SymbolSpace("AttributeGroupDeclarations");
    public static final SymbolSpace MODEL_GROUP_DEFINITIONS = new SymbolSpace("ModelGroupDeclarations");
    public static final SymbolSpace IDENTITY_CONSTRAINT_DEFINITIONS = new SymbolSpace("IdentityConstraintDeclarations");

    public XsdReader( Sequencer.Context context ) {
        super(context);
    }

    @Override
    public void read( InputSource source,
                      Node outputNode ) throws Exception {
        logger.debug("Processing XSD '{0}'", outputNode);
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

            // Convert the XSD to content ...
            XSDSchema schema = parser.getSchema();
            process(schema, encoding, contentSize.get(), outputNode);

        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
                logger.debug(e, "Cannot close reader stream ");
            } finally {
                try {
                    if (stream != null) stream.close();
                } catch (Exception e) {
                    logger.debug(e, "Cannot close reader stream ");
                }
            }
        }
    }

    /**
     * Read an XSDSchema instance and create the node hierarchy under the given root node.
     * 
     * @param schema the schema object; may not be null
     * @param encoding the encoding for the XSD; may be null if the encoding is not specified
     * @param contentSize the size of the XML Schema Document content; may not be negative
     * @param rootNode the root node that will be populated with the XML Schema Document information
     * @throws Exception if there is a probelm reading the XSD content
     */
    protected void process( XSDSchema schema,
                            String encoding,
                            long contentSize,
                            Node rootNode ) throws Exception {
        assert schema != null;

        logger.debug("Target namespace: '{0}'", schema.getTargetNamespace());
        rootNode.setProperty(SrampLexicon.CONTENT_TYPE, MimeTypeConstants.APPLICATION_XML);
        if (encoding != null) {
            rootNode.setProperty(SrampLexicon.CONTENT_ENCODING, encoding);
        }
        rootNode.setProperty(SrampLexicon.CONTENT_SIZE, contentSize);

        // Parse the annotations first to aggregate them all into a single 'sramp:description' property ...
        @SuppressWarnings( "unchecked" )
        List<XSDAnnotation> annotations = schema.getAnnotations();
        processAnnotations(annotations, rootNode);
        processNonSchemaAttributes(schema, rootNode);

        // Parse the objects ...
        for (EObject obj : schema.eContents()) {
            if (obj instanceof XSDSimpleTypeDefinition) {
                processSimpleTypeDefinition((XSDSimpleTypeDefinition)obj, rootNode);
            } else if (obj instanceof XSDComplexTypeDefinition) {
                processComplexTypeDefinition((XSDComplexTypeDefinition)obj, rootNode);
            } else if (obj instanceof XSDElementDeclaration) {
                processElementDeclaration((XSDElementDeclaration)obj, rootNode);
            } else if (obj instanceof XSDAttributeDeclaration) {
                processAttributeDeclaration((XSDAttributeDeclaration)obj, rootNode, false);
            } else if (obj instanceof XSDImport) {
                processImport((XSDImport)obj, rootNode);
            } else if (obj instanceof XSDInclude) {
                processInclude((XSDInclude)obj, rootNode);
            } else if (obj instanceof XSDRedefine) {
                processRedefine((XSDRedefine)obj, rootNode);
            } else if (obj instanceof XSDAttributeGroupDefinition) {
                processAttributeGroupDefinition((XSDAttributeGroupDefinition)obj, rootNode);
            } else if (obj instanceof XSDAnnotation) {
                // already processed above ...
            }
        }

        // Resolve any outstanding, unresolved references ...
        resolveReferences();
    }

    protected void processImport( XSDImport xsdImport,
                                  Node parentNode ) throws RepositoryException {
        logger.debug("Import: '{0}' with location '{1}' ", xsdImport.getNamespace(), xsdImport.getSchemaLocation());
        Node importNode = parentNode.addNode(IMPORT, IMPORT);
        importNode.setProperty(XsdLexicon.NAMESPACE, xsdImport.getNamespace());
        importNode.setProperty(XsdLexicon.SCHEMA_LOCATION, xsdImport.getSchemaLocation());
        processNonSchemaAttributes(xsdImport, importNode);
    }

    protected void processInclude( XSDInclude xsdInclude,
                                   Node parentNode ) throws RepositoryException {
        logger.debug("Include: '{0}' ", xsdInclude.getSchemaLocation());
        Node includeNode = parentNode.addNode(XsdLexicon.INCLUDE, XsdLexicon.INCLUDE);
        includeNode.setProperty(XsdLexicon.SCHEMA_LOCATION, xsdInclude.getSchemaLocation());
        processNonSchemaAttributes(xsdInclude, includeNode);
    }

    protected void processRedefine( XSDRedefine redefine,
                                    Node parentNode ) throws RepositoryException {
        logger.debug("Include: '{0}' ", redefine.getSchemaLocation());
        Node redefineNode = parentNode.addNode(XsdLexicon.REDEFINE, XsdLexicon.REDEFINE);
        redefineNode.setProperty(XsdLexicon.SCHEMA_LOCATION, redefine.getSchemaLocation());
        processNonSchemaAttributes(redefine, redefineNode);
    }

    protected void processSimpleTypeDefinition( XSDSimpleTypeDefinition type,
                                                Node node ) throws RepositoryException {
        boolean isAnonymous = type.getName() == null;
        String nodeName = isAnonymous ? XsdLexicon.SIMPLE_TYPE : type.getName();
        // This is a normal simple type definition ...
        logger.debug("Simple type: '{0}' in ns '{1}' ", type.getName(), type.getTargetNamespace());

        Node typeNode = node.addNode(nodeName, XsdLexicon.SIMPLE_TYPE_DEFINITION);
        typeNode.setProperty(XsdLexicon.NAMESPACE, type.getTargetNamespace());
        if (!isAnonymous) {
            typeNode.setProperty(XsdLexicon.NC_NAME, type.getName());
            registerForSymbolSpace(TYPE_DEFINITIONS, type.getTargetNamespace(), type.getName(), typeNode.getIdentifier());
        }
        processTypeFacets(type, typeNode, type.getBaseType());
        processNonSchemaAttributes(type, typeNode);
    }

    protected void processTypeFacets( XSDSimpleTypeDefinition type,
                                      Node typeNode,
                                      XSDTypeDefinition baseType ) throws RepositoryException {
        if (baseType == null) {
            baseType = type.getBaseType();
        }
        if (baseType == type) {
            // The base type is the anytype ...
            baseType = type.getSchema()
                           .getSchemaForSchema()
                           .resolveSimpleTypeDefinition("http://www.w3.org/2001/XMLSchema", "anyType");
        }
        if (baseType != null) {
            typeNode.setProperty(XsdLexicon.BASE_TYPE_NAME, baseType.getName());
            typeNode.setProperty(XsdLexicon.BASE_TYPE_NAMESPACE, baseType.getTargetNamespace());
            setReference(typeNode,
                         XsdLexicon.BASE_TYPE_REFERENCE,
                         TYPE_DEFINITIONS,
                         baseType.getTargetNamespace(),
                         baseType.getName());
        }

        processFacet(type.getEffectiveMaxLengthFacet(), typeNode, XsdLexicon.MAX_LENGTH, PropertyType.LONG);
        processFacet(type.getMaxLengthFacet(), typeNode, XsdLexicon.MAX_LENGTH, PropertyType.LONG);
        processFacet(type.getEffectiveMinLengthFacet(), typeNode, XsdLexicon.MIN_LENGTH, PropertyType.LONG);
        processFacet(type.getMinLengthFacet(), typeNode, XsdLexicon.MIN_LENGTH, PropertyType.LONG);
        processFacet(type.getEffectiveMaxFacet(), typeNode, XsdLexicon.MAX_VALUE_EXCLUSIVE, PropertyType.LONG);
        processFacet(type.getMaxExclusiveFacet(), typeNode, XsdLexicon.MAX_VALUE_EXCLUSIVE, PropertyType.LONG);
        processFacet(type.getEffectiveMinFacet(), typeNode, XsdLexicon.MIN_VALUE_EXCLUSIVE, PropertyType.LONG);
        processFacet(type.getMinExclusiveFacet(), typeNode, XsdLexicon.MIN_VALUE_EXCLUSIVE, PropertyType.LONG);
        processFacet(type.getMaxInclusiveFacet(), typeNode, XsdLexicon.MAX_VALUE_INCLUSIVE, PropertyType.LONG);
        processFacet(type.getMinInclusiveFacet(), typeNode, XsdLexicon.MIN_VALUE_INCLUSIVE, PropertyType.LONG);
        processFacet(type.getEffectiveTotalDigitsFacet(), typeNode, XsdLexicon.TOTAL_DIGITS, PropertyType.LONG);
        processFacet(type.getTotalDigitsFacet(), typeNode, XsdLexicon.TOTAL_DIGITS, PropertyType.LONG);
        processFacet(type.getEffectiveFractionDigitsFacet(), typeNode, XsdLexicon.FRACTION_DIGITS, PropertyType.LONG);
        processFacet(type.getFractionDigitsFacet(), typeNode, XsdLexicon.FRACTION_DIGITS, PropertyType.LONG);

        processFacet(type.getEffectiveWhiteSpaceFacet(), typeNode, XsdLexicon.WHITESPACE, PropertyType.STRING);
        processFacet(type.getWhiteSpaceFacet(), typeNode, XsdLexicon.WHITESPACE, PropertyType.STRING);

        processFacet(type.getEffectivePatternFacet(), typeNode, XsdLexicon.PATTERN, PropertyType.STRING);
        @SuppressWarnings( "unchecked" )
        List<XSDPatternFacet> patternFacets = type.getPatternFacets();
        processFacetsList(patternFacets, typeNode, XsdLexicon.PATTERN);

        processFacet(type.getEffectiveEnumerationFacet(), typeNode, XsdLexicon.ENUMERATED_VALUES, PropertyType.STRING);
        @SuppressWarnings( "unchecked" )
        List<XSDEnumerationFacet> enumFacets = type.getEnumerationFacets();
        processFacetsList(enumFacets, typeNode, XsdLexicon.ENUMERATED_VALUES);

        @SuppressWarnings( "unchecked" )
        List<XSDSimpleFinal> finalFacets2 = type.getFinal();
        processEnumerators(finalFacets2, typeNode, XsdLexicon.FINAL);

        processAnnotation(type.getAnnotation(), typeNode);
    }

    protected void processComplexTypeDefinition( XSDComplexTypeDefinition type,
                                                 Node parentNode ) throws RepositoryException {
        logger.debug("Complex type: '{0}' in ns '{1}' ", type.getName(), type.getTargetNamespace());
        boolean isAnonymous = type.getName() == null;

        String nodeName = isAnonymous ? XsdLexicon.COMPLEX_TYPE : type.getName();
        Node typeNode = parentNode.addNode(nodeName, XsdLexicon.COMPLEX_TYPE_DEFINITION);
        typeNode.setProperty(XsdLexicon.NAMESPACE, type.getTargetNamespace());
        if (!isAnonymous) {
            typeNode.setProperty(XsdLexicon.NC_NAME, type.getName());
            registerForSymbolSpace(TYPE_DEFINITIONS, type.getTargetNamespace(), type.getName(), typeNode.getIdentifier());
        }
        XSDTypeDefinition baseType = type.getBaseType();
        if (baseType == type) {
            // The base type is the anytype ...
            baseType = type.getSchema()
                           .getSchemaForSchema()
                           .resolveComplexTypeDefinition("http://www.w3.org/2001/XMLSchema", "anyType");
        }
        if (baseType != null) {
            typeNode.setProperty(XsdLexicon.BASE_TYPE_NAME, baseType.getName());
            typeNode.setProperty(XsdLexicon.BASE_TYPE_NAMESPACE, baseType.getTargetNamespace());
        }
        typeNode.setProperty(XsdLexicon.ABSTRACT, type.isAbstract());
        typeNode.setProperty(XsdLexicon.MIXED, type.isMixed());

        @SuppressWarnings( "unchecked" )
        List<XSDProhibitedSubstitutions> blocks = type.getBlock();
        processEnumerators(blocks, typeNode, XsdLexicon.BLOCK);

        @SuppressWarnings( "unchecked" )
        List<XSDSimpleFinal> finalFacets = type.getFinal();
        processEnumerators(finalFacets, typeNode, XsdLexicon.FINAL);

        processComplexTypeContent(type.getContent(), typeNode);

        processAnnotation(type.getAnnotation(), typeNode);
        processNonSchemaAttributes(type, typeNode);
    }

    protected Node processElementDeclaration( XSDElementDeclaration decl,
                                              Node parentNode ) throws RepositoryException {
        if (decl == null) {
            return null;
        }
        logger.debug("Element declaration: '{0}' in ns '{1}' ", decl.getName(), decl.getTargetNamespace());
        Node declarationNode;
        if (decl.getName() != null) {
            // Normal element declaration ...
            declarationNode = parentNode.addNode(decl.getName(), XsdLexicon.ELEMENT_DECLARATION);
            declarationNode.setProperty(XsdLexicon.NC_NAME, decl.getName());
            declarationNode.setProperty(XsdLexicon.NAMESPACE, decl.getTargetNamespace());
        } else {
            assert decl.isFeatureReference() : "expected element reference";
            XSDElementDeclaration resolved = decl.getResolvedElementDeclaration();
            declarationNode = parentNode.addNode(resolved.getName(), XsdLexicon.ELEMENT_DECLARATION);
            declarationNode.setProperty(XsdLexicon.REF_NAME, resolved.getName());
            declarationNode.setProperty(XsdLexicon.REF_NAMESPACE, resolved.getTargetNamespace());
            setReference(declarationNode, XsdLexicon.REF, ELEMENT_DECLARATION, resolved.getTargetNamespace(), resolved.getName());
        }
        if (decl.isGlobal()) {
            registerForSymbolSpace(ELEMENT_DECLARATION,
                                   decl.getTargetNamespace(),
                                   decl.getName(),
                                   declarationNode.getIdentifier());
        }

        declarationNode.setProperty(XsdLexicon.ABSTRACT, decl.isAbstract());
        declarationNode.setProperty(XsdLexicon.NILLABLE, decl.isNillable());

        XSDTypeDefinition type = decl.getType();
        if (type != null) {
            declarationNode.setProperty(XsdLexicon.TYPE_NAME, type.getName());
            declarationNode.setProperty(XsdLexicon.TYPE_NAMESPACE, type.getTargetNamespace());
            setReference(declarationNode, XsdLexicon.TYPE_REFERENCE, TYPE_DEFINITIONS, type.getTargetNamespace(), type.getName());
        }

        if (decl.getAnonymousTypeDefinition() == type) {
            // It's anonymous, so we need to process the definition here ...
            if (type instanceof XSDComplexTypeDefinition) {
                processComplexTypeDefinition((XSDComplexTypeDefinition)type, declarationNode);
            } else if (type instanceof XSDSimpleTypeDefinition) {
                processSimpleTypeDefinition((XSDSimpleTypeDefinition)type, declarationNode);
            }
        }
        processEnumerator(decl.getForm(), declarationNode, XsdLexicon.FORM);

        @SuppressWarnings( "unchecked" )
        List<XSDProhibitedSubstitutions> finals = decl.getLexicalFinal();
        processEnumerators(finals, declarationNode, XsdLexicon.FINAL);

        @SuppressWarnings( "unchecked" )
        List<XSDProhibitedSubstitutions> blocks = decl.getBlock();
        processEnumerators(blocks, declarationNode, XsdLexicon.BLOCK);

        processAnnotation(decl.getAnnotation(), declarationNode);
        processNonSchemaAttributes(type, declarationNode);
        return declarationNode;
    }

    protected Node processAttributeDeclaration( XSDAttributeDeclaration decl,
                                                Node parentNode,
                                                boolean isUse ) throws RepositoryException {
        if (decl == null) {
            return null;
        }
        logger.debug("Attribute declaration: '{0}' in ns '{1}' ", decl.getName(), decl.getTargetNamespace());

        Node attributeDeclarationNode = parentNode.addNode(decl.getName(), XsdLexicon.ATTRIBUTE_DECLARATION);
        attributeDeclarationNode.setProperty(XsdLexicon.NC_NAME, decl.getName());
        attributeDeclarationNode.setProperty(XsdLexicon.NAMESPACE, decl.getTargetNamespace());
        if (decl.isGlobal() && !isUse) {
            registerForSymbolSpace(ATTRIBUTE_DECLARATIONS,
                                   decl.getTargetNamespace(),
                                   decl.getName(),
                                   attributeDeclarationNode.getIdentifier());
        }
        XSDTypeDefinition type = decl.getType();
        if (type != null) {
            attributeDeclarationNode.setProperty(XsdLexicon.TYPE_NAME, type.getName());
            attributeDeclarationNode.setProperty(XsdLexicon.TYPE_NAMESPACE, type.getTargetNamespace());
        }
        processAnnotation(decl.getAnnotation(), attributeDeclarationNode);
        processNonSchemaAttributes(type, attributeDeclarationNode);
        return attributeDeclarationNode;
    }

    protected void processComplexTypeContent( XSDComplexTypeContent content,
                                              Node parentNode ) throws RepositoryException {
        if (content == null) {
            return;
        }

        XSDComplexTypeDefinition owner = (XSDComplexTypeDefinition)content.eContainer();

        if (content instanceof XSDParticle) {
            processParticle((XSDParticle)content, parentNode);
        } else if (content instanceof XSDSimpleTypeDefinition) {
            Node contentNode = parentNode.addNode(XsdLexicon.SIMPLE_CONTENT, XsdLexicon.SIMPLE_CONTENT);
            processTypeFacets((XSDSimpleTypeDefinition)content, contentNode, owner.getBaseTypeDefinition());
        }

        XSDDerivationMethod method = owner.getDerivationMethod();
        if (method != null) {
            parentNode.setProperty(XsdLexicon.METHOD, method.getLiteral());
        }

        @SuppressWarnings( "unchecked" )
        List<XSDAttributeGroupContent> attributeGroupContents = owner.getAttributeContents();
        if (attributeGroupContents != null) {
            for (XSDAttributeGroupContent attributeGroup : attributeGroupContents) {
                processAttributeGroupContent(attributeGroup, parentNode);
            }
        }
        @SuppressWarnings( "unchecked" )
        List<XSDAttributeUse> attributeUses = owner.getAttributeUses();
        if (attributeUses != null) {
            for (XSDAttributeUse attributeUse : attributeUses) {
                processAttributeUse(attributeUse, parentNode);
            }
        }
        XSDWildcard wildcard = owner.getAttributeWildcard();
        processWildcard(wildcard, parentNode);
        processNonSchemaAttributes(owner, parentNode);
    }

    protected void processParticle( XSDParticle content,
                                    Node node ) throws RepositoryException {
        if (content == null) {
            return;
        }
        XSDParticleContent particle = content.getContent();
        Node particleNode = null;
        if (particle instanceof XSDModelGroupDefinition) {
            particleNode = processModelGroupDefinition((XSDModelGroupDefinition)particle, node);
        } else if (particle instanceof XSDElementDeclaration) {
            particleNode = processElementDeclaration((XSDElementDeclaration)particle, node);
        } else if (particle instanceof XSDModelGroup) {
            particleNode = processModelGroup((XSDModelGroup)particle, node);
        } else if (particle instanceof XSDWildcard) {
            particleNode = processWildcard((XSDWildcard)particle, node);
        }
        if (particleNode != null) {
            long minOccurs = content.getMinOccurs();
            long maxOccurs = content.getMaxOccurs();
            particleNode.setProperty(XsdLexicon.MIN_OCCURS, minOccurs);
            if (maxOccurs >= 0) {
                particleNode.setProperty(XsdLexicon.MAX_OCCURS, maxOccurs);
            } else {
                // unbounded ...
            }
        }
    }

    protected Node processModelGroupDefinition( XSDModelGroupDefinition defn,
                                                Node parentNode ) throws RepositoryException {
        if (defn == null) {
            return null;
        }
        XSDModelGroup group = defn.getModelGroup();
        processNonSchemaAttributes(defn, parentNode);
        return processModelGroup(group, parentNode);
    }

    protected Node processModelGroup( XSDModelGroup group,
                                      Node parentNode ) throws RepositoryException {
        if (group == null) {
            return null;
        }
        XSDCompositor compositor = group.getCompositor();
        String primaryTypeName = getPrimaryTypeFromCompositor(compositor);

        Node childNode = parentNode.addNode(primaryTypeName, primaryTypeName);
        @SuppressWarnings( "unchecked" )
        List<XSDParticle> particles = group.getParticles();
        for (XSDParticle particle : particles) {
            processParticle(particle, childNode);
        }
        processNonSchemaAttributes(group, childNode);
        return childNode;
    }

    private String getPrimaryTypeFromCompositor( XSDCompositor compositor ) {
        String primaryTypeName = null;
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
        return primaryTypeName;
    }

    protected void processAttributeGroupContent( XSDAttributeGroupContent content,
                                                 Node parentNode ) throws RepositoryException {
        if (content == null) {
            return;
        }
        if (content instanceof XSDAttributeGroupDefinition) {
            processAttributeGroupDefinition((XSDAttributeGroupDefinition)content, parentNode);
            return;
        }
        if (content instanceof XSDAttributeUse) {
            processAttributeUse((XSDAttributeUse)content, parentNode);
            return;
        }
        assert false : "Invalid attribute group content type";
    }

    protected void processAttributeGroupDefinition( XSDAttributeGroupDefinition defn,
                                                    Node parentNode ) throws RepositoryException {
        if (defn == null) {
            return;
        }
        Node attributeGroupNode = null;
        if (defn.isAttributeGroupDefinitionReference()) {
            XSDAttributeGroupDefinition resolved = defn.getResolvedAttributeGroupDefinition();
            logger.debug("Attribute Group definition (ref): '{0}' in ns '{1}' ", resolved.getName(), resolved.getTargetNamespace());
            attributeGroupNode = parentNode.addNode(resolved.getName(), XsdLexicon.ATTRIBUTE_GROUP);
            setReference(attributeGroupNode,
                         XsdLexicon.REF,
                         ATTRIBUTE_GROUP_DEFINITIONS,
                         resolved.getTargetNamespace(),
                         resolved.getName());
        } else {
            logger.debug("Attribute Group definition: '{0}' in ns '{1}' ", defn.getName(), defn.getTargetNamespace());
            attributeGroupNode = parentNode.addNode(defn.getName(), XsdLexicon.ATTRIBUTE_GROUP);
            registerForSymbolSpace(ATTRIBUTE_GROUP_DEFINITIONS,
                                   defn.getTargetNamespace(),
                                   defn.getName(),
                                   attributeGroupNode.getIdentifier());
            attributeGroupNode.setProperty(XsdLexicon.NC_NAME, defn.getName());
            attributeGroupNode.setProperty(XsdLexicon.NAMESPACE, defn.getTargetNamespace());

            for (Object child : defn.getContents()) {
                if (child instanceof XSDAttributeUse) {
                    processAttributeUse((XSDAttributeUse)child, attributeGroupNode);
                } else if (child instanceof XSDWildcard) {
                    processWildcard((XSDWildcard)child, attributeGroupNode);
                }
            }
        }
        processAnnotation(defn.getAnnotation(), attributeGroupNode);
        processNonSchemaAttributes(defn, attributeGroupNode);
    }

    protected Node processWildcard( XSDWildcard wildcard,
                                    Node parentNode ) throws RepositoryException {
        if (wildcard == null) {
            return null;
        }
        logger.debug("Any Attribute");

        Node anyAttributeNode = parentNode.addNode(XsdLexicon.ANY_ATTRIBUTE, XsdLexicon.ANY_ATTRIBUTE);

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
                anyAttributeNode.setProperty(XsdLexicon.NAMESPACE, values.toArray(new String[values.size()]));
            }
        }
        if (wildcard.getProcessContents() != null) {
            XSDProcessContents processContents = wildcard.getProcessContents();
            anyAttributeNode.setProperty(XsdLexicon.PROCESS_CONTENTS, processContents.getLiteral());
        }
        processAnnotation(wildcard.getAnnotation(), anyAttributeNode);
        processNonSchemaAttributes(wildcard, anyAttributeNode);
        return anyAttributeNode;
    }

    protected void processAttributeUse( XSDAttributeUse use,
                                        Node parentNode ) throws RepositoryException {
        // Process the attribute declaration ...
        Node attributeDeclaration = processAttributeDeclaration(use.getAttributeDeclaration(), parentNode, true);
        if (use.getUse() != null) {
            attributeDeclaration.setProperty(XsdLexicon.USE, use.getUse().getLiteral());
        }
        processNonSchemaAttributes(use, attributeDeclaration);
    }

    protected void processNonSchemaAttributes( XSDConcreteComponent component,
                                               Node node ) throws RepositoryException {
        if (component == null) {
            return;
        }
        Element element = component.getElement();
        if (element == null) {
            return;
        }

        NamedNodeMap attributes = element.getAttributes();
        if (attributes == null) {
            return;
        }

        for (int i = 0, len = attributes.getLength(); i != len; ++i) {
            org.w3c.dom.Node attribute = attributes.item(i);
            if (attribute.getNodeType() != org.w3c.dom.Node.ATTRIBUTE_NODE) {
                continue;
            }
            String namespaceUri = attribute.getNamespaceURI();
            if (!XsdLexicon.Namespace.URI.equals(namespaceUri)) {
                // Record any attribute that is not in the XSD namespace ...
                String localName = attribute.getLocalName();
                String value = attribute.getNodeValue();
                if (value == null) continue;
                if (namespaceUri != null) {
                    NamespaceRegistry namespaceRegistry = node.getSession().getWorkspace().getNamespaceRegistry();
                    String prefix = registerNamespace(namespaceRegistry, namespaceUri, attribute.getPrefix());
                    String propertyName = prefix + ":" + localName;
                    node.setProperty(propertyName, value);
                } else {
                    node.setProperty(localName, value);
                }
            }
        }
    }

    protected void processAnnotation( XSDAnnotation annotation,
                                      Node node ) throws RepositoryException {
        if (annotation == null) {
            return;
        }
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
                node.setProperty(DESCRIPTION, content);
            }
        }
    }

    protected void processAnnotations( List<XSDAnnotation> annotations,
                                       Node parentNode ) throws RepositoryException {
        assert annotations != null;
        StringBuilder sb = new StringBuilder();
        for (XSDAnnotation annotation : annotations) {
            for (Object obj : annotation.getUserInformation()) {
                Element element = (Element)obj;
                if (element.getLocalName().equals("documentation")) {
                    String content = element.getTextContent();
                    if (content != null) sb.append(content);
                }
            }
            sb.append(System.getProperty("line.separator"));
        }
        if (sb.length() != 0) {
            String content = sb.toString();
            content = content.trim();
            if (content.length() != 0) {
                parentNode.setProperty(DESCRIPTION, content);
            }
        }
    }

    /**
     * Given an {@link XSDFacet}, determines the JCR property type based on the value of the facet.
     * 
     * @param facetValue a String representing the lexical value of the facet, which can be null.
     * @param defaultPropertyType a given property type, of which we expected the string value to be convertible to.
     * @return a property type to which the string value can be converted
     */
    private int determineJCRPropertyTypeForFacet( String facetValue,
                                                  int defaultPropertyType ) {
        switch (defaultPropertyType) {
            case PropertyType.LONG: {
                try {
                    Long.valueOf(facetValue);
                    return PropertyType.LONG;
                } catch (NumberFormatException e) {
                    return PropertyType.DECIMAL;
                }
            }
            default: {
                return defaultPropertyType;
            }
        }
    }

    protected void processFacet( XSDFacet facet,
                                 Node node,
                                 String propertyName,
                                 int propertyType ) throws RepositoryException {
        if (facet == null) {
            return;
        }
        String lexicalValue = facet.getLexicalValue();
        if (lexicalValue != null) {
            int actualPropertyType = determineJCRPropertyTypeForFacet(lexicalValue, propertyType);
            Value value = context.valueFactory().createValue(facet.getLexicalValue(), actualPropertyType);
            node.setProperty(propertyName, value);
        } else if (facet instanceof XSDRepeatableFacet) {
            Set<String> values = getRepeatableFacetValues((XSDRepeatableFacet)facet);
            if (!values.isEmpty()) {
                node.setProperty(propertyName, values.toArray(new String[values.size()]));
            }
        }
    }

    private Set<String> getRepeatableFacetValues( XSDRepeatableFacet facet ) {
        EList<?> facetValues = null;
        if (facet instanceof XSDPatternFacet) {
            facetValues = ((XSDPatternFacet)facet).getValue();
        } else if (facet instanceof XSDEnumerationFacet) {
            facetValues = ((XSDEnumerationFacet)facet).getValue();
        }

        Set<String> values = new HashSet<String>();
        if (facetValues != null && !facetValues.isEmpty()) {
            for (Object enumValue : facetValues) {
                values.add(enumValue.toString());
            }
        }
        return values;
    }

    protected <Facet extends XSDFacet> void processFacetsList( List<Facet> facets,
                                                               Node node,
                                                               String propertyName ) throws RepositoryException {
        if (facets == null) {
            return;
        }

        Set<String> values = new HashSet<String>();
        for (XSDFacet facet : facets) {
            String lexicalValue = facet.getLexicalValue();
            if (lexicalValue != null) {
                values.add(facet.getLexicalValue());
            } else if (facet instanceof XSDRepeatableFacet) {
                values.addAll(getRepeatableFacetValues((XSDRepeatableFacet)facet));
            }
        }

        if (!values.isEmpty()) {
            node.setProperty(propertyName, values.toArray(new String[values.size()]));
        }
    }

    protected <Enumerator extends AbstractEnumerator> void processEnumerators( List<Enumerator> enumerators,
                                                                               Node node,
                                                                               String propertyName ) throws RepositoryException {
        if (enumerators == null) {
            return;
        }
        Set<String> values = new HashSet<String>();
        for (Enumerator enumValue : enumerators) {
            String value = enumValue.getLiteral();
            if (value != null) {
                values.add(value);
            }
        }
        if (!values.isEmpty()) {
            node.setProperty(propertyName, values.toArray(new String[values.size()]));
        }
    }

    protected <Enumerator extends AbstractEnumerator> void processEnumerator( Enumerator enumerator,
                                                                              Node node,
                                                                              String propertyName ) throws RepositoryException {
        if (enumerator != null && enumerator.getLiteral() != null) {
            node.setProperty(propertyName, enumerator.getLiteral());
        }
    }
}
