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
package org.modeshape.graph.property.basic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;

/**
 * <p>
 * An implementation of {@link PropertyFactory} which provides the feature for
 * substituting special decorated variables (i.e., ${variable}) with a system
 * property. This will support the following decorated syntax options:
 * <li>${variable}</li>
 * <li>${variable [,variable,..] }</li>
 * <li>${variable [:defaultvalue] }</li>
 * </p>
 * <p>
 * Where the <i>variable</i> represents a value to be looked up in the System
 * properties and <i>defaultvalue</i> indicates what to use when the System
 * property is not found.
 * </p>
 * Notice that the syntax supports multiple <i>variables</i>. The logic will
 * process the <i>variables</i> from let to right, until a System property is
 * found. And at that point, it will stop and will not attempt to find values
 * for the other <i>variables</i>.
 * <p>
 */
@Immutable
public class SystemPropertyFactory extends BasicPropertyFactory {

    private final ValueFactories factories;

    /**
     * @param valueFactories the value factories
     * @throws IllegalArgumentException if the reference to the value factories is null
     */
    public SystemPropertyFactory( ValueFactories valueFactories ) {
	super(valueFactories);
        this.factories = valueFactories;
    }


    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            PropertyType desiredType,
                            Object... values ) {
        CheckArg.isNotNull(name, "name");
        if (values == null || values.length == 0) {
            return new BasicEmptyProperty(name);
        }
        final int len = values.length;
        if (desiredType == null) desiredType = PropertyType.OBJECT;
        final ValueFactory<?> factory = factories.getValueFactory(desiredType);
        if (values.length == 1) {
            Object value = values[0];
            // Check whether the sole value was a collection ...
            if (value instanceof Path) {
                value = factory.create(value);
                return new BasicSingleValueProperty(name, value);
            }
            if (value instanceof Collection<?>) {
                // The single value is a collection, so create property with the collection's contents ...
                return create(name, desiredType, (Iterable<?>)value);
            }
            if (value instanceof Iterator<?>) {
                // The single value is an iterator over a collection, so create property with the iterator's contents ...
                return create(name, desiredType, (Iterator<?>)value);
            }
            if (value instanceof Object[]) {
                // The single value is an object array, so create the property with the array as the value(s)...
                return create(name, desiredType, (Object[])value);
            } 
            if (value instanceof String) {
        	value = factory.create(getSubstitutedProperty((String)value));
        	return new BasicSingleValueProperty(name, value);
            }
            value = factory.create(value);
            return new BasicSingleValueProperty(name, value);
        }
        List<Object> valueList = new ArrayList<Object>(len);
        for (int i = 0; i != len; ++i) {
            Object value = factory.create(values[i]);
            valueList.add(value);
        }
        return new BasicMultiValueProperty(name, valueList);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings( "unchecked" )
    public Property create( Name name,
                            PropertyType desiredType,
                            Iterable<?> values ) {
        CheckArg.isNotNull(name, "name");
        List<Object> valueList = null;
        if (values instanceof Collection) {
            Collection<Object> originalValues = (Collection<Object>)values;
            if (originalValues.isEmpty()) {
                return new BasicEmptyProperty(name);
            }
            valueList = new ArrayList<Object>(originalValues.size());
        } else {
            // We don't know the size
            valueList = new ArrayList<Object>();
        }
        // Copy the values, ensuring that the values are the correct type ...
        if (desiredType == null) desiredType = PropertyType.OBJECT;
        final ValueFactory<?> factory = factories.getValueFactory(desiredType);
        for (Object value : values) {
            valueList.add(factory.create(value));
        }
        if (valueList.isEmpty()) { // may not have been a collection earlier
            return new BasicEmptyProperty(name);
        }
        if (valueList.size() == 1) {
            Object o = valueList.get(0);
            if (o instanceof String) {
        	o = getSubstitutedProperty( (String) o);
            }
            return new BasicSingleValueProperty(name, o);
        }
        return new BasicMultiValueProperty(name, valueList);
    }

    /**
     * {@inheritDoc}
     */
    public Property create( Name name,
                            PropertyType desiredType,
                            Iterator<?> values ) {
        CheckArg.isNotNull(name, "name");
        final List<Object> valueList = new ArrayList<Object>();
        if (desiredType == null) desiredType = PropertyType.OBJECT;
        final ValueFactory<?> factory = factories.getValueFactory(desiredType);
        while (values.hasNext()) {
            Object value = values.next();
            value = factory.create(value);
            valueList.add(value);
        }
        if (valueList.isEmpty()) {
            return new BasicEmptyProperty(name);
        }
        if (valueList.size() == 1) {
            Object o = valueList.get(0);
            if (o instanceof String) {
        	o = getSubstitutedProperty( (String) o);
            }
          
            return new BasicSingleValueProperty(name, o);
        }
        return new BasicMultiValueProperty(name, valueList);
    }
    
    private static final String CURLY_PREFIX = "${";
    private static final String CURLY_SUFFIX = "}";
    private static final String VAR_DELIM = ",";
    private static final String DEFAULT_DELIM = ":";
   
    
    /**
     * getSubstitutedProperty is called to perform the property substitution on
     * the value.
     * @param value
     * @return String
     */
    protected String getSubstitutedProperty(String value) {

	if (value == null || value.trim().length() == 0) return null;
	
	StringBuffer sb = null;

	sb = new StringBuffer(value);

	    // Get the index of the first constant, if any
	int startName = sb.indexOf(CURLY_PREFIX);

	if (startName == -1) return value;
	    
	// process as many different variable groupings that are defined, where one group will resolve to one property substitution
	while (startName != -1) {
		String defaultValue = null;
		
		int endName = sb.indexOf(CURLY_SUFFIX, startName);

		if (endName == -1) {
		    // if no suffix can be found, then this variable was probably defined incorrectly
		    // but return what there is at this point
		    return sb.toString();
		}

		String varString = sb.substring(startName + 2, endName);
		if (varString.indexOf(DEFAULT_DELIM) > -1) {
		    List<String> defaults = split(varString, DEFAULT_DELIM);
		
		// get the property(s) variables that are defined left of the default delimiter.
		    varString = defaults.get(0);
		
		// if the default is defined, then capture in case none of the other properties are found
		    if (defaults.size() == 2 ) {
        		    defaultValue = defaults.get(1);
		    }
		}
		
		String constValue = null;
		// split the property(s) based VAR_DELIM, when multiple property options are defined
		List<String> vars = split(varString, VAR_DELIM);
		for (String var : vars) {
			constValue = System.getenv(var);
			if (constValue == null) {
			    constValue = System.getProperty(var);
			}		    
		    
			// the first found property is the value to be substituted
			if (constValue != null) {
			    break;
			}
		}

		// if no property is found to substitute, then use the default value, if defined
		if (constValue == null && defaultValue != null) {
		    constValue = defaultValue;
		}
		
		if (constValue != null) {
		    sb = sb.replace(startName, endName + 1, constValue);
		    // Checking for another constants
		    startName = sb.indexOf(CURLY_PREFIX);

		} else {
		    // continue to try to substitute for other properties so that all defined variables
		    // are tried to be substituted for
		    startName = sb.indexOf(CURLY_PREFIX, endName);
		    
		}

	}
	    
	return sb.toString();
    }
    
	/**
	 * Split a string into pieces based on delimiters.  Similar to the perl function of
	 * the same name.  The delimiters are not included in the returned strings.
	 *
	 * @param str Full string
	 * @param splitter Characters to split on
	 * @return List of String pieces from full string
	 */
    private static List<String> split(String str, String splitter) {
        StringTokenizer tokens = new StringTokenizer(str, splitter);
        ArrayList<String> l = new ArrayList<String>(tokens.countTokens());
        while(tokens.hasMoreTokens()) {
            l.add(tokens.nextToken());
        }
        return l;
    }    

}
