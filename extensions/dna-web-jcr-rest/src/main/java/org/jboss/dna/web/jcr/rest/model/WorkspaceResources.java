package org.jboss.dna.web.jcr.rest.model;

import javax.xml.bind.annotation.XmlElement;

public class WorkspaceResources {
    private String baseUri;

    public WorkspaceResources() {
    }

    public WorkspaceResources( String contextName,
                               String repositoryName,
                               String workspaceName ) {
        this.baseUri = contextName + "/" + repositoryName + "/" + workspaceName;
    }

    @XmlElement( name = "items" )
    public String getWorkspaces() {
        return baseUri + "/items";
    }
}

