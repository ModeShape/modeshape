package org.jboss.dna.web.jcr.rest.model;

import javax.xml.bind.annotation.XmlElement;

public class RepositoryResources {
    private String baseUri;

    public RepositoryResources() {
    }

    public RepositoryResources( String contextName,
                                String repositoryName ) {
        this.baseUri = contextName + "/" + repositoryName;
    }

    @XmlElement( name = "workspaces" )
    public String getWorkspaces() {
        return baseUri;
    }
}

