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

package org.modeshape.jboss.service;

import java.util.List;
import java.util.Properties;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Editor;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
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
