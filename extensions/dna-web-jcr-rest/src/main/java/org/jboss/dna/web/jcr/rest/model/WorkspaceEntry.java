package org.jboss.dna.web.jcr.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "workspace" )
public class WorkspaceEntry {

    private String name;
    private WorkspaceResources resources;

    public WorkspaceEntry() {

    }

    public WorkspaceEntry( String contextName,
                           String repositoryName,
                           String workspaceName ) {
        this.name = workspaceName;

        resources = new WorkspaceResources(contextName, repositoryName, workspaceName);
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public WorkspaceResources getResources() {
        return resources;
    }
}

