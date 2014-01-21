/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.teiid.xmi;

import java.util.ArrayList;
import java.util.List;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;

/**
 * An element from an XMI file.
 */
public class XmiElement extends XmiBasePart implements XmiDescendent {

    /**
     * The name of the attribute used as the name attribute. Defaults to {@value} .
     */
    public static final String NAME_ATTR_NAME = "name";

    private final List<XmiAttribute> attributes = new ArrayList<XmiAttribute>();
    private final List<XmiElement> children = new ArrayList<XmiElement>();
    private XmiElement parent;

    /**
     * @param name the element name (cannot be <code>null</code> or empty)
     */
    public XmiElement( final String name ) {
        super(name);
    }

    /**
     * @param newAttribute the attribute being added (cannot be <code>null</code>)
     */
    public void addAttribute( final XmiAttribute newAttribute ) {
        CheckArg.isNotNull(newAttribute, "newAttribute");
        this.attributes.add(newAttribute);
        newAttribute.setParent(this);
    }

    /**
     * @param newChild the child element being added (cannot be <code>null</code>)
     */
    public void addChild( final XmiElement newChild ) {
        CheckArg.isNotNull(newChild, "newChild");
        this.children.add(newChild);
        newChild.setParent(this);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#equals(java.lang.Object)
     */
    @Override
    public boolean equals( final Object obj ) {
        if (!super.equals(obj)) {
            return false;
        }

        if (!(obj instanceof XmiElement)) {
            return false;
        }

        final XmiElement that = (XmiElement)obj;

        // compare attributes
        if (!this.attributes.containsAll(that.attributes) || !that.attributes.containsAll(this.attributes)) {
            return false;
        }

        // compare kids
        if (!this.children.containsAll(that.children) || !that.children.containsAll(this.children)) {
            return false;
        }

        // compare parent
        return (this.parent == that.parent);
    }

    /**
     * @param childName the name of the child being requested (cannot be <code>null</code> or empty)
     * @param namespaceUri the URI of the attribute being requested (can be <code>null</code> or empty)
     * @return the requested child element or <code>null</code> if not found
     */
    public XmiElement findChild( final String childName,
                                 final String namespaceUri ) {
        CheckArg.isNotEmpty(childName, "childName");

        for (final XmiElement kid : getChildren()) {
            if (childName.equals(kid.getName())) {
                // requested element should not have a namespace URI
                if (StringUtil.isBlank(namespaceUri)) {
                    if (StringUtil.isBlank(kid.getNamespaceUri())) {
                        return kid;
                    }

                    continue;
                }

                // requested attribute should have a matching namespace URI
                if (namespaceUri.equals(kid.getNamespaceUri())) {
                    return kid;
                }
            }
        }

        // not found
        return null;
    }

    /**
     * @param name the name of the attribute being requested (cannot be <code>null</code> or empty)
     * @param namespaceUri the URI of the attribute being requested (can be <code>null</code> or empty)
     * @return the requested attribute or <code>null</code> if not found
     */
    public XmiAttribute getAttribute( final String name,
                                      final String namespaceUri ) {
        CheckArg.isNotEmpty(name, "attributeName");

        for (final XmiAttribute attribute : getAttributes()) {
            // check names
            if (attribute.getName().equals(name)) {
                // requested attribute should not have a namespace URI
                if (StringUtil.isBlank(namespaceUri)) {
                    if (StringUtil.isBlank(attribute.getNamespaceUri())) {
                        return attribute;
                    }

                    continue;
                }

                // requested attribute should have a matching namespace URI
                if (namespaceUri.equals(attribute.getNamespaceUri())) {
                    return attribute;
                }
            }
        }

        // not found
        return null;
    }

    /**
     * @return the attributes (never <code>null</code> but can be empty)
     */
    public List<XmiAttribute> getAttributes() {
        return this.attributes;
    }

    /**
     * @param namespaceUri the namespace URI of the attributes being requested (can be <code>null</code> or empty)
     * @return the requested attributes (never <code>null</code> but can be empty)
     */
    public List<XmiAttribute> getAttributes( final String namespaceUri ) {
        CheckArg.isNotNull(namespaceUri, "namespaceUri");
        final List<XmiAttribute> matches = new ArrayList<XmiAttribute>();

        for (final XmiAttribute attribute : getAttributes()) {
            // requested attributes should not have a namespace URI
            if (StringUtil.isBlank(namespaceUri)) {
                if (StringUtil.isBlank(attribute.getNamespaceUri())) {
                    matches.add(attribute);
                }

                continue;
            }

            // requested attributes should have a matching namespace URI
            if (namespaceUri.equals(attribute.getNamespaceUri())) {
                matches.add(attribute);
            }
        }

        return matches;
    }

    /**
     * @param name the name of the attribute whose value is being requested (cannot be <code>null</code> or empty)
     * @param namespaceUri the namespace URI of the attribute being requested (can be <code>null</code> or empty)
     * @return the requested attribute value (which can be <code>null</code> or empty) or <code>null</code> if not found
     */
    public String getAttributeValue( final String name,
                                     final String namespaceUri ) {
        CheckArg.isNotNull(name, "name");
        final XmiAttribute attribute = getAttribute(name, namespaceUri);

        if (attribute != null) {
            return attribute.getValue();
        }

        return null;
    }

    /**
     * @return the child elements (never <code>null</code> but can be empty)
     */
    public List<XmiElement> getChildren() {
        return this.children;
    }

    /**
     * @param namespaceUri the namespace URI of the name attribute being requested (can be <code>null</code> or empty)
     * @return the name attribute or <code>null</code> if not found
     */
    public XmiAttribute getNameAttribute( final String namespaceUri ) {
        return getAttribute(NAME_ATTR_NAME, namespaceUri);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#getNamespacePrefix()
     */
    @Override
    public String getNamespacePrefix() {
        final String prefix = super.getNamespacePrefix();

        if (StringUtil.isBlank(prefix)) {
            final XmiElement parent = getParent();

            if (parent != null) {
                return parent.getNamespacePrefix();
            }
        }

        return prefix;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#getNamespaceUri()
     */
    @Override
    public String getNamespaceUri() {
        final String uri = super.getNamespaceUri();

        if (StringUtil.isBlank(uri)) {
            final XmiElement parent = getParent();

            if (parent != null) {
                return parent.getNamespaceUri();
            }
        }

        return uri;
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiDescendent#getParent()
     */
    @Override
    public XmiElement getParent() {
        return this.parent;
    }

    /**
     * @return the XMI UUID or <code>null</code> if not set
     */
    public String getUuid() {
        return getAttributeValue(XmiLexicon.ModelId.UUID, XmiLexicon.Namespace.URI);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiBasePart#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(super.hashCode(), this.parent, this.attributes, this.children);
    }

    /**
     * @see org.modeshape.sequencer.teiid.xmi.XmiDescendent#setParent(org.modeshape.sequencer.teiid.xmi.XmiElement)
     */
    @Override
    public void setParent( final XmiElement parent ) {
        this.parent = parent;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        if (StringUtil.isBlank(getNamespaceUri())) {
            return getName();
        }

        if (!StringUtil.isBlank(getNamespacePrefix())) {
            return getNamespacePrefix() + ':' + getName();
        }

        return '{' + getNamespaceUri() + '}' + getName();
    }
}
