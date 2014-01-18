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
package org.modeshape.jcr;

import java.util.Map;
import javax.transaction.Synchronization;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import org.infinispan.AdvancedCache;

/**
 * An extension of {@link JcrSession} that is also an {@link XAResource}, enabling clients to explicitly enlist the session in the
 * transaction.
 * <p>
 * This implementation simply delegates to the {@link AdvancedCache#getXAResource() Infinispan cache's XAResource} instance, since
 * ModeShape does not need to itself be a resource that participates in the transaction. (Instead, ModeShape registers as a
 * {@link Synchronization} on the transaction, ensuring that it is notified when the transaction completes.)
 * </p>
 */
public class JcrXaSession extends JcrSession implements XAResource {

    protected JcrXaSession( JcrRepository repository,
                            String workspaceName,
                            ExecutionContext context,
                            Map<String, Object> sessionAttributes,
                            boolean readOnly ) {
        super(repository, workspaceName, context, sessionAttributes, readOnly);
    }

    protected final XAResource delegate() {
        return repository.documentStore().xaResource();
    }

    @Override
    public void start( Xid xid,
                       int flags ) throws XAException {
        delegate().start(xid, flags);
    }

    @Override
    public void end( Xid xid,
                     int flags ) throws XAException {
        delegate().end(xid, flags);
    }

    @Override
    public int prepare( Xid xid ) throws XAException {
        return delegate().prepare(xid);
    }

    @Override
    public void commit( Xid xid,
                        boolean onePhase ) throws XAException {
        delegate().commit(xid, onePhase);
    }

    @Override
    public void rollback( Xid xid ) throws XAException {
        delegate().rollback(xid);
    }

    @Override
    public void forget( Xid xid ) throws XAException {
        delegate().forget(xid);
    }

    @Override
    public Xid[] recover( int flag ) throws XAException {
        return delegate().recover(flag);
    }

    @Override
    public boolean isSameRM( XAResource xaRes ) throws XAException {
        return delegate().isSameRM(xaRes);
    }

    @Override
    public int getTransactionTimeout() throws XAException {
        return delegate().getTransactionTimeout();
    }

    @Override
    public boolean setTransactionTimeout( int seconds ) throws XAException {
        return delegate().setTransactionTimeout(seconds);
    }

}
