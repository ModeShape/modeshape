/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.repository.rules;

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.rules.RuleServiceProvider;
import javax.rules.admin.RuleExecutionSet;
import javax.rules.admin.RuleExecutionSetProvider;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.component.ClassLoaderFactory;
import org.jboss.dna.common.component.ComponentConfig;
import org.jboss.dna.common.util.CheckArg;

/**
 * A description of a set of rules compatible with a JSR-94 rule engine.
 * @author Randall Hauch
 */
@Immutable
public class RuleSet extends ComponentConfig implements Cloneable {

    private final String providerUri;
    private final String ruleSetUri;
    private final String rules;
    private final Map<String, Object> properties;

    /**
     * Create a JSR-94 rule set definition.
     * @param name the name of the rule set, which is considered the unique identifier
     * @param description the description
     * @param classname the name of the Java class used for the component
     * @param classpath the optional classpath (defined in a way compatible with a {@link ClassLoaderFactory}
     * @param providerUri the URI of the JSR-94 {@link RuleServiceProvider} implementation to use
     * @param ruleSetUri the URI of the JSR-94 {@link RuleExecutionSet} represented by this object; if null, the name is used
     * @param rules the string containing the rules in a provider-specific language
     * @param properties the provider-specific properties, whose values should be strings or byte arrays (the latter if the
     * provider expects an {@link Reader} with the value)
     * @throws IllegalArgumentException if any of the name, classname, provider URI, or rules parameters are null, empty or blank,
     * or if the classname is not a valid Java classname
     */
    public RuleSet( String name, String description, String classname, String[] classpath, String providerUri, String ruleSetUri, String rules, Map<String, Object> properties ) {
        super(name, description, System.currentTimeMillis(), classname, classpath);
        if (ruleSetUri == null) ruleSetUri = name.trim();
        CheckArg.isNotEmpty(ruleSetUri, "rule set URI");
        CheckArg.isNotEmpty(providerUri, "provider URI");
        CheckArg.isNotEmpty(rules, "rules");
        this.providerUri = providerUri;
        this.ruleSetUri = ruleSetUri;
        this.rules = rules;
        if (properties == null) properties = Collections.emptyMap();
        this.properties = Collections.unmodifiableMap(properties);
    }

    /**
     * Get the URI of the JSR-94 {@link RuleServiceProvider} implementation that should be used.
     * @return the URI of the JSR-94 implementation; never null, empty or blank
     */
    public String getProviderUri() {
        return this.providerUri;
    }

    /**
     * Get the URI of this rule set. The value must be valid as defined by JSR-94 {@link RuleExecutionSet}.
     * @return the rule set's URI; never null, empty or blank
     */
    public String getRuleSetUri() {
        return this.ruleSetUri;
    }

    /**
     * Get the rules defined in terms of the language reqired by the {@link #getProviderUri() provider}.
     * @return the rules for this rule set
     */
    public String getRules() {
        return this.rules;
    }

    /**
     * Get this rule set's properties as an unmodifiable map. Note that the values of these properties are either strings if the
     * value is to be {@link #getExecutionSetProperties() passed} literally, or a byte array if the value is to be
     * {@link #getExecutionSetProperties() passed} as an InputStream.
     * @return the unmodifiable properties; never null but possible empty
     */
    public Map<String, Object> getProperties() {
        return this.properties;
    }

    /**
     * Get the properties for this rule set that can be passed to an {@link RuleExecutionSetProvider}'s
     * {@link RuleExecutionSetProvider#createRuleExecutionSet(String, Map) createRuleExecutionSet} method.
     * <p>
     * This method converts any byte array value in the {@link #getProperties() properties} into an {@link Reader}. Since
     * {@link ByteArrayInputStream} is used, there is no need to close these stream.
     * </p>
     * @return the properties; never null but possible empty
     */
    public Map<Object, Object> getExecutionSetProperties() {
        Map<Object, Object> props = new HashMap<Object, Object>();
        for (Map.Entry<String, Object> entry : this.properties.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof byte[]) {
                value = new InputStreamReader(new ByteArrayInputStream((byte[])value));
            }
            props.put(key, value);
        }
        return props;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasChanged( ComponentConfig obj ) {
        if (super.hasChanged(obj)) return true;
        RuleSet that = (RuleSet)obj;
        if (!this.providerUri.equals(that.providerUri)) return true;
        if (!this.ruleSetUri.equals(that.ruleSetUri)) return true;
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public RuleSet clone() {
        return new RuleSet(this.getName(), this.getDescription(), this.getComponentClassname(), this.getComponentClasspathArray(), this.providerUri, this.ruleSetUri, this.rules, this.properties);
    }
}
