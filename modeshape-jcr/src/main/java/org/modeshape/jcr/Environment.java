/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr;

import java.util.Optional;
import org.jgroups.Channel;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.persistence.file.FileDbProvider;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * A basic environment in which a repository operates. The logical names supplied to these methods are typically obtained directly
 * from the {@link RepositoryConfiguration}.
 */
@ThreadSafe
public interface Environment {

    /**
     * Returns a default persistence configuration document, when nothing is explicitly configured for a repository.
     * 
     * @return a {@link Document} instance, never {@code null}
     */
    default Document defaultPersistenceConfiguration() {
        return new BasicDocument(RepositoryConfiguration.FieldName.TYPE, FileDbProvider.TYPE_MEM);
    }

    /**
     * Returns {@link SchematicDb} instance for the given configuration and optional classpath entries using a 
     * {@link #getClassLoader(Object, String...) custom loader}. The supplied configuration document may contain a custom 
     * {@link org.modeshape.jcr.RepositoryConfiguration.FieldName#CLASSLOADER} attribute which if present, will be used as an
     * additional classpath entry.
     * 
     * @param persistenceConfig a {@link Document} representing the persistence configuration document; it may be null in which
     * case a {@link #defaultPersistenceConfiguration() default configuration} will be used
     * @return a {@link SchematicDb} instance, never {@code null}
     * 
     * @throws ConfigurationException if no persistence provider can return a valid database.
     */
    default SchematicDb getDb(Document persistenceConfig) {
        final Document config = persistenceConfig != null ? persistenceConfig : defaultPersistenceConfiguration();
        String classloader = config.getString(RepositoryConfiguration.FieldName.CLASSLOADER);
        String[] classpathEntries = classloader != null ? new String[] {classloader} : new String[0];
        Optional<SchematicDb> db = Optional.of(Schematic.getDb(config, getClassLoader(this, classpathEntries)));
        return db.orElseThrow(() -> new ConfigurationException(JcrI18n.unableToCreateDb.text(config)));
    }

    /**
     * Get a classloader given the supplied set of logical classpath entries, which the implementation can interpret however it
     * needs.
     * 
     * @param caller the object instance which calls this method and whose class loader will be used as a fallback; may not be null
     * @param classpathEntries the logical classpath entries; may be null
     * @return a classloader instance, never null
     */
    ClassLoader getClassLoader( Object caller, String... classpathEntries );
    
    /**
     * Get the JGroups channel with the given logical name.
     *
     * @param name the name of the channel; may not be null
     * @return the channel, or null if there is no such channel and the environment does not support clustering
     * @throws Exception if there is a problem obtaining the named channel
     */
    Channel getChannel(String name) throws Exception;

    /**
     * Shutdown this environment, allowing it to reclaim any resources.
     */
    default void shutdown() {}

}
