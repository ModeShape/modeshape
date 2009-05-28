package org.jboss.dna.web.jcr.rest.model;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement( name = "repository" )
public class RepositoryEntry {

    private String name;
    private RepositoryResources resources;

    public RepositoryEntry() {
        resources = new RepositoryResources();
    }

    public RepositoryEntry( String contextName,
                            String repositoryName ) {
        this.name = repositoryName;

        resources = new RepositoryResources(contextName, repositoryName);
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public RepositoryResources getResources() {
        return resources;
    }
}
