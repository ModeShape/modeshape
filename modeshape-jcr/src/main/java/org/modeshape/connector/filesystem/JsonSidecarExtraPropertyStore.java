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
package org.modeshape.connector.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Json;
import org.modeshape.jcr.cache.DocumentStoreException;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.spi.federation.ExtraPropertiesStore;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;

/**
 * An {@link ExtraPropertiesStore} implementation that stores extra properties in JSON sidecar files adjacent to the actual file
 * or directory corresponding to the external node.
 */
class JsonSidecarExtraPropertyStore implements ExtraPropertiesStore {

    public static final String DEFAULT_EXTENSION = ".modeshape.json";
    public static final String DEFAULT_RESOURCE_EXTENSION = ".content.modeshape.json";

    private final FileSystemConnector connector;
    private final DocumentTranslator translator;

    protected JsonSidecarExtraPropertyStore( FileSystemConnector connector,
                                             DocumentTranslator translator ) {
        this.connector = connector;
        this.translator = translator;
    }

    protected String getExclusionPattern() {
        return "(.+)\\.(content\\.)?modeshape\\.json$";

    }

    @Override
    public Map<Name, Property> getProperties( String id ) {
        File sidecarFile = sidecarFile(id);
        if (!sidecarFile.exists()) return NO_PROPERTIES;
        try {
            Document document = read(new FileInputStream(sidecarFile));
            Map<Name, Property> results = new HashMap<Name, Property>();
            translator.getProperties(document, results);
            return results;
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public void updateProperties( String id,
                                  Map<Name, Property> properties ) {
        File sidecarFile = sidecarFile(id);
        try {
            EditableDocument document = null;
            if (!sidecarFile.exists()) {
                if (properties.isEmpty()) return;
                sidecarFile.createNewFile();
                document = Schematic.newDocument();
            } else {
                Document existing = read(new FileInputStream(sidecarFile));
                document = Schematic.newDocument(existing);
            }
            for (Map.Entry<Name, Property> entry : properties.entrySet()) {
                Property property = entry.getValue();
                if (property == null) {
                    translator.removeProperty(document, entry.getKey(), null, null);
                } else {
                    translator.setProperty(document, property, null, null);
                }
            }
            write(document, new FileOutputStream(sidecarFile));
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    @Override
    public void storeProperties( String id,
                                 Map<Name, Property> properties ) {
        File sidecarFile = sidecarFile(id);
        try {
            if (!sidecarFile.exists()) {
                if (properties.isEmpty()) return;
                sidecarFile.createNewFile();
            }
            EditableDocument document = Schematic.newDocument();
            for (Property property : properties.values()) {
                if (property == null) continue;
                translator.setProperty(document, property, null, null);
            }
            write(document, new FileOutputStream(sidecarFile));
        } catch (IOException e) {
            throw new DocumentStoreException(id, e);
        }
    }

    protected Document read( InputStream stream ) throws IOException {
        return Json.read(stream, false);
    }

    protected void write( Document document,
                          OutputStream stream ) throws IOException {
        Json.write(document, stream);
    }

    protected String extension() {
        return DEFAULT_EXTENSION;
    }

    protected String resourceExtension() {
        return DEFAULT_RESOURCE_EXTENSION;
    }

    @Override
    public boolean removeProperties( String id ) {
        File file = sidecarFile(id);
        if (!file.exists()) return false;
        file.delete();
        return true;
    }

    protected File sidecarFile( String id ) {
        File actualFile = connector.fileFor(id);
        String extension = extension();
        if (connector.isContentNode(id)) {
            extension = resourceExtension();
        }
        return new File(actualFile.getAbsolutePath() + extension);
    }

    @Override
    public boolean contains( String id ) {
        File file = sidecarFile(id);
        return file.exists();
    }
}
