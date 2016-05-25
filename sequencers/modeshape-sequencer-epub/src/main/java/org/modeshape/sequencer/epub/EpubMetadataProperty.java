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
package org.modeshape.sequencer.epub;

/**
 * Class representing one metadata property.
 * 
 * @since 5.1
 */
public class EpubMetadataProperty {

    private String name;
    private String value;
    private String titleType;
    private String metadataAuthority;
    private String role;
    private Long displaySeq;
    private String fileAs;
    private Long groupPosition;
    private String identifierType;
    private String scheme;
    private AlternateScript alternateScript;

    public String getName() {
        return name;
    }

    public void setName( String name ) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue( String value ) {
        this.value = value;
    }

    public String getTitleType() {
        return titleType;
    }

    public void setTitleType( String titleType ) {
        this.titleType = titleType;
    }

    public String getMetadataAuthority() {
        return metadataAuthority;
    }

    public void setMetadataAuthority( String metadataAuthority ) {
        this.metadataAuthority = metadataAuthority;
    }

    public String getRole() {
        return role;
    }

    public void setRole( String role ) {
        this.role = role;
    }

    public Long getDisplaySeq() {
        return displaySeq;
    }

    public void setDisplaySeq( Long displaySeq ) {
        this.displaySeq = displaySeq;
    }

    public String getFileAs() {
        return fileAs;
    }

    public void setFileAs( String fileAs ) {
        this.fileAs = fileAs;
    }

    public Long getGroupPosition() {
        return groupPosition;
    }

    public void setGroupPosition( Long groupPosition ) {
        this.groupPosition = groupPosition;
    }

    public String getIdentifierType() {
        return identifierType;
    }

    public void setIdentifierType( String identifierType ) {
        this.identifierType = identifierType;
    }

    public String getScheme() {
        return scheme;
    }

    public void setScheme( String scheme ) {
        this.scheme = scheme;
    }

    public AlternateScript getAlternateScript() {
        return alternateScript;
    }

    public void setAlternateScript( AlternateScript alternateScript ) {
        this.alternateScript = alternateScript;
    }

    /**
     * Class representing the alternative-script refinement.
     */
    public static class AlternateScript {
        String value;
        String language;

        public AlternateScript( String value,
                                String language ) {
            this.value = value;
            this.language = language;
        }

        public String getValue() {
            return value;
        }

        public String getLanguage() {
            return language;
        }
    }

}
