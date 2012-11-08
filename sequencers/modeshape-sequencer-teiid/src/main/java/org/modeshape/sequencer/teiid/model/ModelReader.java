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

import javax.jcr.NamespaceRegistry;
import javax.xml.stream.XMLStreamReader;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.TeiidI18n;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.xmi.XmiAttribute;
import org.modeshape.sequencer.teiid.xmi.XmiBasePart;
import org.modeshape.sequencer.teiid.xmi.XmiElement;
import org.modeshape.sequencer.teiid.xmi.XmiReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

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

    private static final Logger LOGGER = Logger.getLogger(ModelReader.class);

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
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#addAttribute(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      org.modeshape.sequencer.teiid.xmi.XmiAttribute)
     */
    @Override
    protected void addAttribute( final XmiElement element,
                                 final XmiAttribute newAttribute ) {
        // make sure attribute prefix is valid before adding to element
        ensureNamespacePrefixIsValid(newAttribute);
        super.addAttribute(element, newAttribute); // set the parent

        if (XmiLexicon.ModelId.UUID.equals(newAttribute.getName())) {
            final String value = newAttribute.getValue();
            
            // strip off prefix
            if (!StringUtil.isBlank(value) && value.startsWith(CoreLexicon.ModelId.MM_UUID_PREFIX)) {
                String uuid = value.substring(CoreLexicon.ModelId.MM_UUID_PREFIX.length());
                newAttribute.setValue(uuid);

                if (!XmiLexicon.Namespace.URI.equals(newAttribute.getNamespaceUri())) {
                    if (this.resolver.getNode(uuid) == null) {
                        try {
                            this.resolver.addUnresolvedReference(uuid);
                        } catch (Exception e) {
                            // should not happen as making sure node does not exist
                        }
                    }
                }
            }
        }
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#addChild(org.modeshape.sequencer.teiid.xmi.XmiElement,
     *      org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    protected void addChild( final XmiElement parent,
                             final XmiElement newChild ) {
        super.addChild(parent, newChild);

        if (!StringUtil.isBlank(newChild.getUuid())) {
            this.resolver.record(newChild.getUuid(), newChild);
        }
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#addElement(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    protected void addElement( final XmiElement newElement ) {
        // make sure prefix is valid before adding to parent
        ensureNamespacePrefixIsValid(newElement);
        super.addElement(newElement);

        if (!StringUtil.isBlank(newElement.getUuid())) {
            this.resolver.record(newElement.getUuid(), newElement);
        }
    }

    /**
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

        String thatPath = that.getPath();

        for (XmiElement modelImport : getModelImports()) {
            if (thatPath.equals(modelImport.getAttributeValue(CoreLexicon.ModelId.NAME, CoreLexicon.Namespace.URI))) {
                return -1;
            }
        }

        String thisPath = getPath();

        for (XmiElement modelImport : that.getModelImports()) {
            if (thisPath.equals(modelImport.getAttributeValue(CoreLexicon.ModelId.NAME, CoreLexicon.Namespace.URI))) {
                return -1;
            }
        }

        // Otherwise, neither model depends upon each other, so base the order upon the number of models ...
        return this.getModelImports().size() - that.getModelImports().size();
    }

    private void ensureNamespacePrefixIsValid( final XmiBasePart xmiPart ) {
        assert (xmiPart != null);

        // models may have a namespace prefix that does not match the one registered in the NamespaceRegistry.
        // so make sure the XMI part prefix is the same as the registered prefix for the namespace URI.
        final String nsUri = xmiPart.getNamespaceUri();

        if (!StringUtil.isBlank(nsUri)) {
            try {
                final String registeredPrefix = this.registry.getPrefix(nsUri);

                if (!registeredPrefix.equals(xmiPart.getNamespacePrefix())) {
                    xmiPart.setNamespacePrefix(registeredPrefix);
                }
            } catch (final Exception e) {
                LOGGER.error(e, TeiidI18n.namespaceUriNotFoundInRegistry, nsUri, getPath());
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

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelId.DESCRIPTION, CoreLexicon.Namespace.URI);
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

        final String maxSetSize = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.MAX_SET_SIZE,
                                                                    CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(maxSetSize)) {
            return DEFAULT_MAX_SET_SIZE;
        }

        return Long.parseLong(maxSetSize);
    }

    /**
     * @return the model annotation XMI element or <code>null</code> if not found
     */
    private XmiElement getModelAnnotation() {
        return getElement(CoreLexicon.ModelId.MODEL_ANNOTATION, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the model imports XMI elements or <code>null</code> if none found
     */
    public List<XmiElement> getModelImports() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        final List<XmiElement> imports = new ArrayList<XmiElement>();

        for (final XmiElement kid : modelAnnotation.getChildren()) {
            if (CoreLexicon.ModelId.MODEL_IMPORT.equals(kid.getName()) && CoreLexicon.Namespace.URI.equals(
                    kid.getNamespaceUri())) {
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

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelId.MODEL_TYPE, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the model name in source or <code>null</code> if not found
     */
    public String getNameInSource() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelId.NAME_IN_SOURCE, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the primary metamodel URI or <code>null</code> if not found
     */
    public String getPrimaryMetamodelUri() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelId.PRIMARY_METAMODEL_URI, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the producer name or <code>null</code> if not found
     */
    public String getProducerName() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelId.PRODUCER_NAME, CoreLexicon.Namespace.URI);
    }

    /**
     * @return the producer version or <code>null</code> if not found
     */
    public String getProducerVersion() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return null;
        }

        return modelAnnotation.getAttributeValue(CoreLexicon.ModelId.PRODUCER_VERSION, CoreLexicon.Namespace.URI);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#handleEndElement(javax.xml.stream.XMLStreamReader)
     */
    @Override
    protected XmiElement handleEndElement( final XMLStreamReader streamReader ) {
        final XmiElement endElement = super.handleEndElement(streamReader);

        // stop if XMI tag or if ModelAnnotation tag short circuit reading if model won't be sequenced
        if (XmiLexicon.ModelId.XMI_TAG.equals(streamReader.getLocalName())
                || (CoreLexicon.ModelId.MODEL_ANNOTATION.equals(endElement.getName()) && !ModelSequencer.shouldSequence(
                this))) {
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

        final String visible = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.VISIBLE, CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(visible)) {
            return DEFAULT_VISIBLE;
        }

        return Boolean.parseBoolean(visible);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#pop(javax.xml.stream.XMLStreamReader)
     */
    @Override
    protected XmiElement pop( final XMLStreamReader streamReader ) {
        // ignore XMI tag
        if (!XmiLexicon.ModelId.XMI_TAG.equals(streamReader.getLocalName()) || (getStackSize() != 0)) {
            return super.pop(streamReader);
        }

        return null;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiReader#push(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    protected void push( final XmiElement element ) {
        // ignore XMI tag
        if (!XmiLexicon.ModelId.XMI_TAG.equals(element.getName()) || (getStackSize() != 0)) {
            super.push(element);
        }
    }

    /**
     * @param stream the input stream of the XMI model being read (cannot be <code>null</code>)
     * @throws Exception if there is a problem reading the input stream
     */
    public void readModel( final InputStream stream ) throws Exception {
        CheckArg.isNotNull(stream, "stream");

        final long startTime = System.currentTimeMillis();
        final List<XmiElement> elements = super.read(stream);

        for (final XmiElement element : elements) {
            debug("====root model element=" + element.getName());
        }

        debug("");

        for (final Entry<String, XmiElement> uuidMapping : this.resolver.getUuidMappings().entrySet()) {
            debug(uuidMapping.getKey() + '=' + uuidMapping.getValue());
        }

        debug("");

        for (String uuid : this.resolver.getUnresolved().keySet()) {
            debug("**** unresolved " + uuid);
        }

        debug("\n\nmodel read time=" + (System.currentTimeMillis() - startTime));
    }

    /**
     * @return <code>true</code> if model supports distinct (defaults to {@value ModelReader#DEFAULT_SUPPORTS_DISTINCT} )
     */
    public boolean supportsDistinct() {
        final XmiElement modelAnnotation = getModelAnnotation();

        if (modelAnnotation == null) {
            return DEFAULT_SUPPORTS_DISTINCT;
        }

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.SUPPORTS_DISTINCT,
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

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.SUPPORTS_JOIN, CoreLexicon.Namespace.URI);

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

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.SUPPORTS_ORDER_BY,
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

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.SUPPORTS_OUTER_JOIN,
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

        final String supports = modelAnnotation.getAttributeValue(CoreLexicon.ModelId.SUPPORTS_WHERE_ALL,
                                                                  CoreLexicon.Namespace.URI);

        if (StringUtil.isBlank(supports)) {
            return DEFAULT_SUPPORTS_WHERE_ALL;
        }

        return Boolean.parseBoolean(supports);
    }
}
