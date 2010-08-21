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
package org.modeshape.jboss.managed.util;

import java.util.ArrayList;
import java.util.List;

import org.modeshape.common.text.Inflector;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Logger;
import org.modeshape.common.util.Reflection;
import org.modeshape.common.util.Reflection.Property;
import org.modeshape.jboss.managed.JBossManagedI18n;
import org.modeshape.jboss.managed.ManagedEngine.Component;
import org.modeshape.jboss.managed.ManagedEngine.ManagedProperty;

/**
 * Class for common utility methods used for ModeShape Managed Objects
 */
public class ManagedUtils { 

    private static final Logger LOGGER = Logger.getLogger(ManagedUtils.class);

    public static List<ManagedProperty> getProperties( Component objectType,
                                                       Object object ) {
        Reflection reflection = new Reflection(object.getClass());
        List<ManagedProperty> managedProps = new ArrayList<ManagedProperty>();
        List<Property> props = new ArrayList<Property>();
        boolean allInferred = true;
        try {
        	props = reflection.getAllPropertiesOn(object);
            for (Property prop : props) {
                if (prop.isInferred()) continue;
                allInferred = false;
                if (prop.isPrimitive() || prop.getType().toString().contains("java.lang.String")){
                	String valueAsString = reflection.getPropertyAsString(object, prop);
                    managedProps.add(new ManagedProperty(prop, valueAsString));
                }
            }
        
            //If all properties are inferred, then we will loop again and just use them all (as long as they are)
	        if (allInferred){
	        	for (Property prop : props) {
	                if (prop.isPrimitive() || prop.getType().toString().contains("java.lang.String")){
	                	String valueAsString = reflection.getPropertyAsString(object, prop);
	                    managedProps.add(new ManagedProperty(prop, valueAsString));
	                }
	            }
	        }

        } catch (Throwable e) {
            LOGGER.error(e, JBossManagedI18n.errorGettingPropertiesFromManagedObject, objectType);
        }
        
        return managedProps;
    }
    
    /**
     * Set the human-readable label for the property. If null, this will be set to the
     * {@link Inflector#humanize(String, String...) humanized} form of the name}.
     * 
     * @param name the label for the property; may not be null
     * @return label
     */
    public static String createLabel( String name ) {
       
    	CheckArg.isNotNull(name, "name");
    	name = name.replaceFirst("option.", "");
    	name = name.replaceFirst("custom.", "");
    	name = name.replace(".", " ");
    	String label = null;
    	Inflector inflector = Inflector.getInstance();
    	
    	if (name != null) {
            label = inflector.titleCase(inflector.humanize(name));
            label = label.replaceFirst("Jcr", "JCR");
        }
        return label;
    }

}
