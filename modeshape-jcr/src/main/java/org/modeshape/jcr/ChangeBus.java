package org.modeshape.jcr;

import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.Observable;

/**
 * A generic interface for an event bus which handles changes.
 *
 * @author Horia Chiorean
 */
public interface ChangeBus extends ChangeSetListener, Observable {
}
