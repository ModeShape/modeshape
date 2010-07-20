package org.modeshape.jcr;

import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Path;

/**
 * An interface for observers of the "/jcr:system" content.
 */
interface JcrSystemObserver extends Observer {

    /**
     * Get the (absolute) path in the "/jcr:system" subgraph below which this observer is interested in changes.
     * 
     * @return the observed path in the system content; may not be null
     */
    Path getObservedPath();

}
