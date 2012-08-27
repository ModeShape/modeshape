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
package org.modeshape.sequencer.teiid.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import javax.jcr.NamespaceRegistry;
import javax.xml.stream.XMLStreamReader;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.TransformLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.xmi.XmiAttribute;
import org.modeshape.sequencer.teiid.xmi.XmiBasePart;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.modeshape.sequencer.teiid.xmi.XmiReader;

/**
 * Reader of XMI relational models to support the CND definitions.
 */
class ModelReader extends XmiReader implements Comparable<ModelReader> {

    private static final long DEFAULT_MAX_SET_SIZE = 100;
    private static final boolean DEFAULT_SUPPORTS_DISTINCT = true;
    private static final boolean DEFAULT_SUPPORTS_JOIN = true;
    private static final boolean DEFAULT_SUPPORTS_ORDER_BY = true;
    private static final boolean DEFAULT_SUPPORTS_OUTER_JOIN = true;
    private static final boolean DEFAULT_SUPPORTS_WHERE_ALL = true;
    private static final boolean DEFAULT_VISIBLE = true;
    private static final String MM_HREF_PREFIX = "mmuuid/";
    private static final String MM_UUID_PREFIX = "mmuuid:";

    private final NamespaceRegistry registry; // never null
    private final ReferenceResolver resolver;

    /**
     * @param path the resource path including the name (cannot be <code>null</code> or empty)
     * @param resolver the reference resolver (cannot be <code>null</code>)
     * @param registry the namespace registry being used by the sequencer (cannot be <code>null</code>)
     */
    public ModelReader( final String path,
                        final ReferenceResolver resolver,
                        final NamespaceRegistry registry ) {
        super(path);
        CheckArg.isNotNull(resolver, "resolver");
        this.registry = registry;
        this.resolver = resolver;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#addAttribute(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      org.modeshape.sequencer.teiid.xmi.XmiAttribute)
     */
    @Override
    protected void addAttribute( final XmiElement element,
                                 final XmiAttribute newAttribute ) {
        // make sure attribute prefix is valid before adding to element
        ensureNamespacePrefixIsValid(newAttribute);
        super.addAttribute(element, newAttribute); // set the parent

        if (XmiLexicon.ModelIds.UUID.equals(newAttribute.getName())
            && XmiLexicon.Namespace.URI.equals(newAttribute.getNamespaceUri())) {
            final String value = newAttribute.getValue();

            if (!StringUtil.isBlank(value) && value.startsWith(MM_UUID_PREFIX)) {
                newAttribute.setValue(value.substring(MM_UUID_PREFIX.length()));
            }
        } else if (CoreLexicon.ModelIds.ANNOTATED_OBJECT.equals(newAttribute.getName())
                   && CoreLexicon.Namespace.URI.equals(newAttribute.getNamespaceUri())) {
            final String value = newAttribute.getValue();

            if (!StringUtil.isBlank(value) && value.startsWith(MM_HREF_PREFIX)) {
                newAttribute.setValue(value.substring(MM_HREF_PREFIX.length()));
            }
        } else if ((TransformLexicon.ModelIds.OUTPUTS.equals(newAttribute.getName()) || TransformLexicon.ModelIds.TARGET.equals(newAttribute.getName()))
                   && TransformLexicon.Namespace.URI.equals(newAttribute.getNamespaceUri())) {
            final String value = newAttribute.getValue();

            if (!StringUtil.isBlank(value) && value.startsWith(MM_HREF_PREFIX)) {
                newAttribute.setValue(value.substring(MM_HREF_PREFIX.length()));
            }
        } else if (TransformLexicon.ModelIds.HREF.equals(newAttribute.getName())
                   && TransformLexicon.Namespace.URI.equals(newAttribute.getNamespaceUri())) {
            final String value = newAttribute.getValue();

            if (!StringUtil.isBlank(value) && (value.indexOf(MM_HREF_PREFIX) != -1)) {
                newAttribute.setValue(value.substring(value.indexOf(MM_HREF_PREFIX) + 1));
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#addChild(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    protected void addChild( final XmiElement parent,
                             final XmiElement newChild ) {
        super.addChild(parent, newChild);

        if (!StringUtil.isBlank(newChild.getUuid())) {
            this.resolver.recordXmiUuid(newChild.getUuid(), newChild);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#addElement(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    protected void addElement( final XmiElement newElement ) {
        // make sure prefix is valid before adding to parent
        ensureNamespacePrefixIsValid(newElement);
        super.addElement(newElement);

        if (!StringUtil.isBlank(newElement.getUuid())) {
            this.resolver.recordXmiUuid(newElement.getUuid(), newElement);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final ModelReader that ) {
        if (that == null) {
            return 1;
        }
        if (that == this) {
            return 0;
        }

        if (getModelImports().contains(that.getPath())) {
            // this model imports that, so this model is greater than that ...
            return 1;
        }

        if (that.getModelImports().contains(getPath())) {
            // that model imports this, so this model is less than that ...
            return -1;
        }

        // Otherwise, neither model depends upon each other, so base the order upon the number of models ...
        return this.getModelImports().size() - that.getModelImports().size();
    }

    private void ensureNamespacePrefixIsValid( final XmiBasePart xmiPart ) {
        assert (xmiPart != null);

        // make sure XMI part prefix is the same as the registered prefix for the namespace URI
        final String nsUri = xmiPart.getNamespaceUri();

        if (!StringUtil.isBlank(nsUri)) {
            try {
                final String registeredPrefix = this.registry.getPrefix(nsUri);

                if (!registeredPrefix.equals(xmiPart.getNamespacePrefix())) {
                    xmiPart.setNamespacePrefix(registeredPrefix);
                }
            } catch (final Exception e) {
                // TODO log and remove stacktrace
                e.printStackTrace();
            }
        }
    }

    /**
     * @return the model description or <code>null</code> if not found
     */
    public String getDescription() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.DESCRIPTION, CoreLexicon.Namespace.URI);
    }

    private XmiElement getElement( final String name,
                                   final String namespaceUri ) {
        for (final XmiElement element : getElements()) {
            if (name.equals(element.getName()) && namespaceUri.equals(element.getNamespaceUri())) {
                return element;
            }
        }

        return null;
    }

    /**
     * @return the max set size or <code>100</code> if not found
     */
    public long getMaxSetSize() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_MAX_SET_SIZE;
        }

        final String maxSetSize = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.MAX_SET_SIZE, CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(maxSetSize)) {
            return DEFAULT_MAX_SET_SIZE;
        }

        return Long.parseLong(maxSetSize);
    }

    /**
     * @return the model annotation XMI element or <code>null</code> if not found
     */
    private XmiElement getModelAnnotation() {
        return getElement(CoreLexicon.ModelIds.MODEL_ANNOTATION, CoreLexicon.Namespace.URI);
    }

    public List<XmiElement> getModelImports() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        final List<XmiElement> imports = new ArrayList<XmiElement>();

        for (final XmiElement kid : modelAnnotation.getChildren()) {
            if (CoreLexicon.ModelIds.MODEL_IMPORT.equals(kid.getName())
                && CoreLexicon.Namespace.URI.equals(kid.getNamespaceUri())) {
                imports.add(kid);
            }
        }

        return imports;
    }

    /**
     * @return the model type or <code>null</code> if not found
     */
    public String getModelType() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.MODEL_TYPE, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the model name in source or <code>null</code> if not found
     */
    public String getNameInSource() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.NAME_IN_SOURCE, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the primary metamodel URI or <code>null</code> if not found
     */
    public String getPrimaryMetamodelUri() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.PRIMARY_METAMODEL_URI, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the producer name or <code>null</code> if not found
     */
    public String getProducerName() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.PRODUCER_NAME, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the producer version or <code>null</code> if not found
     */
    public String getProducerVersion() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.PRODUCER_VERSION, CoreLexicon.Namespace.URI);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#handleEndElement(javax.xml.stream.XMLStreamReader)
     */
    @Override
    protected XmiElement handleEndElement( final XMLStreamReader streamReader ) {
        final XmiElement endElement = super.handleEndElement(streamReader);

        if (XMI_TAG.equals(streamReader.getLocalName())) {
            stop();
        }

        return endElement;
    }

    /**
     * @return <code>true</code> if model is visible (defaults to {@value ModelReader#DEFAULT_VISIBLE} )
     */
    public boolean isVisible() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_VISIBLE;
        }

        final String visible = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.VISIBLE, CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(visible)) {
            return DEFAULT_VISIBLE;
        }

        return Boolean.parseBoolean(visible);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#pop(javax.xml.stream.XMLStreamReader)
     */
    @Override
    protected XmiElement pop( final XMLStreamReader streamReader ) {
        // ignore XMI tag
        if (!XmiReader.XMI_TAG.equals(streamReader.getLocalName()) || (getStackSize() != 0)) {
            return super.pop(streamReader);
        }

        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#push(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    protected void push( final XmiElement element ) {
        // ignore XMI tag
        if (!XmiReader.XMI_TAG.equals(element.getName()) || (getStackSize() != 0)) {
            super.push(element);
        }
    }

    public void readModel( final InputStream stream ) throws Exception {
        final List<XmiElement> elements = super.read(stream);

        if (DEBUG) {
            for (final XmiElement element : elements) {
                System.err.println("====model element=" + element.getName());
            }

            debug("");

            // TODO change this to only print unresolved
            for (final Entry<String, XmiElement> uuidMapping : this.resolver.getUuidMappings().entrySet()) {
                System.err.println(uuidMapping.getKey() + '=' + uuidMapping.getValue());
            }

            debug("");
        }

        if (DEBUG) {
            System.err.println();

            for (final Entry<String, XmiElement> uuidMapping : this.resolver.getUuidMappings().entrySet()) {
                System.err.println(uuidMapping.getKey() + '=' + uuidMapping.getValue());
            }

            System.err.println();
        }
    }

    /**
     * @return <code>true</code> if model supports distinct (defaults to {@value ModelReader#DEFAULT_SUPPORTS_DISTINCT} )
     */
    public boolean supportsDistinct() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_SUPPORTS_DISTINCT;
        }

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.SUPPORTS_DISTINCT,
                                                                  CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(supports)) {
            return DEFAULT_SUPPORTS_DISTINCT;
        }

        return Boolean.parseBoolean(supports);
    }

    /**
     * @return <code>true</code> if model supports joins (defaults to {@value ModelReader#DEFAULT_SUPPORTS_JOIN} )
     */
    public boolean supportsJoin() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_SUPPORTS_JOIN;
        }

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.SUPPORTS_JOIN, CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(supports)) {
            return DEFAULT_SUPPORTS_JOIN;
        }

        return Boolean.parseBoolean(supports);
    }

    /**
     * @return <code>true</code> if model supports order by (defaults to {@value ModelReader#DEFAULT_SUPPORTS_ORDER_BY} )
     */
    public boolean supportsOrderBy() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_SUPPORTS_ORDER_BY;
        }

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.SUPPORTS_ORDER_BY,
                                                                  CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(supports)) {
            return DEFAULT_SUPPORTS_ORDER_BY;
        }

        return Boolean.parseBoolean(supports);
    }

    /**
     * @return <code>true</code> if model supports outer joins (defaults to {@value ModelReader#DEFAULT_SUPPORTS_OUTER_JOIN} )
     */
    public boolean supportsOuterJoin() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_SUPPORTS_OUTER_JOIN;
        }

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.SUPPORTS_OUTER_JOIN,
                                                                  CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(supports)) {
            return DEFAULT_SUPPORTS_OUTER_JOIN;
        }

        return Boolean.parseBoolean(supports);
    }

    /**
     * @return <code>true</code> if model supports where all (defaults to {@value ModelReader#DEFAULT_SUPPORTS_WHERE_ALL} )
     */
    public boolean supportsWhereAll() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_SUPPORTS_WHERE_ALL;
        }

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelIds.SUPPORTS_WHERE_ALL,
                                                                  CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(supports)) {
            return DEFAULT_SUPPORTS_WHERE_ALL;
        }

        return Boolean.parseBoolean(supports);
    }
}
