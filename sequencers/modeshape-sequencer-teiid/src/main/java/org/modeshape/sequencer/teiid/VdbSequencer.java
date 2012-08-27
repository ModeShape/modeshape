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
package org.modeshape.sequencer.teiid;

import java.io.IOException;
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
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrMixLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.teiid.VdbDataRole.Permission;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;
import org.modeshape.sequencer.teiid.model.ModelSequencer;
import org.modeshape.sequencer.teiid.model.ReferenceResolver;

/**
 * A sequencer of Teiid Virtual Database (VDB) files.
 */
public class VdbSequencer extends Sequencer {

    private static final boolean DEBUG = true;
    private static final String MANIFEST_FILE = "/META-INF/vdb.xml";
    private static final Pattern VERSION_REGEX = Pattern.compile("(.*)[.]\\s*[+-]?([0-9]+)\\s*$");

    private static void debug( final String message ) {
        System.err.println(message);
    }

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

    private ModelSequencer modelSequencer;

    private ReferenceResolver resolver;

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.sequencer.Sequencer#execute(javax.jcr.Property, javax.jcr.Node,
     *      org.modeshape.jcr.api.sequencer.Sequencer.Context)
     */
    @Override
    public boolean execute( final Property inputProperty,
                            final Node outputNode,
                            final Context context ) throws Exception {
        if (DEBUG) {
            debug("VdbSequencer.execute called:outputNode name=" + outputNode.getName() + ", path=" + outputNode.getPath());
        }

        final Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");
        ZipInputStream vdbStream = null;

        try {
            vdbStream = new ZipInputStream(binaryValue.getStream());
            VdbManifest manifest = null;
            ZipEntry entry = null;

            while ((entry = vdbStream.getNextEntry()) != null) {
                String entryName = entry.getName();

                if (entryName.endsWith(MANIFEST_FILE)) {
                    if (DEBUG) {
                        debug("----before reading vdb.xml");
                    }

                    manifest = VdbManifest.read(vdbStream, context);
                    assert (manifest != null) : "manifest is null";

                    // Create the output node for the VDB ...
                    // Path vdbPath = pathFactory.createRelativePath(zipFileName);
                    outputNode.setPrimaryType(VdbLexicon.Vdb.VIRTUAL_DATABASE);
                    outputNode.addMixin(JcrMixLexicon.REFERENCEABLE.getString());
                    outputNode.setProperty(VdbLexicon.Vdb.VERSION, manifest.getVersion());
                    outputNode.setProperty(VdbLexicon.Vdb.ORIGINAL_FILE, outputNode.getPath());
                    outputNode.setProperty(ModeShapeLexicon.SHA1.getString(),
                                           ((org.modeshape.jcr.api.Binary)binaryValue).getHexHash());
                    setProperty(outputNode, VdbLexicon.Vdb.DESCRIPTION, manifest.getDescription());

                    // create translator child nodes
                    sequenceTranslators(manifest, outputNode);

                    // create data role child nodes
                    sequenceDataRoles(manifest, outputNode);

                    // create entry child nodes
                    sequenceEntries(manifest, outputNode);

                    // create properties child nodes
                    sequenceProperties(manifest, outputNode);

                    if (DEBUG) {
                        debug(">>>>done reading vdb.xml\n\n");
                    }
                } else if (!entry.isDirectory() && this.modelSequencer.hasModelFileExtension(entryName)) {
                    if (DEBUG) {
                        debug("----before reading model " + entryName);
                    }

                    VdbModel vdbModel = null;

                    // vdb.xml file should be read first in stream so manifest model should be available
                    if (manifest != null) {
                        // remove VDB name from entryName
                        String pathInVdb = entryName;
                        final int index = entryName.indexOf('/');

                        if (index != -1) {
                            pathInVdb = entryName.substring(index);
                        }

                        vdbModel = manifest.getModel(pathInVdb);
                    }

                    // call sequencer here after creating node for last part of entry name
                    final int index = entryName.lastIndexOf('/') + 1;

                    if ((index != -1) && (index < entryName.length())) {
                        entryName = entryName.substring(index);
                    }

                    final Node modelNode = outputNode.addNode(entryName, VdbLexicon.Vdb.MODEL);

                    this.modelSequencer.sequenceVdbModel(vdbStream, modelNode, vdbModel, context);

                    if (DEBUG) {
                        debug(">>>>done reading model " + entryName + "\n\n");
                    }
                } else if (DEBUG) {
                    debug("----ignoring resource " + entryName);
                }
            }

            return true;
        } catch (final Exception e) {
            final String location = null; // TODO set location
            getLogger().error(e, TeiidI18n.errorReadingVdbFile.text(location, e.getMessage()));
            return false;
        } finally {
            if (vdbStream != null) {
                try {
                    vdbStream.close();
                } catch (final Exception e) {
                    getLogger().warn("Cannot close VDB zip input stream", e); // TODO i18n this
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IOException
     * @see org.modeshape.jcr.api.sequencer.Sequencer#initialize(javax.jcr.NamespaceRegistry,
     *      org.modeshape.jcr.api.nodetype.NodeTypeManager)
     */
    @Override
    public void initialize( final NamespaceRegistry registry,
                            final NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registry.registerNamespace(VdbLexicon.Namespace.PREFIX, VdbLexicon.Namespace.URI);
        registry.registerNamespace(XmiLexicon.Namespace.PREFIX, XmiLexicon.Namespace.URI);
        registry.registerNamespace(CoreLexicon.Namespace.PREFIX, CoreLexicon.Namespace.URI);
        registerNodeTypes("xmi.cnd", nodeTypeManager, true);
        registerNodeTypes("mmcore.cnd", nodeTypeManager, true);
        registerNodeTypes("vdb.cnd", nodeTypeManager, true);

        this.resolver = new ReferenceResolver();
        this.modelSequencer = new ModelSequencer(this.resolver);
        this.modelSequencer.initialize(registry, nodeTypeManager);
    }

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
                    }
                }
            }
        }
    }

    /**
     * @param manifest the VDB manifest whose entries are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB node
     * @throws Exception
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
     * @param manifest the VDB manifest whose properties are being sequenced (cannot be <code>null</code>)
     * @param outputNode the VDB node where the properties will be added (cannot be <code>null</code>)
     * @throws Exception if an error occurs setting properties
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
                final Node translatorNode = translatorsGroupNode.addNode(translator.getName(), VdbLexicon.Translator.TRANSLATOR);
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
