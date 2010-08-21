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
package org.modeshape.graph.sequencer;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.SecurityContext;
import org.modeshape.graph.mimetype.MimeTypeDetector;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;

/**
 * A special {@link ExecutionContext} that is used for sequencing streams.
 */
@ThreadSafe
public class StreamSequencerContext extends ExecutionContext {

    private final Path inputPath;
    private final Map<Name, Property> inputPropertiesByName;
    private final Set<Property> inputProperties;
    private final Problems problems;
    private final String mimeType;

    public StreamSequencerContext( ExecutionContext context,
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
     * Return the MIME-type of the content being sequenced.
     * 
     * @return the MIME-type
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
     * @see org.modeshape.graph.ExecutionContext#with(org.modeshape.common.component.ClassLoaderFactory)
     */
    @Override
    public StreamSequencerContext with( ClassLoaderFactory classLoaderFactory ) {
        return new StreamSequencerContext(super.with(classLoaderFactory), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.ExecutionContext#with(java.util.Map)
     */
    @Override
    public StreamSequencerContext with( Map<String, String> data ) {
        return new StreamSequencerContext(super.with(data), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.ExecutionContext#with(org.modeshape.graph.mimetype.MimeTypeDetector)
     */
    @Override
    public StreamSequencerContext with( MimeTypeDetector mimeTypeDetector ) {
        return new StreamSequencerContext(super.with(mimeTypeDetector), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.ExecutionContext#with(org.modeshape.graph.property.NamespaceRegistry)
     */
    @Override
    public StreamSequencerContext with( NamespaceRegistry namespaceRegistry ) {
        return new StreamSequencerContext(super.with(namespaceRegistry), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.ExecutionContext#with(org.modeshape.graph.SecurityContext)
     */
    @Override
    public StreamSequencerContext with( SecurityContext securityContext ) {
        return new StreamSequencerContext(super.with(securityContext), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.ExecutionContext#with(java.lang.String, java.lang.String)
     */
    @Override
    public StreamSequencerContext with( String key,
                                        String value ) {
        return new StreamSequencerContext(super.with(key, value), inputPath, inputProperties, mimeType, problems);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.ExecutionContext#with(java.lang.String)
     */
    @Override
    public StreamSequencerContext with( String processId ) {
        return new StreamSequencerContext(super.with(processId), inputPath, inputProperties, mimeType, problems);
    }
}
