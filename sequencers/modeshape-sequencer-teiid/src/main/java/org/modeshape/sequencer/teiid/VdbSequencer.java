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
package org.modeshape.sequencer.teiid;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrConstants;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.teiid.VdbDataRole.Condition;
import org.modeshape.sequencer.teiid.VdbDataRole.Mask;
import org.modeshape.sequencer.teiid.VdbDataRole.Permission;
import org.modeshape.sequencer.teiid.VdbModel.Source;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.model.ModelSequencer;
import org.modeshape.sequencer.teiid.model.ReferenceResolver;

/**
 * A sequencer of Teiid Virtual Database (VDB) files.
 */
public class VdbSequencer extends Sequencer {

    protected static final Logger LOGGER = Logger.getLogger(VdbSequencer.class);
    private static final String MANIFEST_FILE = "META-INF/vdb.xml";
    private static final Pattern VERSION_REGEX = Pattern.compile("(.*)[.]\\s*[+-]?([0-9]+)\\s*$");

    /**
     * Utility method to extract the version information from a VDB filename.
     *
     * @param fileNameWithoutExtension the filename for the VDB, without its extension; may not be null
     * @param version the reference to the AtomicInteger that will be modified to contain the version
     * @return the 'fileNameWithoutExtension' value (without any trailing '.' characters); never null
     */
    public static String extractVersionInfomation( String fileNameWithoutExtension,
                                                   final AtomicInteger version ) {
        final Matcher matcher = VERSION_REGEX.matcher(fileNameWithoutExtension);

        if (matcher.matches()) {
            // Extract the version number from the name ...
            fileNameWithoutExtension = matcher.group(1);
            version.set(Integer.parseInt(matcher.group(2)));
        }

        // Remove all trailing '.' characters
        return fileNameWithoutExtension.replaceAll("[.]*$", "");
    }

    private ModelSequencer modelSequencer; // constructed during initialize method

    private ReferenceResolver resolver; // constructed during initialize method

    /**
     * @see org.modeshape.jcr.api.sequencer.Sequencer#execute(javax.jcr.Property, javax.jcr.Node,
     *      org.modeshape.jcr.api.sequencer.Sequencer.Context)
     */
    @Override
    public boolean execute( final Property inputProperty,
                            final Node outputNode,
                            final Context context ) throws Exception {
        LOGGER.debug("VdbSequencer.execute called:outputNode name='{0}', path='{1}'", outputNode.getName(), outputNode.getPath());

        final Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        try (final ZipInputStream vdbStream = new ZipInputStream(binaryValue.getStream())) {
            VdbManifest manifest = null;
            ZipEntry entry = null;

            while ((entry = vdbStream.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.endsWith(MANIFEST_FILE)) {
                    manifest = readManifest(binaryValue, vdbStream, outputNode, context);
                } else if (!entry.isDirectory() && this.modelSequencer.hasModelFileExtension(entryName)) {
                    LOGGER.debug("----before reading model '{0}'", entryName);

                    // vdb.xml file should be read first in stream so manifest model should be available
                    if (manifest == null) {
                        throw new Exception("VDB manifest file has not been read");
                    }

                    final VdbModel vdbModel = manifest.getModel(entryName);

                    if (vdbModel == null) {
                        throw new Exception("Model '" + entryName + "' was not found");
                    }

                    // call sequencer here after creating node for last part of entry name
                    final int index = entryName.lastIndexOf('/') + 1;

                    if ((index != -1) && (index < entryName.length())) {
                        entryName = entryName.substring(index);
                    }

                    final Node modelNode = outputNode.addNode(entryName, VdbLexicon.Vdb.MODEL);
                    final boolean sequenced = this.modelSequencer.sequenceVdbModel(vdbStream, modelNode, vdbModel, context);

                    if (!sequenced) {
                        modelNode.remove();
                        LOGGER.debug(">>>>model NOT sequenced '{0}'\n\n", entryName);
                    } else {
                        LOGGER.debug(">>>>done sequencing model '{0}'\n\n", entryName);
                    }
                } else {
                    LOGGER.debug("----ignoring resource '{0}'", entryName);
                }
            }

            return true;
        } catch (final Exception e) {
            throw new RuntimeException(TeiidI18n.errorReadingVdbFile.text(inputProperty.getPath(), e.getMessage()), e);
        }
    }

    protected VdbManifest readManifest(Binary binaryValue, InputStream inputStream, Node outputNode, Context context) throws Exception {
        VdbManifest manifest;
        LOGGER.debug("----before reading vdb.xml");

        manifest = VdbManifest.read(inputStream, context);
        assert (manifest != null) : "manifest is null";

        // Create the output node for the VDB ...
        outputNode.setPrimaryType(VdbLexicon.Vdb.VIRTUAL_DATABASE);
        outputNode.addMixin(JcrConstants.MIX_REFERENCEABLE);
        outputNode.setProperty(VdbLexicon.Vdb.VERSION, manifest.getVersion());
        outputNode.setProperty(VdbLexicon.Vdb.ORIGINAL_FILE, outputNode.getPath());
        outputNode.setProperty(JcrConstants.MODE_SHA1, ((org.modeshape.jcr.api.Binary)binaryValue).getHexHash());
        setProperty(outputNode, VdbLexicon.Vdb.NAME, manifest.getName());
        setProperty(outputNode, VdbLexicon.Vdb.DESCRIPTION, manifest.getDescription());
        setProperty(outputNode, VdbLexicon.Vdb.CONNECTION_TYPE, manifest.getConnectionType());

        // create imported VDBs child nodes
        sequenceImportVdbs(manifest, outputNode);

        // create translator child nodes
        sequenceTranslators(manifest, outputNode);

        // create data role child nodes
        sequenceDataRoles(manifest, outputNode);

        // create entry child nodes
        sequenceEntries(manifest, outputNode);

        // create properties child nodes
        sequenceProperties(manifest, outputNode);

        // create child nodes for declarative models
        sequenceDeclarativeModels(manifest, outputNode);

        LOGGER.debug(">>>>done reading vdb.xml\n\n");
        return manifest;
    }

    /**
     * @throws IOException
     * @see org.modeshape.jcr.api.sequencer.Sequencer#initialize(javax.jcr.NamespaceRegistry,
     *      org.modeshape.jcr.api.nodetype.NodeTypeManager)
     */
    @Override
    public void initialize( final NamespaceRegistry registry,
                            final NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        LOGGER.debug("enter initialize");
//        registry.registerNamespace(VdbLexicon.Namespace.PREFIX, VdbLexicon.Namespace.URI);
//        registry.registerNamespace(XmiLexicon.Namespace.PREFIX, XmiLexicon.Namespace.URI);
//        registry.registerNamespace(CoreLexicon.Namespace.PREFIX, CoreLexicon.Namespace.URI);
        registerNodeTypes("xmi.cnd", nodeTypeManager, true);
        LOGGER.debug("xmi.cnd loaded");

        registerNodeTypes("med.cnd", nodeTypeManager, true);
        LOGGER.debug("med.cnd loaded");

        registerNodeTypes("mmcore.cnd", nodeTypeManager, true);
        LOGGER.debug("mmcore.cnd loaded");

        registerNodeTypes("vdb.cnd", nodeTypeManager, true);
        LOGGER.debug("vdb.cnd loaded");

        this.resolver = new ReferenceResolver();
        this.modelSequencer = new ModelSequencer(this.resolver);
        this.modelSequencer.initialize(registry, nodeTypeManager);

        LOGGER.debug("exit initialize");
    }

    /**
     * @param manifest the VDB manifest whose data roles are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB node (cannot be <code>null</code>)
     * @throws Exception if an error occurs writing the data roles
     */
    private void sequenceDataRoles( final VdbManifest manifest,
                                    final Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        final List<VdbDataRole> dataRolesGroup = manifest.getDataRoles();

        if (!dataRolesGroup.isEmpty()) {
            final Node dataRolesGroupNode = outputNode.addNode(VdbLexicon.Vdb.DATA_ROLES, VdbLexicon.Vdb.DATA_ROLES);

            for (final VdbDataRole dataRole : dataRolesGroup) {
                final Node dataRoleNode = dataRolesGroupNode.addNode(dataRole.getName(), VdbLexicon.DataRole.DATA_ROLE);
                setProperty(dataRoleNode, VdbLexicon.DataRole.DESCRIPTION, dataRole.getDescription());
                dataRoleNode.setProperty(VdbLexicon.DataRole.ANY_AUTHENTICATED, dataRole.isAnyAuthenticated());
                dataRoleNode.setProperty(VdbLexicon.DataRole.ALLOW_CREATE_TEMP_TABLES, dataRole.isAllowCreateTempTables());
                dataRoleNode.setProperty(VdbLexicon.DataRole.GRANT_ALL, dataRole.isGrantAll());

                // set role names
                final List<String> roleNames = dataRole.getMappedRoleNames();

                if (!roleNames.isEmpty()) {
                    dataRoleNode.setProperty(VdbLexicon.DataRole.MAPPED_ROLE_NAMES,
                                             roleNames.toArray(new String[roleNames.size()]));
                }

                // add permissions
                final List<Permission> permissionsGroup = dataRole.getPermissions();

                if (!permissionsGroup.isEmpty()) {
                    final Node permissionsGroupNode = dataRoleNode.addNode(VdbLexicon.DataRole.PERMISSIONS,
                                                                           VdbLexicon.DataRole.PERMISSIONS);

                    for (final Permission permission : permissionsGroup) {
                        final Node permissionNode = permissionsGroupNode.addNode(permission.getResourceName(),
                                                                                 VdbLexicon.DataRole.Permission.PERMISSION);
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER, permission.canAlter());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE, permission.canCreate());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE, permission.canDelete());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE, permission.canExecute());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_READ, permission.canRead());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE, permission.canUpdate());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_LANGUAGE, permission.useLanguage());

                        // add permission's conditions
                        List<Condition> conditions = permission.getConditions();
                        if (! conditions.isEmpty()) {
                            final Node conditionsGroupNode = permissionNode.addNode(VdbLexicon.DataRole.Permission.CONDITIONS,
                                                                                    VdbLexicon.DataRole.Permission.CONDITIONS);

                            for (final Condition condition : conditions) {
                                Node conditionNode = conditionsGroupNode.addNode(condition.getRule(),
                                                                                 VdbLexicon.DataRole.Permission.Condition.CONDITION);
                                conditionNode.setProperty(VdbLexicon.DataRole.Permission.Condition.CONSTRAINT, condition.isConstraint());
                            }
                        }

                        // add add permission's masks
                        List<Mask> masks = permission.getMasks();
                        if (! masks.isEmpty()) {
                            final Node masksGroupNode = permissionNode.addNode(VdbLexicon.DataRole.Permission.MASKS,
                                                                                    VdbLexicon.DataRole.Permission.MASKS);

                            for (final Mask mask : masks) {
                                Node maskNode = masksGroupNode.addNode(mask.getRule(),
                                                                                 VdbLexicon.DataRole.Permission.Mask.MASK);
                                maskNode.setProperty(VdbLexicon.DataRole.Permission.Mask.ORDER, mask.getOrder());
                            }
                        }

                    }
                }
            }
        }
    }

    /**
     * @param manifest the VDB manifest whose declarative models are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB node (cannot be <code>null</code>)
     * @throws Exception if an error occurs writing the VDB declarative models
     */
    private void sequenceDeclarativeModels( final VdbManifest manifest,
                                            final Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        for (final VdbModel model : manifest.getModels()) {
            // if there is metadata then there is no xmi file
            if (model.isDeclarative()) {
                LOGGER.debug(">>>>writing declarative model '{0}'", model.getName());

                final Node modelNode = outputNode.addNode(model.getName(), VdbLexicon.Vdb.DECLARATIVE_MODEL);

                // set vdb:abstractModel properties
                setProperty(modelNode, VdbLexicon.Model.DESCRIPTION, model.getDescription());
                modelNode.setProperty(VdbLexicon.Model.VISIBLE, model.isVisible());
                setProperty(modelNode, VdbLexicon.Model.PATH_IN_VDB, model.getPathInVdb());

                // set vdb:declarativeModel properties
                setProperty(modelNode, CoreLexicon.JcrId.MODEL_TYPE, model.getType());
                setProperty(modelNode, VdbLexicon.Model.METADATA_TYPE, model.getMetadataType());
                setProperty(modelNode, VdbLexicon.Model.MODEL_DEFINITION, model.getModelDefinition());

                // set model sources
                List<Source> sources = model.getSources();
                if (! sources.isEmpty()) {
                    Node modelSourcesGroupNode = modelNode.addNode(VdbLexicon.Vdb.SOURCES, VdbLexicon.Vdb.SOURCES);

                    for (final VdbModel.Source source : sources) {
                        Node sourceNode = modelSourcesGroupNode.addNode(source.getName(), VdbLexicon.Source.SOURCE);
                        sourceNode.setProperty(VdbLexicon.Source.TRANSLATOR, source.getTranslator());
                        sourceNode.setProperty(VdbLexicon.Source.JNDI_NAME, source.getJndiName());
                    }
                }

                for (Map.Entry<String, String> entry : model.getProperties().entrySet()) {
                    setProperty(modelNode, entry.getKey(), entry.getValue());
                }
            }
        }
    }

    /**
     * @param manifest the VDB manifest whose entries are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB node (cannot be <code>null</code>)
     * @throws Exception if an error occurs writing the VDB entries
     */
    private void sequenceEntries( final VdbManifest manifest,
                                  final Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        final List<VdbEntry> entriesGroup = manifest.getEntries();

        if (!entriesGroup.isEmpty()) {
            final Node entriesGroupNode = outputNode.addNode(VdbLexicon.Vdb.ENTRIES, VdbLexicon.Vdb.ENTRIES);

            for (final VdbEntry entry : entriesGroup) {
                final Node entryNode = entriesGroupNode.addNode(VdbLexicon.Entry.ENTRY, VdbLexicon.Entry.ENTRY);
                setProperty(entryNode, VdbLexicon.Entry.PATH, entry.getPath());
                setProperty(entryNode, VdbLexicon.Entry.DESCRIPTION, entry.getDescription());

                // add properties
                final Map<String, String> props = entry.getProperties();

                if (!props.isEmpty()) {
                    for (final Map.Entry<String, String> prop : props.entrySet()) {
                        setProperty(entryNode, prop.getKey(), prop.getValue());
                    }
                }
            }
        }
    }

    /**
     * @param manifest the VDB manifest whose imported VDBs are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB output node where import VDB child nodes will be created (cannot be <code>null</code>)
     * @throws Exception if an error occurs creating nodes or setting properties
     */
    private void sequenceImportVdbs( final VdbManifest manifest,
                                     final Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        final List<ImportVdb> importVdbsGroup = manifest.getImportVdbs();

        if (!importVdbsGroup.isEmpty()) {
            final Node importVdbsGroupNode = outputNode.addNode(VdbLexicon.Vdb.IMPORT_VDBS, VdbLexicon.Vdb.IMPORT_VDBS);

            for (final ImportVdb importVdb : importVdbsGroup) {
                final Node importVdbNode = importVdbsGroupNode.addNode(importVdb.getName(), VdbLexicon.ImportVdb.IMPORT_VDB);
                importVdbNode.setProperty(VdbLexicon.ImportVdb.VERSION, importVdb.getVersion());
                importVdbNode.setProperty(VdbLexicon.ImportVdb.IMPORT_DATA_POLICIES, importVdb.isImportDataPolicies());
            }
        }
    }

    /**
     * @param manifest the VDB manifest whose properties are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB node where the properties will be added (cannot be <code>null</code>)
     * @throws Exception if an error occurs writing the properties
     */
    private void sequenceProperties( final VdbManifest manifest,
                                     final Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        final Map<String, String> props = manifest.getProperties();

        if (!props.isEmpty()) {
            for (final Map.Entry<String, String> prop : props.entrySet()) {
                if (VdbLexicon.ManifestIds.PREVIEW.equals(prop.getKey())) {
                    outputNode.setProperty(VdbLexicon.Vdb.PREVIEW, Boolean.parseBoolean(prop.getValue()));
                } else {
                    setProperty(outputNode, prop.getKey(), prop.getValue());
                }
            }
        }
    }

    /**
     * @param manifest the VDB manifest whose translators are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB output node where translators child nodes will be created (cannot be <code>null</code>)
     * @throws Exception if an error occurs creating nodes or setting properties
     */
    private void sequenceTranslators( final VdbManifest manifest,
                                      final Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        final List<VdbTranslator> translatorsGroup = manifest.getTranslators();

        if (!translatorsGroup.isEmpty()) {
            final Node translatorsGroupNode = outputNode.addNode(VdbLexicon.Vdb.TRANSLATORS, VdbLexicon.Vdb.TRANSLATORS);

            for (final VdbTranslator translator : translatorsGroup) {
                final Node translatorNode = translatorsGroupNode.addNode(translator.getName(),
                                                                         VdbLexicon.Translator.TRANSLATOR);
                setProperty(translatorNode, VdbLexicon.Translator.TYPE, translator.getType());
                setProperty(translatorNode, VdbLexicon.Translator.DESCRIPTION, translator.getDescription());

                // add properties
                final Map<String, String> props = translator.getProperties();

                if (!props.isEmpty()) {
                    for (final Map.Entry<String, String> prop : props.entrySet()) {
                        setProperty(translatorNode, prop.getKey(), prop.getValue());
                    }
                }
            }
        }
    }

    /**
     * Sets a property value only if the value is not <code>null</code> and not empty.
     *
     * @param node the node whose property is being set (cannot be <code>null</code>)
     * @param name the property name (cannot be <code>null</code>)
     * @param value the property value (can be <code>null</code> or empty)
     * @throws Exception if an error occurs setting the node property
     */
    private void setProperty( final Node node,
                              final String name,
                              final String value ) throws Exception {
        assert (node != null);
        assert (!StringUtil.isBlank(name));

        if (!StringUtil.isBlank(value)) {
            node.setProperty(name, value);
        }
    }
}
