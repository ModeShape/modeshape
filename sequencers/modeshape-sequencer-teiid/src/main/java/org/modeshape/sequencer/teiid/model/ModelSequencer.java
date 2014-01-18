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
package org.modeshape.sequencer.teiid.model;

import java.io.IOException;
import java.io.InputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.teiid.TeiidI18n;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.DiagramLexicon;
import org.modeshape.sequencer.teiid.lexicon.RelationalLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;

/**
 * A sequencer of Teiid XMI model files.
 */
public class ModelSequencer extends Sequencer {

    private static final String[] MODEL_FILE_EXTENSIONS = { ".xmi" };
    private static final Logger LOGGER = Logger.getLogger(ModelSequencer.class);

    /**
     * @param modelReader the reader who processed the model file (cannot be <code>null</code>)
     * @return <code>true</code> if the model process by the reader should be sequenced
     */
    public static boolean shouldSequence( final ModelReader modelReader ) {
        assert (modelReader != null);

        final String modelType = modelReader.getModelType();
        final boolean validModelType = CoreLexicon.ModelType.PHYSICAL.equalsIgnoreCase(modelType)
                || CoreLexicon.ModelType.VIRTUAL.equalsIgnoreCase(modelType);
        return (validModelType && RelationalLexicon.Namespace.URI.equals(modelReader.getPrimaryMetamodelUri()));
    }

    private final ReferenceResolver resolver;

    /**
     * Constructs a sequencer and an internal reference resolver.
     */
    public ModelSequencer() {
        this.resolver = new ReferenceResolver();
    }

    /**
     * @param resolver the reference resolver to use during sequencing (cannot be <code>null</code>)
     */
    public ModelSequencer( final ReferenceResolver resolver ) {
        CheckArg.isNotNull(resolver, "resolver");
        this.resolver = resolver;
    }

    /**
     * @see org.modeshape.jcr.api.sequencer.Sequencer#execute(javax.jcr.Property, javax.jcr.Node,
     *      org.modeshape.jcr.api.sequencer.Sequencer.Context)
     */
    @Override
    public boolean execute( final Property inputProperty,
                            final Node outputNode,
                            final Context context ) throws Exception {
        final Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        outputNode.addMixin(CoreLexicon.JcrId.MODEL);

        InputStream modelStream = null;

        try {
            modelStream = binaryValue.getStream();
            return sequenceModel(modelStream, outputNode, outputNode.getPath(), null, context);
        } finally {
            if (modelStream != null) {
                modelStream.close();
            }
        }
    }

    /**
     * @param resourceName the name of the resource being checked (cannot be <code>null</code>)
     * @return <code>true</code> if the resource has a model file extension
     */
    public boolean hasModelFileExtension( final String resourceName ) {
        for (final String extension : MODEL_FILE_EXTENSIONS) {
            if (resourceName.endsWith(extension)) {
                return true;
            }
        }

        // not a model file
        return false;
    }

    /**
     * @see org.modeshape.jcr.api.sequencer.Sequencer#initialize(javax.jcr.NamespaceRegistry,
     *      org.modeshape.jcr.api.nodetype.NodeTypeManager)
     */
    @Override
    public void initialize( final NamespaceRegistry registry,
                            final NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        LOGGER.debug("enter initialize");

        super.registerNodeTypes("../xmi.cnd", nodeTypeManager, true);
        LOGGER.debug("xmi.cnd loaded");

        super.registerNodeTypes("../med.cnd", nodeTypeManager, true);
        LOGGER.debug("med.cnd loaded");

        super.registerNodeTypes("../mmcore.cnd", nodeTypeManager, true);
        LOGGER.debug("mmcore.cnd loaded");

        super.registerNodeTypes("../jdbc.cnd", nodeTypeManager, true);
        LOGGER.debug("jdbc.cnd loaded");

        super.registerNodeTypes("../relational.cnd", nodeTypeManager, true);
        LOGGER.debug("relational.cnd loaded");

        super.registerNodeTypes("../transformation.cnd", nodeTypeManager, true);
        LOGGER.debug("transformation.cnd loaded");

        // Register some of the namespaces we'll need ...
        registerNamespace(DiagramLexicon.Namespace.PREFIX, DiagramLexicon.Namespace.URI, registry);

        LOGGER.debug("exit initialize");
    }

    /**
     * The method that performs the sequencing.
     *
     * @param modelStream the input stream of the model file (cannot be <code>null</code>)
     * @param modelOutputNode the root node of the model being sequenced (cannot be <code>null</code>)
     * @param modelPath the model path including the model name (cannot be <code>null</code> or empty)
     * @param vdbModel the VDB model associated with the input stream (cannot be <code>null</code>)
     * @param context the sequencer context (cannot be <code>null</code>)
     * @return <code>true</code> if the model file input stream was successfully sequenced
     * @throws Exception if there is a problem during sequencing
     */
    private boolean sequenceModel( final InputStream modelStream,
                                   final Node modelOutputNode,
                                   final String modelPath,
                                   final VdbModel vdbModel,
                                   final Context context ) throws Exception {
        assert (modelStream != null);
        assert (modelOutputNode != null);
        assert (context != null);
        assert (modelOutputNode.isNodeType(CoreLexicon.JcrId.MODEL));

        LOGGER.debug("sequenceModel:model node path='{0}', model path='{1}', vdb model='{2}'",
                     modelOutputNode.getPath(),
                     modelPath,
                     vdbModel);

        final NamespaceRegistry registry = modelOutputNode.getSession().getWorkspace().getNamespaceRegistry();
        final ModelReader modelReader = new ModelReader(modelPath, this.resolver, registry);
        modelReader.readModel(modelStream);

        if (shouldSequence(modelReader)) {
            final ModelNodeWriter nodeWriter = new ModelNodeWriter(modelOutputNode, modelReader, this.resolver, vdbModel,
                                                                   context);
            return nodeWriter.write();
        }

        // stream was not sequenced
        LOGGER.debug("sequenceModel:model not sequenced at path '{0}'", modelPath);
        return false;
    }

    /**
     * Used only by the VDB sequencer to sequence a model file contained in a VDB.
     *
     * @param modelStream the input stream of the model file (cannot be <code>null</code>)
     * @param modelOutputNode the root node of the model being sequenced (cannot be <code>null</code>)
     * @param vdbModel the VDB model associated with the input stream (cannot be <code>null</code>)
     * @param context the sequencer context (cannot be <code>null</code>)
     * @return <code>true</code> if the model file input stream was successfully sequenced
     * @throws Exception if there is a problem during sequencing or node does not have a VDB model primary type
     */
    public boolean sequenceVdbModel( final InputStream modelStream,
                                     final Node modelOutputNode,
                                     final VdbModel vdbModel,
                                     final Context context ) throws Exception {
        CheckArg.isNotNull(modelStream, "modelStream");
        CheckArg.isNotNull(modelOutputNode, "modelOutputNode");
        CheckArg.isNotNull(vdbModel, "vdbModel");

        if (!modelOutputNode.isNodeType(VdbLexicon.Model.MODEL)) {
            throw new RuntimeException(TeiidI18n.invalidVdbModelNodeType.text(modelOutputNode.getPath()));
        }

        return sequenceModel(modelStream, modelOutputNode, vdbModel.getPathInVdb(), vdbModel, context);
    }
}
