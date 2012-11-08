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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.sequencer.teiid.lexicon.CoreLexicon;
import org.modeshape.sequencer.teiid.lexicon.VdbLexicon;

/**
 * A simple POJO that is used to represent the information for a model read in from a VDB manifest ("vdb.xml").
 */
public class VdbModel implements Comparable<VdbModel> {
    
    /**
     * The default model definition metadata type. Value is {@value}.
     */
    public static final String DEFAULT_METADATA_TYPE = "DDL";

    private String description;
    private String name;
    private String type;
    private String pathInVdb;
    private String sourceTranslator;
    private String sourceJndiName;
    private String sourceName;
    private boolean visible = true;
    private boolean builtIn = false;
    private long checksum;
    private final Set<String> imports = new HashSet<String>();
    private List<ValidationMarker> problems = new ArrayList<ValidationMarker>();
    private final Map<String, String> properties = new HashMap<String, String>();
    private String metadata; // model definition written in DDL
    private String metadataType;

    /**
     * @param name the model name (cannot be <code>null</code> or empty)
     * @param type the model type (can be <code>null</code> or empty)
     * @param pathInVdb the model path (can be <code>null</code> or empty)
     */
    public VdbModel( String name,
                     String type,
                     String pathInVdb ) {
        CheckArg.isNotEmpty(name, "name");

        this.name = name;
        this.pathInVdb = pathInVdb;
        this.type = (StringUtil.isBlank(type) ? CoreLexicon.ModelType.PHYSICAL : type);
    }

    /**
     * @return the description (never <code>null</code> but can be empty)
     */
    public String getDescription() {
        return ((this.description == null) ? "" : this.description);
    }

    /**
     * @param newValue the new description value (can be <code>null</code> or empty)
     */
    public void setDescription( final String newValue ) {
        this.description = newValue;
    }

    /**
     * @return the name (never <code>null</code> or empty)
     */
    public String getName() {
        return name;
    }

    /**
     * @return the model DDL definition (can be <code>null</code> or empty)
     */
    public String getModelDefinition() {
        return this.metadata;
    }

    /**
     * @param modelDefinition the new model definition (can be <code>null</code> or empty)
     */
    public void setModelDefinition( final String modelDefinition ) {
        this.metadata = modelDefinition;
    }

    /**
     * @return the model metadata type associated with the model definition (can be <code>null</code> or empty when there is no metadata)
     */
    public String getMetadataType() {
        if (StringUtil.isBlank(this.metadata)) {
            return null;
        }

        if (StringUtil.isBlank(this.metadataType)) {
            return DEFAULT_METADATA_TYPE;
        }

        return this.metadataType;
    }

    /**
     * @param metadataType the new metadata type (can be <code>null</code> or empty if default type should be used)
     */
    public void setMetadataType( final String metadataType ) {
        this.metadataType = metadataType;
    }

    /**
     * @return the overridden properties (never <code>null</code>)
     */
    public Map<String, String> getProperties() {
        return this.properties;
    }

    /**
     * @return the type (never <code>null</code> or empty)
     */
    public String getType() {
        return type;
    }

    /**
     * @return visible
     */
    public boolean isVisible() {
        return visible;
    }

    /**
     * @param visible Sets visible to the specified value.
     */
    public void setVisible( boolean visible ) {
        this.visible = visible;
    }

    /**
     * @return builtIn
     */
    public boolean isBuiltIn() {
        return builtIn;
    }

    /**
     * @return <code>true</code> if model definition is declared in the VDB manifest and not by an XMI file
     */
    public boolean isDeclarative() {
        return !getProperties().containsKey(VdbLexicon.ManifestIds.INDEX_NAME);
    }

    /**
     * @param builtIn Sets builtIn to the specified value.
     */
    public void setBuiltIn( boolean builtIn ) {
        this.builtIn = builtIn;
    }

    /**
     * @return checksum
     */
    public long getChecksum() {
        return checksum;
    }

    /**
     * @param checksum Sets checksum to the specified value.
     */
    public void setChecksum( long checksum ) {
        this.checksum = checksum;
    }


    /**
     * @return the path in the VDB (can be <code>null</code> or empty)
     */
    public String getPathInVdb() {
        return pathInVdb;
    }

    /**
     * @return sourceTranslator
     */
    public String getSourceTranslator() {
        return sourceTranslator;
    }

    /**
     * @param sourceTranslator Sets sourceTranslator to the specified value.
     */
    public void setSourceTranslator( String sourceTranslator ) {
        this.sourceTranslator = sourceTranslator;
    }

    /**
     * @return sourceJndiName
     */
    public String getSourceJndiName() {
        return sourceJndiName;
    }

    /**
     * @param sourceJndiName Sets sourceJndiName to the specified value.
     */
    public void setSourceJndiName( String sourceJndiName ) {
        this.sourceJndiName = sourceJndiName;
    }

    /**
     * @return the source name
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * @param sourceName Sets sourceName to the specified value.
     */
    public void setSourceName( String sourceName ) {
        this.sourceName = sourceName;
    }

    /**
     * @return the paths of the imported models (never <code>null</code> but can be empty)
     */
    public Set<String> getImports() {
        return imports;
    }

    /**
     * @param newImport the model import path being added as an import (cannot be <code>null</code> or empty)
     */
    public void addImport( final String newImport ) {
        CheckArg.isNotEmpty(newImport, "newImport");
        this.imports.add(newImport);
    }

    /**
     * @return the validation markers (never <code>null</code>)
     */
    public List<ValidationMarker> getProblems() {
        return problems;
    }

    public void addProblem( Severity severity,
                            String path,
                            String message ) {
        problems.add(new ValidationMarker(severity, path, message));
    }

    public void addProblem( String severity,
                            String path,
                            String message ) {
        if (severity != null) {
            try {
                addProblem(Severity.valueOf(severity.trim().toUpperCase()), path, message);
            } catch (IllegalArgumentException e) {
                // Unknown severity, so ignore
            }
        }
    }

    /**
     * The natural order of VDB models is based upon dependencies (e.g., model imports), where models that depends upon other
     * models will always follow the models they depend on. Thus any model that has no dependencies will always appear first.
     * 
     * @see Comparable#compareTo(Object)
     */
    @Override
    public int compareTo( VdbModel that ) {
        if (that == null) return 1;
        if (that == this) return 0;
        if (this.getImports().contains(that.getPathInVdb())) {
            // this model imports that, so this model is greater than that ...
            return 1;
        }
        if (that.getImports().contains(this.getPathInVdb())) {
            // that model imports this, so this model is less than that ...
            return -1;
        }
        // Otherwise, neither model depends upon each other, so base the order upon the number of models ...
        return this.getImports().size() - that.getImports().size();
    }

    /** The 'vdb.cnd' and 'teiid.cnd' files contain a property definition for 'vdb:severity' with these literal values. */
    public static enum Severity {
        WARNING,
        INFO,
        ERROR;
    }

    public static class ValidationMarker {
        private final String path;
        private final Severity severity;
        private final String message;

        public ValidationMarker( Severity severity,
                                 String path,
                                 String message ) {
            this.severity = severity != null ? severity : Severity.ERROR;
            this.path = path != null ? path : "";
            this.message = message != null ? message : "";
        }

        /**
         * @return message
         */
        public String getMessage() {
            return message;
        }

        /**
         * @return path
         */
        public String getPath() {
            return path;
        }

        /**
         * @return severity
         */
        public Severity getSeverity() {
            return severity;
        }

        /**
         * @see Object#toString()
         */
        @Override
        public String toString() {
            return severity.name() + " '" + path + "': " + message;
        }
    }
}
