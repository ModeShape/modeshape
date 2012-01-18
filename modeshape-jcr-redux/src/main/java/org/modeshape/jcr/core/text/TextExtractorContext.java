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
package org.modeshape.jcr.core.text;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.core.SecurityContext;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;

/**
 * A context for extracting the content.
 */
public class TextExtractorContext extends ExecutionContext {

    private final Path inputPath;
    private final Map<Name, Property> inputPropertiesByName;
    private final Set<Property> inputProperties;
    private final Problems problems;
    private final String mimeType;

    public TextExtractorContext( ExecutionContext context,
                                 Path inputPath,
                                 Set<Property> inputProperties,
                                 String mimeType,
                                 Problems problems ) {
        super(context);
        this.inputPath = inputPath;
        this.inputProperties = inputProperties != null ? new HashSet<Property>(inputProperties) : new HashSet<Property>();
        this.mimeType = mimeType;
        this.problems = problems != null ? problems : new SimpleProblems();
        Map<Name, Property> inputPropertiesByName = new HashMap<Name, Property>();
        for (Property property : this.inputProperties) {
            inputPropertiesByName.put(property.getName(), property);
        }
        this.inputPropertiesByName = Collections.unmodifiableMap(inputPropertiesByName);
    }

    /**
     * Return the path of the input node containing the content being sequenced.
     * 
     * @return input node's path.
     */
    public Path getInputPath() {
        return inputPath;
    }

    /**
     * Return the set of properties from the input node containing the content being sequenced.
     * 
     * @return the input node's properties; never <code>null</code>.
     */
    public Set<Property> getInputProperties() {
        return inputProperties;
    }

    /**
     * Return the property with the supplied name from the input node containing the content being sequenced.
     * 
     * @param name
     * @return the input node property, or <code>null</code> if none exists.
     */
    public Property getInputProperty( Name name ) {
        return inputPropertiesByName.get(name);
    }

    /**
     * Get the MIME type of the content being processed, if it is known.
     * 
     * @return the MIME type, or null if the type is not known.
     */
    public String getMimeType() {
        return this.mimeType;
    }

    /**
     * Get an interface that can be used to record various problems, warnings, and errors that are not extreme enough to warrant
     * throwing exceptions.
     * 
     * @return the interface for recording problems; never null
     */
    public Problems getProblems() {
        return this.problems;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(org.modeshape.common.component.ClassLoaderFactory)
     */
    @Override
    public TextExtractorContext with( ClassLoaderFactory classLoaderFactory ) {
        return new TextExtractorContext(super.with(classLoaderFactory), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(java.util.Map)
     */
    @Override
    public TextExtractorContext with( Map<String, String> data ) {
        return new TextExtractorContext(super.with(data), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(org.modeshape.jcr.api.mimetype.MimeTypeDetector)
     */
    @Override
    public TextExtractorContext with( MimeTypeDetector mimeTypeDetector ) {
        return new TextExtractorContext(super.with(mimeTypeDetector), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(NamespaceRegistry)
     */
    @Override
    public TextExtractorContext with( NamespaceRegistry namespaceRegistry ) {
        return new TextExtractorContext(super.with(namespaceRegistry), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(org.modeshape.jcr.core.SecurityContext)
     */
    @Override
    public TextExtractorContext with( SecurityContext securityContext ) {
        return new TextExtractorContext(super.with(securityContext), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(java.lang.String, java.lang.String)
     */
    @Override
    public TextExtractorContext with( String key,
                                      String value ) {
        return new TextExtractorContext(super.with(key, value), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.core.ExecutionContext#with(java.lang.String)
     */
    @Override
    public TextExtractorContext with( String processId ) {
        return new TextExtractorContext(super.with(processId), inputPath, inputProperties, mimeType, problems);
    }
}
