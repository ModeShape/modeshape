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
package org.modeshape.maven;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.NoOpEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;

/**
 * Identifier of a Maven 2 artifact.
 */
public class MavenId implements Comparable<MavenId>, Cloneable {

    /**
     * Build a classpath of {@link MavenId}s by parsing the supplied string containing comma-separated Maven artifact
     * coordinates. Any duplicates in the classpath are excluded.
     * @param commaSeparatedCoordinates the string of Maven artifact coordinates
     * @return the array of {@link MavenId} instances representing the classpath
     */
    public static MavenId[] createClasspath( String commaSeparatedCoordinates ) {
        if (commaSeparatedCoordinates == null) return new MavenId[] {};
        String[] coordinates = commaSeparatedCoordinates.split(",");
        return createClasspath(coordinates);
    }

    /**
     * Build a classpath of {@link MavenId}s by parsing the supplied Maven artifact coordinates. Any duplicates in the classpath
     * are excluded.
     * @param mavenCoordinates the array of Maven artifact coordinates
     * @return the array of {@link MavenId} instances representing the classpath
     */
    public static MavenId[] createClasspath( String... mavenCoordinates ) {
        if (mavenCoordinates == null) return new MavenId[] {};
        // Use a linked set that maintains order and adds no duplicates ...
        Set<MavenId> result = new LinkedHashSet<MavenId>();
        for (int i = 0; i < mavenCoordinates.length; i++) {
            String coordinateStr = mavenCoordinates[i];
            if (coordinateStr == null) continue;
            coordinateStr = coordinateStr.trim();
            if (coordinateStr.length() != 0) {
                result.add(new MavenId(coordinateStr));
            }
        }
        return result.toArray(new MavenId[result.size()]);
    }

    /**
     * Create a classpath of {@link MavenId}s by examining the supplied IDs and removing any duplicates.
     * @param mavenIds the Maven IDs
     * @return the array of {@link MavenId} instances representing the classpath
     */
    public static MavenId[] createClasspath( MavenId... mavenIds ) {
        // Use a linked set that maintains order and adds no duplicates ...
        Set<MavenId> result = new LinkedHashSet<MavenId>();
        for (MavenId mavenId : mavenIds) {
            if (mavenId != null) result.add(mavenId);
        }
        return result.toArray(new MavenId[result.size()]);
    }

    private final String groupId;
    private final String artifactId;
    private final Version version;
    private final String classifier;

    /**
     * Create an Maven ID from the supplied string containing the coordinates for a Maven artifact. Coordinates are of the form:
     * 
     * <pre>
     *         groupId:artifactId[:version[:classifier]]
     * </pre>
     * 
     * where
     * <dl>
     * <dt>groupId</dt>
     * <dd> is the group identifier (e.g., <code>org.modeshape</code>), which may not be empty
     * <dt>artifactId</dt>
     * <dd> is the artifact identifier (e.g., <code>modeshape-maven</code>), which may not be empty
     * <dt>version</dt>
     * <dd> is the optional version (e.g., <code>org.modeshape</code>)
     * <dt>classifier</dt>
     * <dd> is the optional classifier (e.g., <code>test</code> or <code>jdk1.4</code>)
     * </dl>
     * @param coordinates the string containing the Maven coordinates
     * @throws IllegalArgumentException if the supplied string is null or if the string does not match the expected format
     */
    public MavenId( String coordinates ) {
        CheckArg.isNotNull(coordinates, "coordinates");
        coordinates = coordinates.trim();
        CheckArg.isNotEmpty(coordinates, "coordinates");

        // This regular expression has the following groups:
        // 1) groupId
        // 2) :artifactId
        // 3) artifactId
        // 4) :version
        // 5) version
        // 6) :classifier
        // 7) classifier
        Pattern urlPattern = Pattern.compile("([^:]+)(:([^:]+)(:([^:]*)(:([^:]*))?)?)?");
        Matcher matcher = urlPattern.matcher(coordinates);
        if (!matcher.find()) {
            throw new IllegalArgumentException(MavenI18n.unsupportedMavenCoordinateFormat.text(coordinates));
        }
        String groupId = matcher.group(1);
        String artifactId = matcher.group(3);
        String version = matcher.group(5);
        String classifier = matcher.group(7);
        CheckArg.isNotEmpty(groupId, "groupId");
        CheckArg.isNotEmpty(artifactId, "artifactId");
        this.groupId = groupId.trim();
        this.artifactId = artifactId.trim();
        this.classifier = classifier != null ? classifier.trim() : "";
        this.version = version != null ? new Version(version) : new Version("");
    }

    /**
     * Create a Maven ID from the supplied group and artifact IDs.
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @throws IllegalArgumentException if the group or artifact identifiers are null, empty or blank
     */
    public MavenId( String groupId, String artifactId ) {
        this(groupId, artifactId, null, null);
    }

    /**
     * Create a Maven ID from the supplied group and artifact IDs and the version.
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version; may be null or empty
     * @throws IllegalArgumentException if the group or artifact identifiers are null, empty or blank
     */
    public MavenId( String groupId, String artifactId, String version ) {
        this(groupId, artifactId, version, null);
    }

    /**
     * Create a Maven ID from the supplied group ID, artifact ID, version, and classifier.
     * @param groupId the group identifier
     * @param artifactId the artifact identifier
     * @param version the version; may be null or empty
     * @param classifier the classifier; may be null or empty
     * @throws IllegalArgumentException if the group or artifact identifiers are null, empty or blank
     */
    public MavenId( String groupId, String artifactId, String version, String classifier ) {
        CheckArg.isNotEmpty(groupId, "groupId");
        CheckArg.isNotEmpty(artifactId, "artifactId");
        this.groupId = groupId.trim();
        this.artifactId = artifactId.trim();
        this.classifier = classifier != null ? classifier.trim() : "";
        this.version = version != null ? new Version(version) : new Version("");
    }

    /**
     * A universally unique identifier for a project. It is normal to use a fully-qualified package name to distinguish it from
     * other projects with a similar name (eg. <code>org.apache.maven</code>).
     * @return the group identifier
     */
    public String getGroupId() {
        return this.groupId;
    }

    /**
     * The identifier for this artifact that is unique within the group given by the group ID. An artifact is something that is
     * either produced or used by a project. Examples of artifacts produced by Maven for a project include: JARs, source and
     * binary distributions, and WARs.
     * @return the artifact identifier
     */
    public String getArtifactId() {
        return this.artifactId;
    }

    /**
     * @return classifier
     */
    public String getClassifier() {
        return this.classifier;
    }

    /**
     * @return version
     */
    public String getVersion() {
        return this.version.toString();
    }

    /**
     * Return the relative JCR path for this resource, built from the components of the {@link #getGroupId() group ID}, the
     * {@link #getArtifactId() artifact ID}, and the {@link #getVersion() version}.
     * @return the path; never null
     */
    public String getRelativePath() {
        return getRelativePath(NoOpEncoder.getInstance());
    }

    /**
     * Return the relative JCR path for this resource, built from the components of the {@link #getGroupId() group ID}, the
     * {@link #getArtifactId() artifact ID}, and the {@link #getVersion() version}.
     * @param escapingStrategy the strategy to use for escaping characters that are not allowed in JCR names.
     * @return the path; never null
     */
    public String getRelativePath( TextEncoder escapingStrategy ) {
        return getRelativePath(NoOpEncoder.getInstance(), true);
    }

    /**
     * Return the relative JCR path for this resource, built from the components of the {@link #getGroupId() group ID}, the
     * {@link #getArtifactId() artifact ID}, and the {@link #getVersion() version}.
     * @param includeVersion true if the version is to be included in the path
     * @return the path; never null
     */
    public String getRelativePath( boolean includeVersion ) {
        return getRelativePath(NoOpEncoder.getInstance(), includeVersion);
    }

    /**
     * Return the relative JCR path for this resource, built from the components of the {@link #getGroupId() group ID}, the
     * {@link #getArtifactId() artifact ID}, and the {@link #getVersion() version}.
     * @param escapingStrategy the strategy to use for escaping characters that are not allowed in JCR names.
     * @param includeVersion true if the version is to be included in the path
     * @return the path; never null
     */
    public String getRelativePath( TextEncoder escapingStrategy, boolean includeVersion ) {
        StringBuilder sb = new StringBuilder();
        String[] groupComponents = this.getGroupId().split("[\\.]");
        for (String groupComponent : groupComponents) {
            if (sb.length() != 0) sb.append("/");
            sb.append(escapingStrategy.encode(groupComponent));
        }
        sb.append("/").append(escapingStrategy.encode(this.getArtifactId()));
        if (includeVersion) {
            sb.append("/").append(escapingStrategy.encode(this.getVersion()));
        }
        return sb.toString();
    }

    public String getCoordinates() {
        return StringUtil.createString("{0}:{1}:{2}:{3}", this.groupId, this.artifactId, this.version, this.classifier);
    }

    public static MavenId createFromCoordinates( String coordinates ) {
        String[] parts = coordinates.split("[:]");
        String groupId = null;
        String artifactId = null;
        String version = null;
        String classifier = null;
        if (parts.length > 0) groupId = parts[0];
        if (parts.length > 1) artifactId = parts[1];
        if (parts.length > 2) version = parts[2];
        if (parts.length > 3) classifier = parts[3];
        return new MavenId(groupId, artifactId, classifier, version);
    }

    protected boolean isAnyVersion() {
        return this.version.isAnyVersion();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        // The version is excluded from the hash code so that the 'any version' will be in the same bucket of a hash table
        return HashCode.compute(this.groupId, this.artifactId, this.classifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj instanceof MavenId) {
            MavenId that = (MavenId)obj;
            if (!this.groupId.equalsIgnoreCase(that.groupId)) return false;
            if (!this.artifactId.equalsIgnoreCase(that.artifactId)) return false;
            if (!this.version.equals(that.version)) return false;
            if (!this.classifier.equalsIgnoreCase(that.classifier)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo( MavenId that ) {
        if (that == null) return 1;
        if (this == that) return 0;

        // Check the group ID ...
        int diff = this.groupId.compareTo(that.groupId);
        if (diff != 0) return diff;

        // then the artifact ID ...
        diff = this.artifactId.compareTo(that.artifactId);
        if (diff != 0) return diff;

        // then the version ...
        diff = this.version.compareTo(that.version);
        if (diff != 0) return diff;

        // then the classifier ...
        diff = this.classifier.compareTo(that.classifier);
        return diff;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.getCoordinates();
    }

    public class Version implements Comparable<Version> {

        private final String version;
        private final Object[] components;

        protected Version( String version ) {
            this.version = version != null ? version.trim() : "";
            this.components = getVersionComponents(this.version);
        }

        /**
         * @return components
         */
        public Object[] getComponents() {
            return this.components;
        }

        public boolean isAnyVersion() {
            return this.version.length() == 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return version;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.version.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public int compareTo( Version that ) {
            if (that == null) return 1;
            Object[] thisComponents = this.getComponents();
            Object[] thatComponents = that.getComponents();
            int thisLength = thisComponents.length;
            int thatLength = thatComponents.length;
            int minLength = Math.min(thisLength, thatLength);
            for (int i = 0; i != minLength; ++i) {
                Object thisComponent = thisComponents[i];
                Object thatComponent = thatComponents[i];
                int diff = 0;
                if (thisComponent instanceof Integer && thatComponent instanceof Integer) {
                    diff = ((Integer)thisComponent).compareTo((Integer)thatComponent);
                } else {
                    String thisString = thisComponent.toString();
                    String thatString = thatComponent.toString();
                    diff = thisString.compareToIgnoreCase(thatString);
                }
                if (diff != 0) return diff;
            }
            return 0;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof Version) {
                Version that = (Version)obj;
                if (this.isAnyVersion() || that.isAnyVersion()) return true;
                if (!this.version.equalsIgnoreCase(that.version)) return false;
                return true;
            }
            return false;
        }
    }

    /**
     * Utility to break down the version string into the individual components. This utility splits the supplied version on
     * periods ('.'), dashes ('-'), forward slashes ('/'), and commas (',').
     * @param version the version string
     * @return the array of {@link String} and {@link Integer} components; never null
     */
    protected static Object[] getVersionComponents( String version ) {
        if (version == null) return new Object[] {};
        version = version.trim();
        if (version.length() == 0) return new Object[] {};
        String[] parts = version.split("[\\.\\-/,]");
        if (parts == null) return new Object[] {};
        Object[] components = new Object[parts.length];
        for (int i = 0, len = parts.length; i < len; i++) {
            String part = parts[i].trim();
            Object component = part;
            try {
                component = Integer.parseInt(part);
            } catch (NumberFormatException e) {
                // If there are any problems, we don't treat it as an integer
            }
            components[i] = component;
        }
        return components;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MavenId clone() {
        return new MavenId(this.groupId, this.artifactId, this.version.toString(), this.classifier);
    }
}
