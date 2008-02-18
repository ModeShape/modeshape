/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.maven;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jboss.dna.common.text.ITextEncoder;
import org.jboss.dna.common.text.NoOpEncoder;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.HashCodeUtil;
import org.jboss.dna.common.util.StringUtil;

/**
 * Identifier of a Maven 2 artifact.
 */
public class MavenId implements Comparable<MavenId>, Cloneable {

    private final String groupId;
    private final String artifactId;
    private final Version version;
    private final String classifier;

    public MavenId( String coordinates ) {
        if (coordinates == null) throw new IllegalArgumentException("The coordinates reference may not be null");
        coordinates = coordinates.trim();
        ArgCheck.isNotEmpty(coordinates, "coordinates");

        // This regular expression has the following groups:
        // 1) groupId
        // 2) :artifactId
        // 3) artifactId
        // 4) :version
        // 5) version
        // 6) :classifier
        // 7) classifier
        Pattern urlPattern = Pattern.compile("([^:]+)(:([^:]+)(:([^:]+)(:([^:]+))?)?)?");
        Matcher matcher = urlPattern.matcher(coordinates);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unsupported format: " + coordinates);
        }
        String groupId = matcher.group(1);
        String artifactId = matcher.group(3);
        String version = matcher.group(5);
        String classifier = matcher.group(7);
        ArgCheck.isNotEmpty(groupId, "groupId");
        ArgCheck.isNotEmpty(artifactId, "artifactId");
        this.groupId = groupId.trim();
        this.artifactId = artifactId.trim();
        this.classifier = classifier != null ? classifier.trim() : "";
        this.version = version != null ? new Version(version) : new Version("");
    }

    public MavenId( String groupId, String artifactId ) {
        this(groupId, artifactId, null, null);
    }

    public MavenId( String groupId, String artifactId, String version ) {
        this(groupId, artifactId, version, null);
    }

    public MavenId( String groupId, String artifactId, String version, String classifier ) {
        ArgCheck.isNotEmpty(groupId, "groupId");
        ArgCheck.isNotEmpty(artifactId, "artifactId");
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
    public String getRelativePath( ITextEncoder escapingStrategy ) {
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
    public String getRelativePath( ITextEncoder escapingStrategy, boolean includeVersion ) {
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
        return StringUtil.createString("{1}:{2}:{3}:{4}", this.groupId, this.artifactId, this.version, this.classifier);
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
        return HashCodeUtil.computeHash(this.groupId, this.artifactId, this.classifier);
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
