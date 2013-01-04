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

package org.modeshape.jboss.service;

import java.util.List;
import java.util.Properties;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;

/**
 * {@link Service} implementation handling external sources
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SourceService implements Service<JcrRepository> {

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<ModeShapeEngine>();
    private final InjectedValue<JcrRepository> jcrRepositoryInjector = new InjectedValue<JcrRepository>();

    private final Properties sourceProperties;
    private final String repositoryName;

    public SourceService( String repositoryName,
                          Properties sourceProperties ) {
        this.repositoryName = repositoryName;
        this.sourceProperties = sourceProperties;
    }

    @Override
    public JcrRepository getValue() throws IllegalStateException, IllegalArgumentException {
        return jcrRepositoryInjector.getValue();
    }

    private ModeShapeEngine getModeShapeEngine() {
        return engineInjector.getValue();
    }

    @Override
    public void start( StartContext arg0 ) throws StartException {
        ModeShapeEngine engine = getModeShapeEngine();

        JcrRepository repository = null;
        try {
            repository = engine.getRepository(repositoryName);
        } catch (NoSuchRepositoryException e) {
            throw new StartException(e);
        }

        RepositoryConfiguration repositoryConfig = repository.getConfiguration();

        Editor configEditor = repositoryConfig.edit();
        EditableDocument externalSources = configEditor.getOrCreateDocument(RepositoryConfiguration.FieldName.EXTERNAL_SOURCES);

        EditableDocument externalSource = Schematic.newDocument();

        for (Object key : sourceProperties.keySet()) {
            String keyStr = (String)key;
            if (RepositoryConfiguration.FieldName.NAME.equals(keyStr)) {
                continue;
            }
            Object value = sourceProperties.get(keyStr);
            if (value instanceof List<?>) {
                for (Object val : (List<?>)value) {
                    externalSource.getOrCreateArray(keyStr).addValue(val);
                }
            } else {
                // Just set the value as a field
                externalSource.set(keyStr, value);
            }
        }

        String sourceName = sourceProperties.getProperty(RepositoryConfiguration.FieldName.NAME);
        assert sourceName != null;
        externalSources.setDocument(sourceName, externalSource);

        // Update the deployed repository's configuration with these changes
        try {
            engine.update(this.repositoryName, configEditor.getChanges());
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
    }

    /**
     * @return the injector
     */
    public InjectedValue<ModeShapeEngine> getModeShapeEngineInjector() {
        return engineInjector;
    }

    /**
     * @return the jcrRepositoryInjector
     */
    public InjectedValue<JcrRepository> getJcrRepositoryInjector() {
        return jcrRepositoryInjector;
    }
}
