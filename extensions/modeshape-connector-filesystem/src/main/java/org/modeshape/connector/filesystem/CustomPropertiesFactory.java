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

import java.io.File;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Property;

/**
 * A simple interface that allows an implementer to define additional properties for "nt:folder", "nt:file", and "nt:resource"
 * nodes created by the file system connector.
 * <p>
 * To use, supply the implementation to a {@link FileSystemSource} object (or register the factory in a subclass of
 * FileSystemSource). Implementations should be immutable because they are shared between all the connections.
 * </p>
 */
@Immutable
public interface CustomPropertiesFactory extends Serializable {

    /**
     * Construct the custom properties that should be created for the supplied directory that is to be treated as an "nt:folder".
     * The resulting properties should not include the standard {@link JcrLexicon#PRIMARY_TYPE} or {@link JcrLexicon#CREATED}
     * properties, which are set automatically and will override any returned Property with the same name.
     * 
     * @param context the execution context; never null
     * @param location the Location of the node, which always contains a {@link Location#getPath() path}; never null
     * @param directory the file system object; never null and {@link File#isDirectory()} will always return true
     * @return the custom properties; never null but possibly empty
     */
    Collection<Property> getDirectoryProperties( ExecutionContext context,
                                                 Location location,
                                                 File directory );

    /**
     * Construct the custom properties that should be created for the supplied file that is to be treated as an "nt:resource",
     * which is the node that contains the content-oriented properties and that is a child of a "nt:file" node. The resulting
     * properties should not include the standard {@link JcrLexicon#PRIMARY_TYPE}, {@link JcrLexicon#LAST_MODIFIED}, or
     * {@link JcrLexicon#DATA} properties, which are set automatically and will override any returned Property with the same name.
     * 
     * @param context the execution context; never null
     * @param location the Location of the node, which always contains a {@link Location#getPath() path}; never null
     * @param file the file system object; never null and {@link File#isFile()} will always return true
     * @param mimeType the mime type for the file, as determined by the {@link ExecutionContext#getMimeTypeDetector() MIME type
     *        detector}, or null if the MIME type could not be determined
     * @return the custom properties; never null but possibly empty
     */
    Collection<Property> getResourceProperties( ExecutionContext context,
                                                Location location,
                                                File file,
                                                String mimeType );

    /**
     * Construct the custom properties that should be created for the supplied file that is to be treated as an "nt:file". The
     * resulting properties should not include the standard {@link JcrLexicon#PRIMARY_TYPE} or {@link JcrLexicon#CREATED}
     * properties, which are set automatically and will override any returned Property with the same name.
     * <p>
     * Although the connector does not automatically determine the MIME type for the "nt:file" nodes, an implementation can
     * determine the MIME type by using the context's {@link ExecutionContext#getMimeTypeDetector() MIME type detector}. Note,
     * however, that this may be an expensive operation, so it should be used only when needed.
     * </p>
     * 
     * @param context the execution context; never null
     * @param location the Location of the node, which always contains a {@link Location#getPath() path}; never null
     * @param file the file system object; never null and {@link File#isFile()} will always return true
     * @return the custom properties; never null but possibly empty
     */
    Collection<Property> getFileProperties( ExecutionContext context,
                                            Location location,
                                            File file );

    /**
     * Record the supplied properties as being set on the designated "nt:folder" node.
     * 
     * @param context the execution context; never null
     * @param sourceName the name of the repository source; never null
     * @param location the Location of the node, which always contains a {@link Location#getPath() path}; never null
     * @param file the file system object; never null, and both {@link File#exists()} and {@link File#isDirectory()} will always
     *        return true
     * @param properties the properties that are to be set
     * @return the names of the properties that were created, or an empty or null set if no properties were created on the file
     * @throws RepositorySourceException if any properties are invalid or cannot be set on these nodes
     */
    Set<Name> recordDirectoryProperties( ExecutionContext context,
                                         String sourceName,
                                         Location location,
                                         File file,
                                         Map<Name, Property> properties ) throws RepositorySourceException;

    /**
     * Record the supplied properties as being set on the designated "nt:file" node.
     * 
     * @param context the execution context; never null
     * @param sourceName the name of the repository source; never null
     * @param location the Location of the node, which always contains a {@link Location#getPath() path}; never null
     * @param file the file system object; never null, and both {@link File#exists()} and {@link File#isFile()} will always return
     *        true
     * @param properties the properties that are to be set
     * @return the names of the properties that were created, or an empty or null set if no properties were created on the file
     * @throws RepositorySourceException if any properties are invalid or cannot be set on these nodes
     */
    Set<Name> recordFileProperties( ExecutionContext context,
                                    String sourceName,
                                    Location location,
                                    File file,
                                    Map<Name, Property> properties ) throws RepositorySourceException;

    /**
     * Record the supplied properties as being set on the designated "nt:resource" node.
     * 
     * @param context the execution context; never null
     * @param sourceName the name of the repository source; never null
     * @param location the Location of the node, which always contains a {@link Location#getPath() path}; never null
     * @param file the file system object; never null, and both {@link File#exists()} and {@link File#isFile()} will always return
     *        true
     * @param properties the properties that are to be set
     * @return the names of the properties that were created, or an empty or null set if no properties were created on the file
     * @throws RepositorySourceException if any properties are invalid or cannot be set on these nodes
     */
    Set<Name> recordResourceProperties( ExecutionContext context,
                                        String sourceName,
                                        Location location,
                                        File file,
                                        Map<Name, Property> properties ) throws RepositorySourceException;

}
