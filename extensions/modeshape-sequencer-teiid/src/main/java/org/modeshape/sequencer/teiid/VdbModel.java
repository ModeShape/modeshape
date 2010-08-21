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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A simple POJO that is used to represent the information for a model read in from a VDB manifest ("vdb.xml").
 */
public class VdbModel implements Comparable<VdbModel> {

    public static final class ModelType {
        // These are not the only valid model types, but they're the only ones we care about at the moment
        public static final String PHYSICAL = "PHYSICAL";
        public static final String VIRTUAL = "VIRTUAL";
    }

    private final String name;
    private final String type;
    private final String pathInVdb;
    private String sourceTranslator;
    private String sourceJndiName;
    private String sourceName;
    private boolean visible = true;
    private boolean builtIn = false;
    private long checksum;
    private final Set<String> imports = new HashSet<String>();
    private List<ValidationMarker> problems = new ArrayList<ValidationMarker>();

    public VdbModel( String name,
                     String type,
                     String pathInVdb ) {
        this.name = name;
        this.pathInVdb = pathInVdb;
        this.type = type;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * @return type
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
     * @return pathInVdb
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
     * @return sourceName
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
     * @return imports
     */
    public Set<String> getImports() {
        return imports;
    }

    public void addImport( String importedPath ) {
        if (importedPath != null) imports.add(importedPath);
    }

    /**
     * @return problems
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
                // Uknown severity, so ignore
            }
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * The natural order of VDB models is based upon dependencies (e.g., model imports), where models that depends upon other
     * models will always follow the models they depend on. Thus any model that has no dependencies will always appear first.
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
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
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return severity.name() + " '" + path + "': " + message;
        }
    }
}
