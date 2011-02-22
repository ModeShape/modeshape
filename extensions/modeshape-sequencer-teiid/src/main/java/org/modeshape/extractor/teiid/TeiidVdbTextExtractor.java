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
package org.modeshape.extractor.teiid;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.IoUtil;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.text.TextExtractor;
import org.modeshape.graph.text.TextExtractorContext;
import org.modeshape.graph.text.TextExtractorOutput;
import org.modeshape.sequencer.teiid.TeiidI18n;
import org.modeshape.sequencer.teiid.VdbManifest;
import org.modeshape.sequencer.teiid.VdbModel;
import org.modeshape.sequencer.teiid.VdbSequencer;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;

/**
 * 
 */
public class TeiidVdbTextExtractor implements TextExtractor {

    private static final UrlEncoder URL_ENCODER = new UrlEncoder();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.text.TextExtractor#supportsMimeType(java.lang.String)
     */
    @Override
    public boolean supportsMimeType( String mimeType ) {
        return "application/vnd.teiid.vdb".equals(mimeType);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.text.TextExtractor#extractFrom(java.io.InputStream, org.modeshape.graph.text.TextExtractorOutput,
     *      org.modeshape.graph.text.TextExtractorContext)
     */
    @Override
    public void extractFrom( InputStream stream,
                             TextExtractorOutput output,
                             TextExtractorContext context ) throws IOException {
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
                zipFileName = context.getValueFactories()
                                     .getNameFactory()
                                     .create(zipFileName.getNamespaceUri(), fileNameWithoutExtension);
                String name = stringFactory.create(zipFileName);
                zipFileName = VdbSequencer.extractVersionInfomation(context, name, version);
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
            IoUtil.write(stream, ostream); // closes streams ...

            // Now we can access the files in any order, so start with the "vdb.xml" file ...
            ZipFile vdbArchive = new ZipFile(vdbArchiveFile);
            ZipEntry vdbXml = vdbArchive.getEntry("META-INF/vdb.xml");
            if (vdbXml == null) return;

            VdbManifest manifest = VdbManifest.read(vdbArchive.getInputStream(vdbXml), context);
            if (version.get() != 0) {
                // The version information was specified in the name, so override what was in the file ...
                manifest.setVersion(version.get());
            }

            // Now record the text from the name, the description, and the model filenames ...
            output.recordText(stringFactory.create(zipFileName));
            output.recordText(manifest.getName());
            output.recordText(manifest.getDescription());
            output.recordText(Integer.toString(manifest.getVersion()));
            for (VdbModel model : manifest.getModels()) {
                output.recordText(model.getName());
                output.recordText(model.getType());
                output.recordText(model.getSourceName());
                output.recordText(model.getSourceTranslator());
                output.recordText(model.getSourceJndiName());
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

}
