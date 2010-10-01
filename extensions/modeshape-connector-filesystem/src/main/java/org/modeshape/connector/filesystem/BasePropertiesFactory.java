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
package org.modeshape.connector.filesystem;

import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.ModeShapeIntLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

/**
 * A base class for {@link CustomPropertiesFactory} implementations that handle "extra" or "custom" properties for 'nt:file',
 * 'nt:folder', or 'nt:resource' nodes.
 */
public abstract class BasePropertiesFactory implements CustomPropertiesFactory {
    private static final long serialVersionUID = 1L;

    /**
     * Only certain properties are tolerated when writing content (dna:resource or jcr:resource) nodes. These properties are
     * implicitly stored (primary type, data) or silently ignored (encoded, mimetype, last modified). The silently ignored
     * properties must be accepted to stay compatible with the JCR specification.
     */
    protected final Set<Name> STANDARD_PROPERTIES_FOR_CONTENT = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                              Arrays.asList(new Name[] {
                                                                                                                  JcrLexicon.PRIMARY_TYPE,
                                                                                                                  JcrLexicon.DATA,
                                                                                                                  JcrLexicon.ENCODING,
                                                                                                                  JcrLexicon.MIMETYPE,
                                                                                                                  JcrLexicon.LAST_MODIFIED,
                                                                                                                  JcrLexicon.LAST_MODIFIED_BY,
                                                                                                                  JcrLexicon.UUID,
                                                                                                                  ModeShapeIntLexicon.NODE_DEFINITON})));
    /**
     * Only certain properties are tolerated when writing files (nt:file) or folders (nt:folder) nodes. These properties are
     * implicitly stored in the file or folder (primary type, created).
     */
    protected final Set<Name> STANDARD_PROPERTIES_FOR_FILE_OR_FOLDER = Collections.unmodifiableSet(new HashSet<Name>(
                                                                                                                     Arrays.asList(new Name[] {
                                                                                                                         JcrLexicon.PRIMARY_TYPE,
                                                                                                                         JcrLexicon.CREATED,
                                                                                                                         JcrLexicon.CREATED_BY,
                                                                                                                         JcrLexicon.UUID,
                                                                                                                         ModeShapeIntLexicon.NODE_DEFINITON})));

    protected static final Collection<Property> NO_PROPERTIES_COLLECTION = Collections.emptyList();
    protected static final Set<Name> NO_NAMES = Collections.emptySet();

    /**
     * Create an instance of this factory.
     */
    protected BasePropertiesFactory() {
    }

    /**
     * Create a filename filter that will ignore any files needed by this implementation.
     * 
     * @param exclusionFilter the default filter, which should be included; may be null if there is no such filter
     * @return the filter
     */
    public FilenameFilter getFilenameFilter( FilenameFilter exclusionFilter ) {
        return exclusionFilter;
    }
}
