package org.modeshape.jcr;

import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Path;

public interface JcrSystemObserver extends Observer {

    Path getObservedRootPath();

}
