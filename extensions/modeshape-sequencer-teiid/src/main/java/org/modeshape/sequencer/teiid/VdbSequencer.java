/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.SecureHash;
import org.modeshape.common.util.SecureHash.Algorithm;
import org.modeshape.common.util.SecureHash.HashingInputStream;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.JcrMixLexicon;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.sequencer.SequencerOutput;
import org.modeshape.graph.sequencer.StreamSequencer;
import org.modeshape.graph.sequencer.StreamSequencerContext;
import org.modeshape.sequencer.teiid.VdbModel.ModelType;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import org.modeshape.sequencer.teiid.lexicon.XmiLexicon;

/**
 * A sequencer of Teiid Virtual Database (VDB) files.
 */
public class VdbSequencer implements StreamSequencer {

    private static final UrlEncoder URL_ENCODER = new UrlEncoder();
    private static final Pattern VERSION_REGEX = Pattern.compile("(.*)[.]\\s*[+-]?([0-9]+)\\s*$");

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.sequencer.StreamSequencer#sequence(java.io.InputStream,
     *      org.modeshape.graph.sequencer.SequencerOutput, org.modeshape.graph.sequencer.StreamSequencerContext)
     */
    public void sequence( InputStream stream,
                          SequencerOutput output,
                          StreamSequencerContext context ) {
        BinaryFactory binaryFactory = context.getValueFactories().getBinaryFactory();
        DateTimeFactory dateFactory = context.getValueFactories().getDateFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        NameFactory nameFactory = context.getValueFactories().getNameFactory();
        ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
        AtomicInteger version = new AtomicInteger(0);

        // Figure out the name of the VDB archive ...
        Path pathToArchiveFile = context.getInputPath();
        Name zipFileName = VdbLexicon.VIRTUAL_DATABASE;
        if (pathToArchiveFile != null && !pathToArchiveFile.isRoot()) {
            // Remove the 'jcr:content' node (of type 'nt:resource'), if it is there ...
            if (pathToArchiveFile.getLastSegment().getName().equals(JcrLexicon.CONTENT)) pathToArchiveFile = pathToArchiveFile.getParent();
            if (!pathToArchiveFile.isRoot()) {
                zipFileName = pathToArchiveFile.getLastSegment().getName();
                // Remove the ".xmi" extension
                String fileNameWithoutExtension = zipFileName.getLocalName().replaceAll("\\.vdb$", "");
                zipFileName = context.getValueFactories().getNameFactory().create(zipFileName.getNamespaceUri(),
                                                                                  fileNameWithoutExtension);
                String name = stringFactory.create(zipFileName);
                zipFileName = extractVersionInfomation(context, name, version);
            }
        }
        assert zipFileName != null;

        // We need to access the 'vdb.xml' file first, and then process the models in the dependency order (with physical models
        // first) so that dependencies can be resolved when they're needed. Because we can't randomly access the
        // ZipEntry objects using a ZipInputStream, we need to use a ZipFile, and thus have to write out the VDB
        // archive to a temporary file...
        File vdbArchiveFile = null;
        try {
            String prefix = "modeshape" + URL_ENCODER.encode(stringFactory.create(zipFileName));
            vdbArchiveFile = File.createTempFile(prefix, "vdb");
            OutputStream ostream = new BufferedOutputStream(new FileOutputStream(vdbArchiveFile));
            HashingInputStream hashingStream = SecureHash.createHashingStream(Algorithm.SHA_1, stream);
            IoUtil.write(hashingStream, ostream); // closes streams ...
            String sha1 = hashingStream.getHashAsHexString();

            // Now we can access the files in any order, so start with the "vdb.xml" file ...
            ZipFile vdbArchive = new ZipFile(vdbArchiveFile);
            ZipEntry vdbXml = vdbArchive.getEntry("META-INF/vdb.xml");
            if (vdbXml == null) return;

            VdbManifest manifest = VdbManifest.read(vdbArchive.getInputStream(vdbXml), context);
            if (version.get() != 0) {
                // The version information was specified in the name, so override what was in the file ...
                manifest.setVersion(version.get());
            }

            // Register the namespaces that we'll know we'll need.
            // Other Teiid-related namespaces will be added by the model sequencers ...
            NamespaceRegistry registry = context.getNamespaceRegistry();
            registerIfMissing(registry, VdbLexicon.Namespace.PREFIX, VdbLexicon.Namespace.URI);
            registerIfMissing(registry, XmiLexicon.Namespace.PREFIX, XmiLexicon.Namespace.URI);
            registerIfMissing(registry, CoreLexicon.Namespace.PREFIX, CoreLexicon.Namespace.URI);

            // Create the node for the VDB ...
            Path vdbPath = pathFactory.createRelativePath(zipFileName);
            output.setProperty(vdbPath, JcrLexicon.PRIMARY_TYPE, VdbLexicon.VIRTUAL_DATABASE);
            output.setProperty(vdbPath, JcrLexicon.MIXIN_TYPES, JcrMixLexicon.REFERENCEABLE);
            output.setProperty(vdbPath, JcrLexicon.UUID, UUID.randomUUID());
            output.setProperty(vdbPath, VdbLexicon.DESCRIPTION, manifest.getDescription());
            output.setProperty(vdbPath, VdbLexicon.VERSION, (long)manifest.getVersion());
            output.setProperty(vdbPath, VdbLexicon.PREVIEW, manifest.isPreview());
            output.setProperty(vdbPath, VdbLexicon.ORIGINAL_FILE, pathToArchiveFile);
            output.setProperty(vdbPath, ModeShapeLexicon.SHA1, sha1);

            ReferenceResolver resolver = new ReferenceResolver(context);
            for (VdbModel model : manifest.modelsInDependencyOrder()) {
                if (model.getType().equalsIgnoreCase(ModelType.PHYSICAL) || model.getType().equalsIgnoreCase(ModelType.VIRTUAL)) {
                    ModelSequencer sequencer = new ModelSequencer(model, vdbPath, resolver);
                    sequencer.setUseXmiUuidsAsJcrUuids(false);
                    ZipEntry modelEntry = vdbArchive.getEntry(model.getPathInVdb());
                    if (modelEntry == null) {
                        // Some older VDBs have the model paths as absolute ...
                        modelEntry = vdbArchive.getEntry("/" + model.getPathInVdb());
                    }
                    if (modelEntry != null) {
                        String pathInVdb = model.getPathInVdb();
                        sequencer.sequence(vdbArchive.getInputStream(modelEntry), output, context);
                    }
                }
            }

        } catch (Exception e) {
            String location = context.getValueFactories().getStringFactory().create(context.getInputPath());
            context.getProblems().addError(e, TeiidI18n.errorReadingVdbFile, location, e.getMessage());
        } finally {
            if (vdbArchiveFile != null) {
                vdbArchiveFile.delete();
            }
        }
    }

    protected void registerIfMissing( NamespaceRegistry registry,
                                      String prefix,
                                      String url ) {
        if (!registry.isRegisteredNamespaceUri(url)) {
            registry.register(prefix, url);
        }
    }

    protected static Name extractVersionInfomation( ExecutionContext context,
                                                    String fileNameWithoutExtension,
                                                    AtomicInteger version ) {
        Matcher matcher = VERSION_REGEX.matcher(fileNameWithoutExtension);
        if (matcher.matches()) {
            // Extract the version number from the name ...
            fileNameWithoutExtension = matcher.group(1);
            version.set(context.getValueFactories().getLongFactory().create(matcher.group(2)).intValue());
        }
        // Remove all trailing '.' characters
        fileNameWithoutExtension = fileNameWithoutExtension.replaceAll("[.]*$", "");
        return context.getValueFactories().getNameFactory().create(fileNameWithoutExtension);
    }
}
