/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.jboss;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DEFAULT;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIPTION;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MAX_OCCURS;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REQUIRED;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.TYPE;

import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.jboss.dmr.ModelNode;
import org.jboss.dmr.ModelType;

@SuppressWarnings("nls")
enum Element {
    // must be first
    UNKNOWN(null),  
            	
	// Repository 
    REPOSITORY_ELEMENT("repository"),
    REPOSITORY_NAME_ATTRIBUTE("name", "name", ModelType.STRING, true, null),
    REPOSITORY_JNDI_NAME_ATTRIBUTE("jndi-name", "jndi-name", ModelType.STRING, true, null),
    REPOSITORY_ROOT_NODE_ID_ATTRIBUTE("root-node-id", "root-node-id", ModelType.STRING, false, "cafebabe-cafe-babe-cafe-babecafebabe"),
    REPOSITORY_LARGE_VALUE_SIZE_ID_ATTRIBUTE("large-value-size", "large-value-size", ModelType.STRING, false, "10240");
//    REPOSITORY_STORAGE_ELEMENT("storage"),
//    REPOSITORY_CACHE_NAME("cache-name", "cache-name", ModelType.STRING, true, null),
//    REPOSITORY_CACHE_CONFIGURATION("cache-configuration", "cache-configuration", ModelType.STRING, true, null),
//    REPOSITORY_WORKSPACES_ELEMENT("workspaces"),
//    REPOSITORY_WORKSPACE_SYSTEM("system", "system", ModelType.STRING, true, "system"),
//    REPOSITORY_WORKSPACE_DEFAULT("default", "default", ModelType.STRING, true, "system"),
//    REPOSITORY_WORKSPACE_ALLOW_CREATION("allow-creation", "allow-creation", ModelType.STRING, true, "true"),
//    REPOSITORY_WORKSPACE_PREDEFINED("predefined", "predefined", ModelType.STRING, true, null),
//    REPOSITORY_QUERY("query"),
//    REPOSITORY_QUERY_EXTRACTORS("extractors"),
//    REPOSITORY_QUERY_EXTRACTOR_TYPE("extractorType"),
//    REPOSITORY_QUERY_EXTRACTOR("extractor", "extractor", ModelType.STRING, true, null),
//    REPOSITORY_QUERY_EXTRACTOR_NAME("name", "name", ModelType.STRING, true, null),
//    REPOSITORY_QUERY_EXTRACTOR_CLASS_NAME("classname", "classname", ModelType.STRING, true, null),
//    REPOSITORY_QUERY_EXTRACTOR_DESCRIPTION("description", "description", ModelType.STRING, true, null),
//    REPOSITORY_QUERY_REBUILD_UPON_STARTUP_ENUM("rebuild-upon-startup-enum", "rebuild-upon-startup-enum", ModelType.STRING, true, "ifMissing"),
//    REPOSITORY_QUERY_ENABLED("enabled", "enabled", ModelType.BOOLEAN, true, "true"),
//    TRANSLATOR_MODULE_ATTRIBUTE("module", "module", ModelType.BOOLEAN, true, "true");
//    
    private final String name;
    private final String modelName;
    private final boolean required;
    private final ModelType modelType;
    private final String defaultValue;

    private Element(String name) {
    	this.name = name;
    	this.modelName = name;
    	this.required = false;
    	this.modelType = null;
    	this.defaultValue = null;
    }
    
    Element(final String name, String modelName, ModelType type, boolean required, String defltValue) {
        this.name = name;
        this.modelName = modelName;
        this.modelType = type;
        this.required = required;
        this.defaultValue = defltValue;
    }

    /**
     * Get the local name of this element.
     *
     * @return the local name
     */
    public String getLocalName() {
        return name;
    }
    
    public String getModelName() {
    	return this.modelName;
    }
    
    private static final Map<String, Element> elements;

    static {
        final Map<String, Element> map = new HashMap<String, Element>();
        for (Element element : values()) {
            final String name = element.getModelName();
            if (name != null) map.put(name, element);
        }
        elements = map;
    }

    public static Element forName(String localName, Element parentNode) {
    	String modelName = parentNode.getLocalName()+"-"+localName;
        final Element element = elements.get(modelName);
        return element == null ? UNKNOWN : element;
    }
    
    public static Element forName(String localName) {
        final Element element = elements.get(localName);
        return element == null ? UNKNOWN : element;
    }    
    
    public void describe(ModelNode node, String type, ResourceBundle bundle) {
		String name = getModelName();
		node.get(type, name, TYPE).set(this.modelType);
        node.get(type, name, DESCRIPTION).set(getDescription(bundle));
        node.get(type, name, REQUIRED).set(this.required);
        node.get(type, name, MAX_OCCURS).set(1);
        
        if (this.defaultValue != null) {
        	if (ModelType.INT == this.modelType) {
        		node.get(type, name, DEFAULT).set(Integer.parseInt(this.defaultValue));
        	}
        	else if (ModelType.BOOLEAN == this.modelType) {
        		node.get(type, name, DEFAULT).set(Boolean.parseBoolean(this.defaultValue));
        	}
        	else if (ModelType.LONG == this.modelType) {
        		node.get(type, name, DEFAULT).set(Long.parseLong(this.defaultValue));
        	}        	
        	else if (ModelType.STRING == this.modelType) {
        		node.get(type, name, DEFAULT).set(this.defaultValue);
        	}
        	else {
        		throw new RuntimeException();
        	}
        }        
    }
    
    public void populate(ModelNode operation, ModelNode model) {
    	if (getModelName() == null) {
    		return;
    	}
    	
    	if (operation.hasDefined(getModelName())) {
    		if (ModelType.STRING == this.modelType) {
    			model.get(getModelName()).set(operation.get(getModelName()).asString());
    		}
    		else if (ModelType.INT == this.modelType) {
    			model.get(getModelName()).set(operation.get(getModelName()).asInt());
    		}
    		else if (ModelType.LONG == this.modelType) {
    			model.get(getModelName()).set(operation.get(getModelName()).asLong());
    		}
    		else if (ModelType.BOOLEAN == this.modelType) {
    			model.get(getModelName()).set(operation.get(getModelName()).asBoolean());
    		}
    		else {
    			throw new RuntimeException();
    		}
    	}
    }
    
    public boolean isDefined(ModelNode node) {
    	return node.hasDefined(getModelName());
    }
    
    public int asInt(ModelNode node) {
    	return node.get(getModelName()).asInt();
    }
    
    public long asLong(ModelNode node) {
    	return node.get(getModelName()).asLong();
    }
    
    public String asString(ModelNode node) {
    	return node.get(getModelName()).asString();
    }
    
    public boolean asBoolean(ModelNode node) {
    	return node.get(getModelName()).asBoolean();
    }
    
    public boolean isLike(ModelNode node) {
    	Set<String> keys = node.keys();
    	for(String key:keys) {
    		if (key.startsWith(this.name)) {
    			return true;
    		}
    	}
    	return false; 
    }
    
    public String getDescription(ResourceBundle bundle) {
    	return bundle.getString(this.modelName+".describe");
    }

	public boolean sameAsDefault(String value) {
		if (this.defaultValue == null) {
			return (value == null);
		}
		return this.defaultValue.equalsIgnoreCase(value);
	}
}

