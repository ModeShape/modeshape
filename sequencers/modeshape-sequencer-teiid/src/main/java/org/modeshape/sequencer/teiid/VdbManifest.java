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

import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.sequencer.Sequencer.Context;
import org.modeshape.sequencer.teiid.VdbDataRole.Permission;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The POJO for the vdb.xml file.
 */
public class VdbManifest implements Comparable<VdbManifest> {

    /*
     * Information gathered from the latest vdb-deployer.xsd.
     * 
     * A VDB has these attributes:
     *   name (string/required)
     *   version (int/required)
     *   
     * A VDB has these elements:
     *   description (string/optional)
     *   property (property/0..M)
     *   import-vdb (import-vdb/0..M)
     *   model (model/0..M)
     *   translator (translator/0..M)
     *   data-role (data-role/0..M)
     *   entry (entry/0..M)
     * 
     * A model has these attributes:
     *   name (string/required)
     *   type (string/required)
     *   visible (boolean/optional)
     *   path (string/optional)
     *   
     * A model has these elements:
     *   description (string/optional)
     *   property (property/0..M)
     *   source (source/0..M)
     *   validation-error (validation-error/0..M)
     * 
     * A translator has these attributes:
     *   name (string/required)
     *   type (string/required)
     *   description (string/optional)
     *   
     * A translator has these elements:
     *   property (property/0..M)
     * 
     * A data-role has these attributes:
     *   name (string)
     *   any-authenticated (boolean)
     *   allow-create-temporary-tables (boolean)
     *   
     * A data-role has these elements:
     *   description (string/optional)
     *   permission (permission/1..M)
     *   mapped-role-name (string/0..M)
     * 
     * A source has these attributes:
     *   name (string/required)
     *   translator-name (string/required)
     *   connection-jndi-name (string/optional)
     * 
     * A validation-error has these attributes:
     *   severity (string/required)
     *   path (string/optional)
     * 
     * A property has these attributes:
     *   name (string/required)
     *   value (string/required)
     * 
     * A permission has these elements:
     *   resource-name (string/required)
     *   allow-create (boolean/optional)
     *   allow-read (boolean/optional)
     *   allow-update (boolean/optional)
     *   allow-delete (boolean/optional)
     *   allow-execute (boolean/optional)
     *   allow-alter (boolean/optional)
     *   
     * An entry has these attributes:
     *   path (string/required)
     * 
     * An entry has these elements:
     *   description (string/optional)
     *   property (property/0..M)  
     */

    static final Logger LOGGER = Logger.getLogger(VdbManifest.class);

    public static VdbManifest read( final InputStream stream,
                                    final Context context ) throws Exception {

        return new Reader().read(stream, context);
    }

    private final String name;
    private String description;
    private final Map<String, String> properties = new HashMap<String, String>();
    private int version = 1;

    private final List<VdbDataRole> dataRoles = new ArrayList<VdbDataRole>();
    private final List<VdbEntry> entries = new ArrayList<VdbEntry>();
    private final List<VdbModel> models = new ArrayList<VdbModel>();
    private final List<VdbTranslator> translators = new ArrayList<VdbTranslator>();
    private final List<ImportVdb> importVdbs = new ArrayList<ImportVdb>();

    /**
     * @param name the VDB name (cannot be <code>null</code> or empty)
     */
    public VdbManifest( final String name ) {
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo( final VdbManifest that ) {
        CheckArg.isNotNull(that, "that");

        if (this == that) {
            return 0;
        }

        // order by name
        return this.name.compareTo(that.name);
    }

    /**
     * @return the data roles found in the VDB (never <code>null</code> but can be empty)
     */
    public List<VdbDataRole> getDataRoles() {
        return this.dataRoles;
    }

    /**
     * @return the description (never <code>null</code> but can be empty)
     */
    public String getDescription() {
        return ((this.description == null) ? "" : this.description);
    }

    /**
     * @return the entries (never <code>null</code>)
     */
    public List<VdbEntry> getEntries() {
        return this.entries;
    }

    /**
     * @return the import VDBs found in the VDB (never <code>null</code> but can be empty)
     */
    public List<ImportVdb> getImportVdbs() {
        return this.importVdbs;
    }

    /**
     * @param path the path of the VDB model being requested (cannot be <code>null</code> or empty)
     * @return the VDB model or <code>null</code> if not found
     */
    public VdbModel getModel( String path ) {
        CheckArg.isNotEmpty(path, "path");

        for (VdbModel model : getModels()) {
            if (path.equals(model.getPathInVdb())) {
                return model;
            }
        }

        return null;
    }

    /**
     * @return the models (never <code>null</code>)
     */
    public List<VdbModel> getModels() {
        return this.models;
    }

    /**
     * @return the VDB name (never <code>null</code> or empty)
     */
    public String getName() {
        return this.name;
    }

    /**
     * @return the properties (never <code>null</code>)
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * @return the translators found in the VDB (never <code>null</code> but can be empty)
     */
    public List<VdbTranslator> getTranslators() {
        return this.translators;
    }

    /**
     * @return version
     */
    public int getVersion() {
        return this.version;
    }

    public Iterable<VdbModel> modelsInDependencyOrder() {
        if (!this.models.isEmpty()) {
            Collections.sort(this.models);
        }
        return this.models;
    }

    /**
     * @param description Sets description to the specified value.
     */
    public void setDescription( final String description ) {
        this.description = description != null ? description : "";
    }

    /**
     * @param version Sets version to the specified value.
     */
    public void setVersion( final int version ) {
        this.version = version;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return this.name + " v" + this.version + " (\"" + this.description + "\")";
    }

    protected static class Reader {
        private VdbDataRole parseDataRole( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.DATA_ROLE.equals(streamReader.getLocalName());

            // create data role and collect attributes
            final VdbDataRole dataRole = processDataRoleAttributes(streamReader);
            assert (dataRole != null) : "data role should not be null";

            while (streamReader.hasNext()) {
                final int eventType = streamReader.next();

                if (streamReader.isStartElement()) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.DESCRIPTION.equals(elementName)) {
                        final String description = streamReader.getElementText();
                        dataRole.setDescription(description);
                    } else if (VdbLexicon.ManifestIds.PERMISSION.equals(elementName)) {
                        final VdbDataRole.Permission permission = parsePermission(streamReader, dataRole);
                        assert (permission != null) : "permission is null";
                        dataRole.getPermissions().add(permission);
                    } else if (VdbLexicon.ManifestIds.MAPPED_ROLE_NAME.equals(elementName)) {
                        final String roleName = streamReader.getElementText();
                        dataRole.getMappedRoleNames().add(roleName);
                    } else {
                        LOGGER.debug("**** unexpected data role element={0}", elementName);
                    }
                } else if (streamReader.isEndElement() && VdbLexicon.ManifestIds.DATA_ROLE.equals(
                        streamReader.getLocalName())) {
                    break;
                } else {
                    if (streamReader.isCharacters()) {
                        if (!StringUtil.isBlank(streamReader.getText())) {
                            LOGGER.debug("**** unhandled data role event type CHARACTERS={0}", streamReader.getText());
                        }
                    } else if (streamReader.isEndElement()) {
                        LOGGER.debug("**** unhandled data role event type END_ELEMENT={0}", streamReader.getLocalName());
                    } else {
                        LOGGER.debug("**** unhandled data role event type={0}", eventType);
                    }
                }
            }

            assert (dataRole != null) : "dataRole is null";
            return dataRole;
        }

        private VdbEntry parseEntry( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.ENTRY.equals(streamReader.getLocalName());

            // collect attributes
            final VdbEntry entry = processEntryAttributes(streamReader);
            assert (entry != null) : "entry path is null";

            while (streamReader.hasNext()) {
                final int eventType = streamReader.next();

                if (streamReader.isStartElement()) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.DESCRIPTION.equals(elementName)) {
                        final String description = streamReader.getElementText();
                        entry.setDescription(description);
                    } else if (VdbLexicon.ManifestIds.PROPERTY.equals(elementName)) {
                        final Map.Entry<String, String> property = processPropertyAttributes(streamReader);
                        assert (property != null) : "entry property is null";
                        entry.getProperties().put(property.getKey(), property.getValue());
                    } else {
                        LOGGER.debug("**** unexpected entry element={0}", elementName);
                    }
                } else if (streamReader.isEndElement() && VdbLexicon.ManifestIds.ENTRY.equals(streamReader.getLocalName())) {
                    break;
                } else {
                    if (streamReader.isCharacters()) {
                        if (!StringUtil.isBlank(streamReader.getText())) {
                            LOGGER.debug("**** unhandled entry event type CHARACTERS={0}", streamReader.getText());
                        }
                    } else if (streamReader.isEndElement()) {
                        LOGGER.debug("**** unhandled entry event type END_ELEMENT={0}", streamReader.getLocalName());
                    } else {
                        LOGGER.debug("**** unhandled entry event type={0}", eventType);
                    }
                }
            }
            return entry;
        }

        private VdbModel parseModel( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.MODEL.equals(streamReader.getLocalName());

            // collect model attributes
            final VdbModel model = processModelAttributes(streamReader);
            assert (model != null) : "model is null";

            while (streamReader.hasNext()) {
                final int eventType = streamReader.next();

                if (streamReader.isStartElement()) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.VALIDATION_ERROR.equals(elementName)) {
                        processValidationErrorAttributes(streamReader, model);
                    } else if (VdbLexicon.ManifestIds.SOURCE.equals(elementName)) {
                        processModelSourceAttributes(streamReader, model);
                    } else if (VdbLexicon.ManifestIds.PROPERTY.equals(elementName)) {
                        final Map.Entry<String, String> property = processPropertyAttributes(streamReader);
                        assert (property != null) : "model property is null";

                        if (VdbLexicon.ManifestIds.IMPORTS.equals(property.getKey())) {
                            model.addImport(property.getValue());
                        } else if (VdbLexicon.ManifestIds.CHECKSUM.equals(property.getKey())) {
                            model.setChecksum(Long.parseLong(property.getValue()));
                        } else if (VdbLexicon.ManifestIds.BUILT_IN.equals(property.getKey())) {
                            model.setBuiltIn(Boolean.parseBoolean(property.getValue()));
                        } else {
                            model.getProperties().put(property.getKey(), property.getValue());
                        }
                    } else if (VdbLexicon.ManifestIds.DESCRIPTION.equals(elementName)) {
                        final String description = streamReader.getElementText();
                        model.setDescription(description);
                    } else if (VdbLexicon.ManifestIds.METADATA.equals(elementName)) {
                        if (streamReader.getAttributeCount() == 1) {
                            model.setMetadataType(streamReader.getAttributeValue(0));
                        }

                        final String metadata = streamReader.getElementText().trim();
                        model.setModelDefinition(metadata.replaceAll("\\s{2,}", " ")); // collapse whitespace
                    } else {
                        LOGGER.debug("**** unexpected model element={0}", elementName);
                    }
                } else if (streamReader.isEndElement()) {
                    if (VdbLexicon.ManifestIds.SOURCE.equals(streamReader.getLocalName())) {
                        continue;
                    } else if (VdbLexicon.ManifestIds.MODEL.equals(streamReader.getLocalName())) {
                        break;
                    } else {
                        LOGGER.debug("**** unhandled model event type END_ELEMENT={0}", streamReader.getLocalName());
                    }
                } else {
                    if (streamReader.isCharacters()) {
                        if (!StringUtil.isBlank(streamReader.getText())) {
                            LOGGER.debug("**** unhandled model event type CHARACTERS={0}", streamReader.getText());
                        }
                    } else {
                        LOGGER.debug("**** unhandled model event type={0}", eventType);
                    }
                }
            }

            return model;
        }

        private Permission parsePermission( final XMLStreamReader streamReader,
                                            final VdbDataRole dataRole ) throws Exception {
            assert VdbLexicon.ManifestIds.PERMISSION.equals(streamReader.getLocalName());
            assert (dataRole != null) : "data role is null";

            // should not have any attributes
            if (LOGGER.isDebugEnabled()) {
                for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                    final QName name = streamReader.getAttributeName(i);
                    final String value = streamReader.getAttributeValue(i);
                    LOGGER.debug("**** unexpected data role permission attribute name={0}, value={1}", name.getLocalPart(), value);
                }
            }

            Permission permission = null;
            boolean alter = false;
            boolean create = false;
            boolean delete = false;
            boolean execute = false;
            boolean read = false;
            String resourceName = null;
            boolean update = false;

            while (streamReader.hasNext()) {
                final int eventType = streamReader.next();

                if (streamReader.isStartElement()) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.RESOURCE_NAME.equals(elementName)) {
                        resourceName = streamReader.getElementText();
                    } else if (VdbLexicon.ManifestIds.ALLOW_ALTER.equals(elementName)) {
                        alter = Boolean.parseBoolean(streamReader.getElementText());
                    } else if (VdbLexicon.ManifestIds.ALLOW_CREATE.equals(elementName)) {
                        create = Boolean.parseBoolean(streamReader.getElementText());
                    } else if (VdbLexicon.ManifestIds.ALLOW_DELETE.equals(elementName)) {
                        delete = Boolean.parseBoolean(streamReader.getElementText());
                    } else if (VdbLexicon.ManifestIds.ALLOW_EXECUTE.equals(elementName)) {
                        execute = Boolean.parseBoolean(streamReader.getElementText());
                    } else if (VdbLexicon.ManifestIds.ALLOW_READ.equals(elementName)) {
                        read = Boolean.parseBoolean(streamReader.getElementText());
                    } else if (VdbLexicon.ManifestIds.ALLOW_UPDATE.equals(elementName)) {
                        update = Boolean.parseBoolean(streamReader.getElementText());
                    } else {
                        LOGGER.debug("**** unexpected data role permission element={0}", elementName);
                    }
                } else if (streamReader.isEndElement() && VdbLexicon.ManifestIds.PERMISSION.equals(
                        streamReader.getLocalName())) {
                    if (StringUtil.isBlank(resourceName)) {
                        throw new Exception(TeiidI18n.missingPermissionResourceName.text());
                    }

                    permission = dataRole.new Permission(resourceName);
                    permission.allowAlter(alter);
                    permission.allowCreate(create);
                    permission.allowDelete(delete);
                    permission.allowExecute(execute);
                    permission.allowRead(read);
                    permission.allowUpdate(update);

                    break;
                } else {
                    if (streamReader.isCharacters()) {
                        if (!StringUtil.isBlank(streamReader.getText())) {
                            LOGGER.debug("**** unhandled data role permission event type CHARACTERS={0}", streamReader.getText());
                        }
                    } else if (streamReader.isEndElement()) {
                        LOGGER.debug("**** unhandled data role permission event type END_ELEMENT={0}", streamReader.getLocalName());
                    } else {
                        LOGGER.debug("**** unhandled data role permission event type={0}", eventType);
                    }
                }
            }

            return permission;
        }

        private VdbTranslator parseTranslator( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.TRANSLATOR.equals(streamReader.getLocalName());

            // collect attributes
            final VdbTranslator translator = processTranslatorAttributes(streamReader);
            assert (translator != null) : "translator is null";

            while (streamReader.hasNext()) {
                final int eventType = streamReader.next();

                if (streamReader.isStartElement()) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.PROPERTY.equals(elementName)) {
                        final Map.Entry<String, String> property = processPropertyAttributes(streamReader);
                        assert (property != null) : "translator property is null";
                        translator.getProperties().put(property.getKey(), property.getValue());
                    } else {
                        LOGGER.debug("**** unexpected data role element={0}", elementName);
                    }
                } else if (streamReader.isEndElement() && VdbLexicon.ManifestIds.TRANSLATOR.equals(
                        streamReader.getLocalName())) {
                    break;
                } else {
                    if (streamReader.isCharacters()) {
                        if (!StringUtil.isBlank(streamReader.getText())) {
                            LOGGER.debug("**** unhandled translator event type CHARACTERS={0}", streamReader.getText());
                        }
                    } else if (streamReader.isEndElement()) {
                        LOGGER.debug("**** unhandled translator event type END_ELEMENT={0}", streamReader.getLocalName());
                    } else {
                        LOGGER.debug("**** unhandled translator event type={0}", eventType);
                    }
                }
            }
            return translator;
        }

        private VdbManifest parseVdb( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.VDB.equals(streamReader.getLocalName());

            // collect VDB attributes
            final VdbManifest manifest = processVdbAttributes(streamReader);
            assert (manifest != null) : "manifest is null";

            // collect children
            while (streamReader.hasNext()) {
                streamReader.next(); // point to next element

                if (streamReader.isStartElement()) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.DESCRIPTION.equals(elementName)) {
                        final String description = streamReader.getElementText();
                        manifest.setDescription(description);
                    } else if (VdbLexicon.ManifestIds.MODEL.equals(elementName)) {
                        final VdbModel model = parseModel(streamReader);
                        assert (model != null) : "model is null";
                        manifest.getModels().add(model);
                    } else if (VdbLexicon.ManifestIds.PROPERTY.equals(elementName)) {
                        final Map.Entry<String, String> property = processPropertyAttributes(streamReader);
                        assert (property != null) : "VDB property is null";
                        manifest.getProperties().put(property.getKey(), property.getValue());
                    } else if (VdbLexicon.ManifestIds.TRANSLATOR.equals(elementName)) {
                        final VdbTranslator translator = parseTranslator(streamReader);
                        assert (translator != null) : "translator is null";
                        manifest.getTranslators().add(translator);
                    } else if (VdbLexicon.ManifestIds.DATA_ROLE.equals(elementName)) {
                        final VdbDataRole dataRole = parseDataRole(streamReader);
                        assert (dataRole != null) : "dataRole is null";
                        manifest.getDataRoles().add(dataRole);
                    } else if (VdbLexicon.ManifestIds.ENTRY.equals(elementName)) {
                        final VdbEntry entry = parseEntry(streamReader);
                        assert (entry != null) : "entry is null";
                        manifest.getEntries().add(entry);
                    } else if (VdbLexicon.ManifestIds.IMPORT_VDB.equals(elementName)) {
                        final ImportVdb importVdb = processImportVdbAttributes(streamReader);
                        assert (importVdb != null) : "importVdb is null";
                        manifest.getImportVdbs().add(importVdb);
                    } else {
                        LOGGER.debug("**** unexpected VDB element={0}", elementName);
                    }
                } else if (streamReader.isEndElement() && VdbLexicon.ManifestIds.VDB.equals(streamReader.getLocalName())) {
                    break;
                }
            }

            return manifest;
        }

        private VdbDataRole processDataRoleAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.DATA_ROLE.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("data-role attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            // make sure there is a name
            final String name = attributes.get(VdbLexicon.ManifestIds.NAME);

            if (StringUtil.isBlank(name)) {
                throw new Exception(TeiidI18n.missingDataRoleName.text());
            }

            final VdbDataRole dataRole = new VdbDataRole(name);
            attributes.remove(VdbLexicon.ManifestIds.NAME);

            { // set any-authenticated
                final String anyAuthenticated = attributes.get(VdbLexicon.ManifestIds.ANY_AUTHENTICATED);

                if (!StringUtil.isBlank(anyAuthenticated)) {
                    dataRole.setAnyAuthenticated(Boolean.parseBoolean(anyAuthenticated));
                    attributes.remove(VdbLexicon.ManifestIds.ANY_AUTHENTICATED);
                }
            }

            { // set allow-create-temporary-tables
                final String allowCreateTempTables = attributes.get(VdbLexicon.ManifestIds.ALLOW_CREATE_TEMP_TABLES);

                if (!StringUtil.isBlank(allowCreateTempTables)) {
                    dataRole.setAllowCreateTempTables(Boolean.parseBoolean(allowCreateTempTables));
                    attributes.remove(VdbLexicon.ManifestIds.ALLOW_CREATE_TEMP_TABLES);
                }
            }

            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected data role attribute:name={0}", entry.getKey());
                }
            }

            return dataRole;
        }

        private VdbEntry processEntryAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.ENTRY.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("entry attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            // make sure there is a path
            final String path = attributes.get(VdbLexicon.ManifestIds.PATH);

            if (StringUtil.isBlank(path)) {
                throw new Exception(TeiidI18n.missingEntryPath.text());
            }

            final VdbEntry entry = new VdbEntry(path);
            attributes.remove(VdbLexicon.ManifestIds.PATH);

            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> attrib : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected entry attribute:name={0}", attrib.getKey());
                }
            }

            return entry;
        }

        private ImportVdb processImportVdbAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.IMPORT_VDB.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("import VDB attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            // make sure there is a name and version
            final String name = attributes.get(VdbLexicon.ManifestIds.NAME);
            final String version = attributes.get(VdbLexicon.ManifestIds.VERSION);

            if (StringUtil.isBlank(name) || StringUtil.isBlank(version)) {
                throw new Exception(TeiidI18n.missingImportVdbNameOrVersion.text());
            }

            // create ImportVdb
            final ImportVdb importVdb = new ImportVdb(attributes.get(VdbLexicon.ManifestIds.NAME),
                                                      Integer.parseInt(attributes.get(VdbLexicon.ManifestIds.VERSION)));
            attributes.remove(VdbLexicon.ManifestIds.NAME);
            attributes.remove(VdbLexicon.ManifestIds.VERSION);

            { // set import data policies flag
                final String importDataPolicies = attributes.get(VdbLexicon.ManifestIds.IMPORT_DATA_POLICIES);

                if (!StringUtil.isBlank(importDataPolicies)) {
                    importVdb.setImportDataPolicies(Boolean.parseBoolean(importDataPolicies));
                    attributes.remove(VdbLexicon.ManifestIds.IMPORT_DATA_POLICIES);
                }
            }

            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected import VDB attribute:name={0}", entry.getKey());
                }
            }

            return importVdb;
        }

        private VdbModel processModelAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.MODEL.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("model attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            // make sure there is a model name, type, and path
            final String name = attributes.get(VdbLexicon.ManifestIds.NAME);
            final String type = attributes.get(VdbLexicon.ManifestIds.TYPE);
            String path = attributes.get(VdbLexicon.ManifestIds.PATH);

            if (!StringUtil.isBlank(path)) {
                path = path.replaceFirst("^/", "");
            }

            // create model
            final VdbModel model = new VdbModel(name, type, path);
            attributes.remove(VdbLexicon.ManifestIds.NAME);
            attributes.remove(VdbLexicon.ManifestIds.TYPE);
            attributes.remove(VdbLexicon.ManifestIds.PATH);

            { // set model visibility flag
                final String visible = attributes.get(VdbLexicon.ManifestIds.VISIBLE);

                if (!StringUtil.isBlank(visible)) {
                    model.setVisible(Boolean.parseBoolean(visible));
                    attributes.remove(VdbLexicon.ManifestIds.VISIBLE);
                }
            }

            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected model attribute:name={0}", entry.getKey());
                }
            }

            return model;
        }

        private void processModelSourceAttributes( final XMLStreamReader streamReader,
                                                   final VdbModel model ) throws Exception {
            assert VdbLexicon.ManifestIds.SOURCE.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("model source attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            { // set model source name
                final String name = attributes.get(VdbLexicon.ManifestIds.NAME);

                if (!StringUtil.isBlank(name)) {
                    model.setSourceName(name);
                    attributes.remove(VdbLexicon.ManifestIds.NAME);
                }
            }

            { // set model translator name
                final String translatorName = attributes.get(VdbLexicon.ManifestIds.TRANSLATOR_NAME);

                if (!StringUtil.isBlank(translatorName)) {
                    model.setSourceTranslator(translatorName);
                    attributes.remove(VdbLexicon.ManifestIds.TRANSLATOR_NAME);
                }
            }

            { // set model connection JNDI name
                final String jndiName = attributes.get(VdbLexicon.ManifestIds.JNDI_NAME);

                if (!StringUtil.isBlank(jndiName)) {
                    model.setSourceJndiName(jndiName);
                    attributes.remove(VdbLexicon.ManifestIds.JNDI_NAME);
                }
            }

            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected model source attribute:name={0}", entry.getKey());
                }
            }

            assert (((streamReader.next() == XMLStreamConstants.END_ELEMENT) && VdbLexicon.ManifestIds.SOURCE.equals(
                    streamReader.getLocalName())));
        }

        private Map.Entry<String, String> processPropertyAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.PROPERTY.equals(streamReader.getLocalName());

            if (streamReader.getAttributeCount() == 2) {
                String propName = null;
                String propValue = null;

                QName qname = streamReader.getAttributeName(0);
                String name = qname.getLocalPart();
                String value = streamReader.getAttributeValue(0);

                if (VdbLexicon.ManifestIds.NAME.equals(name)) {
                    propName = value;
                } else if (VdbLexicon.ManifestIds.VALUE.equals(name)) {
                    propValue = value;
                }

                qname = streamReader.getAttributeName(1);
                name = qname.getLocalPart();
                value = streamReader.getAttributeValue(1);

                if (VdbLexicon.ManifestIds.VALUE.equals(name)) {
                    propValue = value;
                } else if (VdbLexicon.ManifestIds.NAME.equals(name)) {
                    propName = value;
                }

                // must have both a property name and value
                if (StringUtil.isBlank(propName) || StringUtil.isBlank(propValue)) {
                    throw new Exception(TeiidI18n.missingPropertyNameOrValue.text());
                }
                LOGGER.debug("adding property name={0}, value={1}", propName, propValue);

                final int eventType = streamReader.next();
                assert (eventType == XMLStreamConstants.END_ELEMENT) : "**** unexpected PROPERTY END_ELEMENT of " + eventType;
                return new AbstractMap.SimpleEntry<String, String>(propName, propValue);
            }

            // should only have a name and value
            throw new Exception(TeiidI18n.invalidNumberOfPropertyAttributes.text(streamReader.getAttributeCount()));

        }

        private VdbTranslator processTranslatorAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.TRANSLATOR.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("translator attribute name={0}, value={1}", name.getLocalPart(),  value);
            }

            // make sure there is a name and type
            final String name = attributes.get(VdbLexicon.ManifestIds.NAME);
            final String type = attributes.get(VdbLexicon.ManifestIds.TYPE);

            if (StringUtil.isBlank(name) || StringUtil.isBlank(type)) {
                throw new Exception(TeiidI18n.missingTranslatorNameOrType.text());
            }

            final VdbTranslator translator = new VdbTranslator(name, type);
            attributes.remove(VdbLexicon.ManifestIds.NAME);
            attributes.remove(VdbLexicon.ManifestIds.TYPE);

            { // set description
                final String description = attributes.get(VdbLexicon.ManifestIds.DESCRIPTION);

                if (!StringUtil.isBlank(description)) {
                    translator.setDescription(description);
                    attributes.remove(VdbLexicon.ManifestIds.DESCRIPTION);
                }
            }
            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected translator attribute:name={0}", entry.getKey());
                }
            }

            return translator;
        }

        private void processValidationErrorAttributes( final XMLStreamReader streamReader,
                                                       final VdbModel model ) throws Exception {
            assert VdbLexicon.ManifestIds.VALIDATION_ERROR.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("model validation error attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            { // create model problem
                final String severity = attributes.get(VdbLexicon.ManifestIds.SEVERITY);
                attributes.remove(VdbLexicon.ManifestIds.SEVERITY);

                final String path = attributes.get(VdbLexicon.ManifestIds.PATH);
                attributes.remove(VdbLexicon.ManifestIds.PATH);

                final String message = streamReader.getElementText();

                model.addProblem(severity, path, message);
            }
            // look for unhandled attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected model validation error attribute:name={0}", entry.getKey());
                }
            }
        }

        private VdbManifest processVdbAttributes( final XMLStreamReader streamReader ) throws Exception {
            assert VdbLexicon.ManifestIds.VDB.equals(streamReader.getLocalName());

            final Map<String, String> attributes = new HashMap<String, String>();

            for (int i = 0, size = streamReader.getAttributeCount(); i < size; ++i) {
                final QName name = streamReader.getAttributeName(i);
                final String value = streamReader.getAttributeValue(i);
                attributes.put(name.getLocalPart(), value);
                LOGGER.debug("VDB attribute name={0}, value={1}", name.getLocalPart(), value);
            }

            // set VDB name
            final String name = attributes.get(VdbLexicon.ManifestIds.NAME);

            if (StringUtil.isBlank(name)) {
                throw new Exception(TeiidI18n.missingVdbName.text());
            }

            final VdbManifest manifest = new VdbManifest(name);
            attributes.remove(VdbLexicon.ManifestIds.NAME);

            { // set VDB version
                final String version = attributes.get(VdbLexicon.ManifestIds.VERSION);

                if (!StringUtil.isBlank(version)) {
                    try {
                        manifest.setVersion(Integer.parseInt(version));
                    } catch (final NumberFormatException e) {
                        LOGGER.error(e, TeiidI18n.invalidVdbVersion, name, version);
                    }

                    attributes.remove(VdbLexicon.ManifestIds.VERSION);
                }
            }

            // look for unexpected attributes
            if (LOGGER.isDebugEnabled()) {
                for (final Map.Entry<String, String> entry : attributes.entrySet()) {
                    LOGGER.debug("**** unexpected VDB attribute:name={0}", entry.getKey());
                }
            }

            return manifest;
        }

        public VdbManifest read( final InputStream stream,
                                 final Context context ) throws Exception {
            VdbManifest manifest = null;
            final XMLInputFactory factory = XMLInputFactory.newInstance();
            final XMLStreamReader streamReader = factory.createXMLStreamReader(stream);

            if (streamReader.hasNext()) {
                if (streamReader.next() == XMLStreamConstants.START_ELEMENT) {
                    final String elementName = streamReader.getLocalName();

                    if (VdbLexicon.ManifestIds.VDB.equals(elementName)) {
                        manifest = parseVdb(streamReader);
                        assert (manifest != null) : "manifest is null";
                    } else {
                        LOGGER.debug("**** unhandled vdb read element ****");
                    }
                }
            }

            return manifest;
        }
    }
}
