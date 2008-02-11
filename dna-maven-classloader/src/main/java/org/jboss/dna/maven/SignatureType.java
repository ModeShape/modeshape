/*
 *
 */
package org.jboss.dna.maven;

/**
 * @author Randall Hauch
 */
public enum SignatureType {
    MD5(".md5"),
    SHA1(".sha1"),
    PGP(".asc");

    private String suffix;

    private SignatureType( String suffix ) {
        this.suffix = suffix;
    }

    public String getSuffix() {
        return this.suffix;
    }

    public static SignatureType valueBySuffix( String suffix ) {
        for (SignatureType type : SignatureType.values()) {
            if (type.suffix.equalsIgnoreCase(suffix)) return type;
        }
        return null;
    }

}
