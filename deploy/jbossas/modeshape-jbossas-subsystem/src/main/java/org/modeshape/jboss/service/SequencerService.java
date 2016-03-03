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
import javax.jcr.RepositoryException;
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
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;

public class SequencerService implements Service<JcrRepository> {

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<ModeShapeEngine>();
    private final InjectedValue<JcrRepository> jcrRepositoryInjector = new InjectedValue<JcrRepository>();

    private final Properties sequencerProperties;
    private final String repositoryName;

    public SequencerService( String repositoryName,
                             Properties sequencerProperties ) {
        this.repositoryName = repositoryName;
        this.sequencerProperties = sequencerProperties;
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

        EditableDocument sequencing = editor.getOrCreateDocument(FieldName.SEQUENCING);
        EditableDocument sequencers = sequencing.getOrCreateDocument(FieldName.SEQUENCERS);

        EditableDocument seq = Schematic.newDocument();
        String sequencerName = sequencerProperties.getProperty(FieldName.NAME);
        for (Object key : sequencerProperties.keySet()) {
            String keyStr = (String)key;
            if (FieldName.NAME.equals(keyStr)) continue;
            Object value = sequencerProperties.get(keyStr);
            if (value instanceof List<?>) {
                for (Object val : (List<?>)value) {
                    seq.getOrCreateArray(keyStr).addValue(val);
                }
            } else {
                // Just set the value as a field
                seq.set(keyStr, value);
            }
        }

        sequencers.set(sequencerName, seq);

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
        } catch (ConfigurationException | RepositoryException e) {
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
