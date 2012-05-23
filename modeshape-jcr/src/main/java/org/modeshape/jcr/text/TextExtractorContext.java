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
package org.modeshape.jcr.text;

import java.util.Map;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.component.ClassLoaderFactory;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.mimetype.MimeTypeDetector;
import org.modeshape.jcr.api.text.TextExtractor;
import org.modeshape.jcr.security.SecurityContext;
import org.modeshape.jcr.value.NamespaceRegistry;

/**
 * A context for extracting the content.
 */
public class TextExtractorContext extends ExecutionContext implements TextExtractor.Context {

    private final Problems problems;
    private final String mimeType;

    public TextExtractorContext( ExecutionContext context,
                                 String mimeType,
                                 Problems problems ) {
        super(context);
        this.mimeType = mimeType;
        this.problems = problems != null ? problems : new SimpleProblems();
    }

    @Override
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

    @Override
    public TextExtractorContext with( ClassLoaderFactory classLoaderFactory ) {
        return new TextExtractorContext(super.with(classLoaderFactory), mimeType, problems);
    }

    @Override
    public TextExtractorContext with( Map<String, String> data ) {
        return new TextExtractorContext(super.with(data), mimeType, problems);
    }

    @Override
    public TextExtractorContext with( MimeTypeDetector mimeTypeDetector ) {
        return new TextExtractorContext(super.with(mimeTypeDetector), mimeType, problems);
    }

    @Override
    public TextExtractorContext with( NamespaceRegistry namespaceRegistry ) {
        return new TextExtractorContext(super.with(namespaceRegistry), mimeType, problems);
    }

    @Override
    public TextExtractorContext with( SecurityContext securityContext ) {
        return new TextExtractorContext(super.with(securityContext), mimeType, problems);
    }

    @Override
    public TextExtractorContext with( String key,
                                      String value ) {
        return new TextExtractorContext(super.with(key, value), mimeType, problems);
    }

    @Override
    public TextExtractorContext with( String processId ) {
        return new TextExtractorContext(super.with(processId), mimeType, problems);
    }
}
