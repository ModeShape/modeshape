package org.modeshape.jcr.cache.change;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.modeshape.common.annotation.ThreadSafe;

@ThreadSafe
public class MultiplexingChangeSetListener implements ChangeSetListener, Observable {

    private final Set<ChangeSetListener> delegates = new CopyOnWriteArraySet<ChangeSetListener>();

    public MultiplexingChangeSetListener() {
    }

    @Override
    public boolean register( ChangeSetListener listener ) {
        return listener != null ? delegates.add(listener) : false;
    }

    @Override
    public boolean unregister( ChangeSetListener listener ) {
        return listener != null ? delegates.remove(listener) : false;
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        for (ChangeSetListener listener : delegates) {
            listener.notify(changeSet);
        }
    }
}
