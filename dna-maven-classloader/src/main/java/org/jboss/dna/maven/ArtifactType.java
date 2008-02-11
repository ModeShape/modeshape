/*
 *
 */
package org.jboss.dna.maven;

/**
 * @author Randall Hauch
 */
public enum ArtifactType {
    METADATA("maven-metadata.xml"),
    JAR(".jar"),
    SOURCE("-sources.jar"),
    POM(".pom");

    private String suffix;

    private ArtifactType( String suffix ) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public static ArtifactType valueBySuffix( String suffix ) {
        for (ArtifactType type : ArtifactType.values()) {
            if (type.suffix.equalsIgnoreCase(suffix)) return type;
        }
        return null;
    }
}
