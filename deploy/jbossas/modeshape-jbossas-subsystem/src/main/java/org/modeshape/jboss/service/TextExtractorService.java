/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.modeshape.jboss.service;

import javax.jcr.RepositoryException;
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
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.NoSuchRepositoryException;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryConfiguration.FieldName;
import java.util.List;
import java.util.Properties;

/**
 * {@link Service} implementation which exposes ModeShape's text extraction feature.
 */
public class TextExtractorService implements Service<JcrRepository> {

    private final InjectedValue<ModeShapeEngine> engineInjector = new InjectedValue<ModeShapeEngine>();
    private final InjectedValue<JcrRepository> jcrRepositoryInjector = new InjectedValue<JcrRepository>();

    private final Properties extractorProperties;
    private final String repositoryName;

    public TextExtractorService( String repositoryName,
                                 Properties extractorProperties ) {
        this.repositoryName = repositoryName;
        this.extractorProperties = extractorProperties;
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
        EditableDocument queryDocument = configEditor.getDocument(FieldName.QUERY);

        EditableDocument textExtracting = queryDocument.getOrCreateDocument(FieldName.TEXT_EXTRACTING);
        EditableDocument extractors = textExtracting.getOrCreateDocument(FieldName.EXTRACTORS);

        EditableDocument extractor = Schematic.newDocument();
        String sequencerName = extractorProperties.getProperty(FieldName.NAME);
        for (Object key : extractorProperties.keySet()) {
            String keyStr = (String)key;
            if (FieldName.NAME.equals(keyStr)) continue;
            Object value = extractorProperties.get(keyStr);
            if (value instanceof List<?>) {
                for (Object val : (List<?>)value) {
                    extractor.getOrCreateArray(keyStr).addValue(val);
                }
            } else {
                // Just set the value as a field
                extractor.set(keyStr, value);
            }
        }

        extractors.set(sequencerName, extractor);

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
        } catch (ConfigurationException e) {
            throw new StartException(e);
        } catch (NoSuchRepositoryException e) {
            throw new StartException(e);
        } catch (RepositoryException e) {
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
