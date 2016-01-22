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
import org.modeshape.schematic.document.Changes;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Editor;
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
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class IndexProviderService implements Service<JcrRepository> {

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<ModeShapeEngine>();
    private final InjectedValue<JcrRepository> jcrRepositoryInjector = new InjectedValue<JcrRepository>();

    private final Properties providerProperties;
    private final String repositoryName;

    public IndexProviderService( String repositoryName,
                                 Properties providerProperties ) {
        this.repositoryName = repositoryName;
        this.providerProperties = providerProperties;
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

        Editor editor = repositoryConfig.edit();

        EditableDocument providers = editor.getOrCreateDocument(FieldName.INDEX_PROVIDERS);

        EditableDocument provider = Schematic.newDocument();
        String providerName = providerProperties.getProperty(FieldName.NAME);
        for (Object key : providerProperties.keySet()) {
            String keyStr = (String)key;
            if (FieldName.NAME.equals(keyStr)) continue;
            Object value = providerProperties.get(keyStr);
            if (value instanceof List<?>) {
                for (Object val : (List<?>)value) {
                    provider.getOrCreateArray(keyStr).addValue(val);
                }
            } else {
                // Just set the value as a field
                provider.set(keyStr, value);
            }
        }

        providers.set(providerName, provider);

        // Get the changes and validate them ...
        Changes changes = editor.getChanges();
        Problems validationResults = repositoryConfig.validate(changes);

        if (validationResults.hasErrors()) {
            String msg = JcrI18n.errorsInRepositoryConfiguration.text(this.repositoryName,
                                                                      validationResults.errorCount(),
                                                                      validationResults.toString());
            throw new StartException(msg);
        }
        // Update the deployed repository's configuration with these changes
        try {
            engine.update(this.repositoryName, changes);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    @Override
    public void stop( StopContext arg0 ) {
    }

    public InjectedValue<ModeShapeEngine> getModeShapeEngineInjector() {
        return engineInjector;
    }

    public InjectedValue<JcrRepository> getJcrRepositoryInjector() {
        return jcrRepositoryInjector;
    }

}
