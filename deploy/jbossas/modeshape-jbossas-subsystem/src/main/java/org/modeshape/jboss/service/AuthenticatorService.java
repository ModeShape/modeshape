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
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jboss.security.JBossDomainAuthenticationProvider;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.document.Changes;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Editor;

/**
 * {@link Service} implementation which exposes ModeShape's custom authenticators.
 */
public class AuthenticatorService implements Service<JcrRepository> {

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<>();
    private final InjectedValue<JcrRepository> jcrRepositoryInjector = new InjectedValue<>();

    private final Properties authenticatorProperties;
    private final String repositoryName;

    public AuthenticatorService( String repositoryName,
                                 Properties extractorProperties ) {
        this.repositoryName = repositoryName;
        this.authenticatorProperties = extractorProperties;
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
        EditableDocument security = configEditor.getOrCreateDocument(FieldName.SECURITY);
        EditableArray providers = security.getOrCreateArray(FieldName.PROVIDERS);

        EditableDocument provider = Schematic.newDocument();
        String providerName = authenticatorProperties.getProperty(FieldName.NAME);
        provider.set(FieldName.NAME, providerName);
        for (Object key : authenticatorProperties.keySet()) {
            String keyStr = (String)key;
            if (FieldName.NAME.equals(keyStr)) continue;
            Object value = authenticatorProperties.get(keyStr);
            if (value instanceof List<?>) {
                for (Object val : (List<?>)value) {
                    provider.getOrCreateArray(keyStr).addValue(val);
                }
            } else {
                // Just set the value as a field
                provider.set(keyStr, value);
            }
        }
        
        // compute the position of the provider in the list of existing providers, making sure any custom providers go first
        int providerPosition = 0;
        for (int i = 0; i < providers.size(); i++) {
            providerPosition = i;
            Document document = (Document)providers.get(i);
            String className = document.getString(FieldName.CLASSNAME);
            boolean isBuiltinProvider = !StringUtil.isBlank(className) && 
                                        (className.equalsIgnoreCase(JBossDomainAuthenticationProvider.class.getName()) ||
                                        className.equalsIgnoreCase("servlet"));
            if (isBuiltinProvider) {
                break;
            }
        }
        providers.add(providerPosition, provider);

        // Get the changes and validate them ...
        Changes changes = configEditor.getChanges();
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
