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
package org.modeshape.jcr;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.api.text.TextExtractor;
import org.modeshape.jcr.api.text.TextExtractorOutput;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Facility for managing {@link TextExtractor} instances.
 */
@Immutable
public final class TextExtractors implements TextExtractor {

    public static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    private final List<TextExtractor> extractors;
    private final boolean stopAfterFirst;

    public TextExtractors( JcrRepository.RunningState repository,
                           Collection<Component> components ) {
        this.stopAfterFirst = true;
        final ClassLoader defaultClassLoader = getClass().getClassLoader();
        final ExecutionContext context = repository.context();
        this.extractors = new ArrayList<TextExtractor>();
        for (Component component : components) {
            try {
                ClassLoader cl = context.getClassLoader(component.getClasspath());
                if (cl == null) cl = defaultClassLoader;
                TextExtractor extractor = component.createInstance(cl);
                this.extractors.add(extractor);
            } catch (Throwable t) {
                String name = component.getName();
                String repoName = repository.name();
                Logger.getLogger(getClass()).error(t, JcrI18n.unableToInitializeTextExtractor, name, repoName, t.getMessage());
            }
        }
    }

    /**
     * Get the number of text extractors.
     * 
     * @return the number of text extractors; may be 0 or greater
     */
    public int size() {
        return extractors.size();
    }

    @Override
    public boolean supportsMimeType( String mimeType ) {
        for (TextExtractor extractor : extractors) {
            if (extractor.supportsMimeType(mimeType)) return true;
        }
        return false;
    }

    @Override
    public void extractFrom( InputStream stream,
                             TextExtractorOutput output,
                             Context context ) throws IOException {
        if (stream == null) return;
        if (stream.markSupported()) {
            stream.mark(Integer.MAX_VALUE);
        }
        final String mimeType = context.getMimeType();

        // Run through the extractors and have them extract the text
        for (TextExtractor extractor : extractors) {
            if (!extractor.supportsMimeType(mimeType)) continue;
            extractor.extractFrom(stream, output, context);
            if (stopAfterFirst || !stream.markSupported()) break;
            stream.reset();
        }
    }
}
