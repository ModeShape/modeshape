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
package org.modeshape.jdbc.rest;

import static org.modeshape.jdbc.rest.JSONHelper.valueFrom;
import static org.modeshape.jdbc.rest.JSONHelper.valuesFrom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.Value;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeTypeIterator;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.modeshape.common.annotation.Immutable;


/**
 * Implementation of {@link javax.jcr.nodetype.NodeType} for the ModeShape client.
 */
@Immutable
public class NodeType implements javax.jcr.nodetype.NodeType {

    private final String name;
    private final boolean isAbstract;
    private final boolean isMixin;
    private final boolean isQueryable;
    private final boolean hasOrderableChildNodes;
    private final String primaryItemName;
    private final Map<PropertyDefinition.Id, PropertyDefinition> propertyDefinitions;
    private final Map<ChildNodeDefinition.Id, ChildNodeDefinition> childNodeDefinitions;
    private final List<String> declaredSuperTypes;
    private final NodeTypes nodeTypes;
    private List<NodeType> allSuperTypes;
    private Set<String> allSuperTypeNames;
    private Map<PropertyDefinition.Id, PropertyDefinition> allPropertyDefinitions;
    private Map<ChildNodeDefinition.Id, ChildNodeDefinition> allChildNodeDefinitions;

    @SuppressWarnings("unchecked")
    protected NodeType(JSONObject json, NodeTypes nodeTypes) {
        this.nodeTypes = nodeTypes;

        this.name = valueFrom(json, "jcr:nodeTypeName");
        assert this.name != null;
        this.isMixin = valueFrom(json, "jcr:isMixin", false);
        this.isAbstract = valueFrom(json, "jcr:isAbstract", false);
        this.hasOrderableChildNodes = valueFrom(json, "jcr:hasOrderableChildNodes", false);
        this.isQueryable = valueFrom(json, "jcr:isQueryable", false);
        this.primaryItemName = valueFrom(json, "jcr:primaryItemName");
        this.declaredSuperTypes = valuesFrom(json, "jcr:supertypes");

        this.propertyDefinitions = new HashMap<>();
        this.childNodeDefinitions = new HashMap<>();

        // Process the children (the property definition and child node definition objects) ...
        if (json.has("children")) {
            try {
                JSONObject children = json.getJSONObject("children");
                for (Iterator<String> itr = children.keys(); itr.hasNext(); ) {
                    String key = itr.next();
                    JSONObject child = children.getJSONObject(key);
                    if (child != null) {
                        // Get the primary type of this child object ...
                        String type = getPrimaryType(key, child);
                        if (type.startsWith("nt:propertyDefinition") || type.startsWith("jcr:propertyDefinition")) {
                            PropertyDefinition defn = new PropertyDefinition(name, child, this.nodeTypes);
                            propertyDefinitions.put(defn.id(), defn);
                        } else if (type.startsWith("nt:childNodeDefinition") || type.startsWith(
                                "jcr:childNodeDefinition")) {
                            ChildNodeDefinition defn = new ChildNodeDefinition(name, child, this.nodeTypes);
                            childNodeDefinitions.put(defn.id(), defn);
                        }
                    }
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public javax.jcr.nodetype.NodeType[] getDeclaredSupertypes() {
        return nodeTypes.toNodeTypes(declaredSuperTypes);
    }

    @Override
    public javax.jcr.nodetype.NodeType[] getSupertypes() {
        List<NodeType> allSuperTypes = allSuperTypes();
        return allSuperTypes.toArray(new javax.jcr.nodetype.NodeType[allSuperTypes.size()]);
    }

    @Override
    public String[] getDeclaredSupertypeNames() {
        return declaredSuperTypes.toArray(new String[declaredSuperTypes.size()]);
    }

    @Override
    public NodeTypeIterator getDeclaredSubtypes() {
        List<NodeType> results = new ArrayList<>();
        for (NodeType nodeType : nodeTypes.nodeTypes()) {
            if (nodeType == this) continue;
            if (nodeType.declaredSuperTypes.contains(name)) {
                results.add(nodeType);
            }
        }
        return iterator(results);
    }

    @Override
    public NodeTypeIterator getSubtypes() {
        List<NodeType> results = new ArrayList<>();
        for (NodeType nodeType : nodeTypes.nodeTypes()) {
            if (nodeType == this) continue;
            if (nodeType.allSuperTypeNames().contains(name)) {
                results.add(nodeType);
            }
        }
        return iterator(results);
    }

    protected List<NodeType> allSuperTypes() {
        if (this.allSuperTypes == null) {
            List<NodeType> allSuperTypes = new ArrayList<>();
            Set<String> allSuperTypeNames = new HashSet<>();
            for (String superTypeName : declaredSuperTypes) {
                NodeType superType = nodeTypes.getNodeType(superTypeName);
                if (superType != null) {
                    allSuperTypes.add(superType);
                    allSuperTypeNames.add(superType.getName());
                    // Add all of the supertypes ...
                    allSuperTypes.addAll(superType.allSuperTypes());
                    allSuperTypeNames.addAll(superType.allSuperTypeNames());
                }
            }
            if (allSuperTypes.isEmpty() && !isMixin) {
                // All non-mixin node types ultimately extend 'nt:base' ...
                NodeType ntBase = nodeTypes.getNodeType("nt:base");
                if (ntBase != null) {
                    allSuperTypes.add(ntBase);
                    allSuperTypeNames.add(ntBase.getName());
                }
            }
            this.allSuperTypes = allSuperTypes;
            this.allSuperTypeNames = allSuperTypeNames;
        }
        return this.allSuperTypes;
    }

    protected Set<String> allSuperTypeNames() {
        allSuperTypes();
        return allSuperTypeNames;
    }

    @Override
    public NodeDefinition[] getDeclaredChildNodeDefinitions() {
        return childNodeDefinitions.values().toArray(new NodeDefinition[childNodeDefinitions.size()]);
    }

    @Override
    public NodeDefinition[] getChildNodeDefinitions() {
        Collection<ChildNodeDefinition> allDefns = allChildNodeDefinitions();
        return allDefns.toArray(new NodeDefinition[allDefns.size()]);
    }

    protected Collection<ChildNodeDefinition> declaredChildNodeDefinitions() {
        return childNodeDefinitions.values();
    }

    protected Collection<ChildNodeDefinition> allChildNodeDefinitions() {
        if (this.allChildNodeDefinitions == null) {
            Map<ChildNodeDefinition.Id, ChildNodeDefinition> allDefns = new HashMap<>();
            // Add the declared child node definitions for this node ...
            allDefns.putAll(childNodeDefinitions);
            for (NodeType superType : allSuperTypes()) {
                for (ChildNodeDefinition childDefn : superType.declaredChildNodeDefinitions()) {
                    if (!allDefns.containsKey(childDefn.id())) {
                        allDefns.put(childDefn.id(), childDefn);
                    }
                }
            }
            this.allChildNodeDefinitions = allDefns;
        }
        return this.allChildNodeDefinitions.values();
    }

    @Override
    public javax.jcr.nodetype.PropertyDefinition[] getDeclaredPropertyDefinitions() {
        return propertyDefinitions.values().toArray(new javax.jcr.nodetype.PropertyDefinition[propertyDefinitions.size()]);
    }


    @Override
    public javax.jcr.nodetype.PropertyDefinition[] getPropertyDefinitions() {
        Collection<PropertyDefinition> allDefns = allPropertyDefinitions();
        return allDefns.toArray(new javax.jcr.nodetype.PropertyDefinition[allDefns.size()]);
    }

    protected Collection<PropertyDefinition> declaredPropertyDefinitions() {
        return propertyDefinitions.values();
    }

    protected Collection<PropertyDefinition> allPropertyDefinitions() {
        if (this.allPropertyDefinitions == null) {
            Map<PropertyDefinition.Id, PropertyDefinition> allDefns = new HashMap<>();
            // Add the declared child node definitions for this node ...
            allDefns.putAll(propertyDefinitions);
            for (NodeType superType : allSuperTypes()) {
                for (PropertyDefinition propDefn : superType.declaredPropertyDefinitions()) {
                    if (!allDefns.containsKey(propDefn.id())) {
                        allDefns.put(propDefn.id(), propDefn);
                    }
                }
            }
            this.allPropertyDefinitions = allDefns;
        }
        return this.allPropertyDefinitions.values();
    }

    protected String getPrimaryType( String key,
                                     JSONObject child ) {
        // older versions used to have "jcr:propertyDefinition" or "jcr:childNodeDefinition" as key
        try {
            String primaryType = child.getString("jcr:primaryType");
            return primaryType != null ? primaryType : key;
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getPrimaryItemName() {
        return primaryItemName;
    }

    @Override
    public boolean hasOrderableChildNodes() {
        return hasOrderableChildNodes;
    }

    @Override
    public boolean isAbstract() {
        return isAbstract;
    }

    @Override
    public boolean isMixin() {
        return isMixin;
    }

    @Override
    public boolean isQueryable() {
        return isQueryable;
    }

    @Override
    public boolean isNodeType( String nodeTypeName ) {
        if (nodeTypeName == null) return false;
        if (this.name.equals(nodeTypeName)) return true;
        return allSuperTypeNames().contains(nodeTypeName);
    }

    @Override
    public boolean canAddChildNode( String childNodeName ) {
        return false;
    }

    @Override
    public boolean canAddChildNode( String childNodeName,
                                    String nodeTypeName ) {
        return false;
    }

    @Override
    public boolean canRemoveItem( String itemName ) {
        return false;
    }

    @Override
    public boolean canRemoveNode( String nodeName ) {
        return false;
    }

    @Override
    public boolean canRemoveProperty( String propertyName ) {
        return false;
    }

    @Override
    public boolean canSetProperty( String propertyName,
                                   Value value ) {
        return false;
    }

    @Override
    public boolean canSetProperty( String propertyName,
                                   Value[] values ) {
        return false;
    }
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj instanceof NodeType) {
            return this.name.equals(((NodeType) obj).name);
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(name);
        sb.append(']');
        if (getDeclaredSupertypeNames().length != 0) {
            sb.append(" > ");
            boolean first = true;
            for (String typeName : getDeclaredSupertypeNames()) {
                if (typeName == null) continue;
                if (first) first = false;
                else sb.append(',');
                sb.append(typeName);
            }
        }
        if (isAbstract()) sb.append(" abstract");
        if (isMixin()) sb.append(" mixin");
        if (!isQueryable()) sb.append(" noquery");
        if (hasOrderableChildNodes()) sb.append(" orderable");
        if (getPrimaryItemName() != null) {
            sb.append(" primaryitem ").append(getPrimaryItemName());
        }
        for (PropertyDefinition propDefn : declaredPropertyDefinitions()) {
            sb.append('\n').append(propDefn);
        }
        for (ChildNodeDefinition childDefn : declaredChildNodeDefinitions()) {
            sb.append('\n').append(childDefn);
        }
        sb.append('\n');
        return sb.toString();
    }

    private NodeTypeIterator iterator(final List<NodeType> nodeTypes) {
        return new NodeTypeIterator() {
            private int position = 0;
            private Iterator<NodeType> iterator = nodeTypes.iterator();

            @Override
            public javax.jcr.nodetype.NodeType nextNodeType() {
                NodeType nodeType = iterator.next();
                position++;
                return nodeType;
            }

            @Override
            public void skip( long skipNum ) {
                for (int i = 0; i < skipNum; i++) {
                    position++;
                    if (position >= getSize()) {
                        throw new NoSuchElementException();
                    }
                    nextNodeType();
                }
            }

            @Override
            public long getSize() {
                return nodeTypes.size();
            }

            @Override
            public long getPosition() {
                return position;
            }

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Object next() {
                return iterator.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
