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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import javax.jcr.Binary;
import javax.jcr.NamespaceRegistry;
import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.common.util.SecureHash.HashingInputStream;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.JcrMixLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.sequencer.Sequencer;
import org.modeshape.sequencer.teiid.VdbDataRole.Permission;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;

/**
 * A sequencer of Teiid Virtual Database (VDB) files.
 */
public class VdbSequencer extends Sequencer {

    private static final UrlEncoder URL_ENCODER = new UrlEncoder();
    private static final Pattern VERSION_REGEX = Pattern.compile("(.*)[.]\\s*[+-]?([0-9]+)\\s*$");

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.sequencer.Sequencer#execute(javax.jcr.Property, javax.jcr.Node,
     *      org.modeshape.jcr.api.sequencer.Sequencer.Context)
     */
    @Override
    public boolean execute( Property inputProperty,
                            Node outputNode,
                            Context context ) throws Exception {
        System.err.println("********outputNode name=" + outputNode.getName() + ", path=" + outputNode.getPath());
        Binary binaryValue = inputProperty.getBinary();
        CheckArg.isNotNull(binaryValue, "binary");

        ZipInputStream vdbInputStream = null;
        File vdbArchiveFile = null;

        try {
            vdbInputStream = new ZipInputStream(binaryValue.getStream());
            AtomicInteger version = new AtomicInteger(0);
            //
            // PathFactory pathFactory = context.getValueFactories().getPathFactory();
            // ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();

            // Figure out the name of the VDB archive ...
            // Path pathToArchiveFile = context.getInputPath();
            // Name zipFileName = VdbLexicon.VIRTUAL_DATABASE;
            String pathToArchiveFile = inputProperty.getPath();

            Node parent = inputProperty.getParent();
            String zipFileName = parent.getName();
            assert zipFileName.length() != 0; // if 0, then the parent is the root. WTF?
            if ("jcr:content".equals(zipFileName)) {
                // we're sequencing an 'nt:file' node, and the name of the VDB is the grand-parent of the property ...
                zipFileName = parent.getParent().getName();
            }
            assert zipFileName != null;
            assert zipFileName.length() != 0;

            // if (pathToArchiveFile != null && !pathToArchiveFile.isRoot()) {
            // // Remove the 'jcr:content' node (of type 'nt:resource'), if it is there ...
            // if (pathToArchiveFile.getLastSegment().getName().equals(JcrLexicon.CONTENT)) {
            // pathToArchiveFile = pathToArchiveFile.getParent();
            // }
            //
            // if (!pathToArchiveFile.isRoot()) {
            // zipFileName = pathToArchiveFile.getLastSegment().getName();
            // // Remove the ".xmi" extension
            // String fileNameWithoutExtension = zipFileName.getLocalName().replaceAll("\\.vdb$", "");
            // zipFileName = context.getValueFactories().getNameFactory().create(zipFileName.getNamespaceUri(),
            // fileNameWithoutExtension);
            // zipFileName = extractVersionInfomation(zipFileName, version);
            // }
            // }
            assert zipFileName != null;

            // We need to access the 'vdb.xml' file first, and then process the models in the dependency order (with physical
            // models
            // first) so that dependencies can be resolved when they're needed. Because we can't randomly access the
            // ZipEntry objects using a ZipInputStream, we need to use a ZipFile, and thus have to write out the VDB
            // archive to a temporary file...
            String prefix = "modeshape" + URL_ENCODER.encode(zipFileName);
            vdbArchiveFile = File.createTempFile(prefix, "vdb");
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(vdbArchiveFile));
            HashingInputStream hashingStream = SecureHash.createHashingStream(Algorithm.SHA_1, vdbInputStream);
            IoUtil.write(hashingStream, ostream); // closes streams ...
            String sha1 = hashingStream.getHashAsHexString();

            // TODO create zip file from stream and get rid of code just above
            // TODO set sha1 like this instead ((org.modeshape.jcr.api.Binary)binaryValue).getHexHash();

            // Now we can access the files in any order, so start with the "vdb.xml" file ...
            ZipFile vdbArchive = new ZipFile(vdbArchiveFile);
            ZipEntry vdbXml = vdbArchive.getEntry("META-INF/vdb.xml");

            if (vdbXml == null) {
                return false; // TODO handle this better
            }

            // set VDB version
            VdbManifest manifest = VdbManifest.read(vdbArchive.getInputStream(vdbXml), context);

            if (version.get() != 0) {
                // The version information was specified in the name, so override what was in the file ...
                manifest.setVersion(version.get());
            }

            // Create the output node for the VDB ...
            // Path vdbPath = pathFactory.createRelativePath(zipFileName);
            outputNode.setProperty(JcrLexicon.PRIMARY_TYPE.getString(), VdbLexicon.VIRTUAL_DATABASE);
            outputNode.setProperty(JcrLexicon.MIXIN_TYPES.getString(), JcrMixLexicon.REFERENCEABLE.getString());
            outputNode.setProperty(JcrLexicon.UUID.getString(), UUID.randomUUID().toString());
            outputNode.setProperty(VdbLexicon.VERSION, manifest.getVersion());
            outputNode.setProperty(VdbLexicon.ORIGINAL_FILE, pathToArchiveFile);
            outputNode.setProperty(ModeShapeLexicon.SHA1.getString(), sha1);
            setProperty(outputNode, VdbLexicon.DESCRIPTION, manifest.getDescription());

            // create translator child nodes
            sequenceTranslators(manifest, outputNode);

            // create data role child nodes
            sequenceDataRoles(manifest, outputNode);

            // create entry child nodes
            sequenceEntries(manifest, outputNode);

            // create properties child nodes
            sequenceProperties(manifest, outputNode);

            // create model child nodes
            sequenceModels(manifest, outputNode);

            return true;
        } catch (Exception e) {
            String location = null; // TODO set location
            getLogger().error(e, TeiidI18n.errorReadingVdbFile.text(location, e.getMessage()));
            return false;
        } finally {
            if (vdbInputStream != null) {

                try {
                    vdbInputStream.close();
                } catch (Exception e) {
                    getLogger().warn("Cannot close VDB input stream", e); // TODO i18n this
                }
            }

            if (vdbArchiveFile != null) {
                vdbArchiveFile.delete();
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
    public void initialize( NamespaceRegistry registry,
                            NodeTypeManager nodeTypeManager ) throws RepositoryException, IOException {
        registry.registerNamespace(VdbLexicon.Namespace.PREFIX, VdbLexicon.Namespace.URI);
        registry.registerNamespace(XmiLexicon.Namespace.PREFIX, XmiLexicon.Namespace.URI);
        registry.registerNamespace(CoreLexicon.Namespace.PREFIX, CoreLexicon.Namespace.URI);
        registerNodeTypes("xmi.cnd", nodeTypeManager, true);
        registerNodeTypes("mmcore.cnd", nodeTypeManager, true);
        registerNodeTypes("jdbc.cnd", nodeTypeManager, true);
        registerNodeTypes("relational.cnd", nodeTypeManager, true);
        registerNodeTypes("transformation.cnd", nodeTypeManager, true);
        registerNodeTypes("vdb.cnd", nodeTypeManager, true);
        // registerNodeTypes("teiid.cnd", nodeTypeManager, true);
    }

    /**
     * Utility method to extract the version information from a VDB filename.
     * 
     * @param fileNameWithoutExtension the filename for the VDB, without its extension; may not be null
     * @param version the reference to the AtomicInteger that will be modified to contain the version
     * @return the 'fileNameWithoutExtension' value (without any trailing '.' characters); never null
     */
    public static String extractVersionInfomation( String fileNameWithoutExtension,
                                                   AtomicInteger version ) {
        Matcher matcher = VERSION_REGEX.matcher(fileNameWithoutExtension);

        if (matcher.matches()) {
            // Extract the version number from the name ...
            fileNameWithoutExtension = matcher.group(1);
            version.set(Integer.parseInt(matcher.group(2)));
        }

        // Remove all trailing '.' characters
        return fileNameWithoutExtension.replaceAll("[.]*$", "");
    }

    private void sequenceDataRoles( VdbManifest manifest,
                                    Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        List<VdbDataRole> dataRolesGroup = manifest.getDataRoles();

        if (!dataRolesGroup.isEmpty()) {
            Node dataRolesGroupNode = outputNode.addNode(VdbLexicon.DataRole.DATA_ROLES);

            for (VdbDataRole dataRole : dataRolesGroup) {
                Node dataRoleNode = dataRolesGroupNode.addNode(VdbLexicon.DataRole.DATA_ROLE);
                setProperty(dataRoleNode, VdbLexicon.DataRole.NAME, dataRole.getName());
                dataRoleNode.setProperty(VdbLexicon.DataRole.ANY_AUTHENTICATED, dataRole.isAnyAuthenticated());
                dataRoleNode.setProperty(VdbLexicon.DataRole.ALLOW_CREATE_TEMP_TABLES, dataRole.isAllowCreateTempTables());

                // add permissions
                List<Permission> permissionsGroup = dataRole.getPermissions();

                if (!permissionsGroup.isEmpty()) {
                    Node permissionsGroupNode = dataRoleNode.addNode(VdbLexicon.DataRole.Permission.PERMISSIONS);

                    for (Permission permission : permissionsGroup) {
                        Node permissionNode = permissionsGroupNode.addNode(VdbLexicon.DataRole.Permission.PERMISSION);
                        setProperty(permissionNode, VdbLexicon.DataRole.Permission.RESOURCE_NAME, permission.getResourceName());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_ALTER, permission.canAlter());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_CREATE, permission.canCreate());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_DELETE, permission.canDelete());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_EXECUTE, permission.canExecute());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_READ, permission.canRead());
                        permissionNode.setProperty(VdbLexicon.DataRole.Permission.ALLOW_UPDATE, permission.canUpdate());
                    }
                }

                // set role names
                List<String> roleNames = dataRole.getMappedRoleNames();

                if (!roleNames.isEmpty()) {
                    dataRoleNode.setProperty(VdbLexicon.DataRole.MAPPED_ROLE_NAMES,
                                             roleNames.toArray(new String[roleNames.size()]));
                }
            }
        }
    }

    private void sequenceEntries( VdbManifest manifest,
                                  Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        List<VdbEntry> entriesGroup = manifest.getEntries();

        if (!entriesGroup.isEmpty()) {
            Node entriesGroupNode = outputNode.addNode(VdbLexicon.Entry.ENTRIES);

            for (VdbEntry entry : entriesGroup) {
                Node entryNode = entriesGroupNode.addNode(VdbLexicon.Entry.ENTRY);
                setProperty(entryNode, VdbLexicon.Entry.PATH, entry.getPath());

                // add properties
                Map<String, String> props = entry.getProperties();

                if (!props.isEmpty()) {
                    Node propertyGroupNode = entryNode.addNode(VdbLexicon.Entry.PROPERTIES);

                    for (Map.Entry<String, String> prop : props.entrySet()) {
                        setProperty(propertyGroupNode, prop.getKey(), prop.getValue());
                    }
                }
            }
        }
    }

    private void sequenceModels( VdbManifest manifest,
                                 Node outputNode ) {

        //
        // ReferenceResolver resolver = new ReferenceResolver(context);
        //
        // for (VdbModel model : manifest.modelsInDependencyOrder()) {
        // if (model.getType().equalsIgnoreCase(ModelType.PHYSICAL) || model.getType().equalsIgnoreCase(ModelType.VIRTUAL)) {
        // ModelSequencer sequencer = new ModelSequencer(model, vdbPath, resolver);
        // // TODO create model sequencer in initialize method and reuse
        // sequencer.setUseXmiUuidsAsJcrUuids(false);
        // ZipEntry modelEntry = vdbArchive.getEntry(model.getPathInVdb());
        //
        // if (modelEntry == null) {
        // // Some older VDBs have the model paths as absolute ...
        // modelEntry = vdbArchive.getEntry("/" + model.getPathInVdb());
        // }
        //
        // if (modelEntry != null) {
        // // create model sequencer method to take stream or model entry, also pass in ReferenceResolver
        // sequencer.execute(vdbArchive.getInputStream(modelEntry), outputNode, context);
        // }
        // }
        // }
    }

    private void sequenceProperties( VdbManifest manifest,
                                     Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        Map<String, String> props = manifest.getProperties();

        if (!props.isEmpty()) {
            Node propertyGroupNode = outputNode.addNode(VdbLexicon.PROPERTIES);

            for (Map.Entry<String, String> prop : props.entrySet()) {
                setProperty(propertyGroupNode, prop.getKey(), prop.getValue());
            }
        }
    }

    private void sequenceTranslators( VdbManifest manifest,
                                      Node outputNode ) throws Exception {
        assert (manifest != null) : "manifest is null";
        assert (outputNode != null) : "outputNode is null";

        List<VdbTranslator> translatorsGroup = manifest.getTranslators();

        if (!translatorsGroup.isEmpty()) {
            Node translatorsGroupNode = outputNode.addNode(VdbLexicon.Translator.TRANSLATORS);

            for (VdbTranslator translator : translatorsGroup) {
                Node translatorNode = translatorsGroupNode.addNode(VdbLexicon.Translator.TRANSLATOR);
                setProperty(translatorNode, VdbLexicon.Translator.NAME, translator.getName());
                setProperty(translatorNode, VdbLexicon.Translator.TYPE, translator.getType());
                setProperty(translatorNode, VdbLexicon.Translator.NAME, translator.getDescription());

                // add properties
                Map<String, String> props = translator.getProperties();

                if (!props.isEmpty()) {
                    Node propertyGroupNode = translatorNode.addNode(VdbLexicon.Translator.PROPERTIES);

                    for (Map.Entry<String, String> prop : props.entrySet()) {
                        setProperty(propertyGroupNode, prop.getKey(), prop.getValue());
                    }
                }
            }
        }
    }

    private void setProperty( Node node,
                              String name,
                              String value ) throws Exception {
        assert (node != null);
        assert (!StringUtil.isBlank(name));

        if (!StringUtil.isBlank(value)) {
            node.setProperty(name, value);
        }
    }
}
