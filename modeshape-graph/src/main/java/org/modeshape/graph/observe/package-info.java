/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/**
 * The Observation API provides several mechanisms for asynchronously observing changes to content.
 * <p>
 * Many event frameworks define the listeners and sources as interfaces.  While this is often useful, it requires
 * the implementations properly address the thread-safe semantics of managing and calling the listeners.
 * This observation framework uses abstract or concrete classes to minimize the effort required for implementing
 * {@link ChangeObserver} or {@link Observable}. The classes also allow the framework to implement a number of
 * utility methods, such as the {@link ChangeObserver#unregister() unregister()} method on ChangeObserver, that
 * also save effort and code.
 * </p>
 * <p>
 * However, one of the more important reasons for providing classes is that {@link ChangeObserver} uses 
 * {@link java.lang.ref.WeakReference weak references} to track the {@link Observable} instances, and the {@link ChangeObservers}
 * class uses weak references for the listeners.  This means that if an observers do not prevent {@link Observable} instances
 * from being garbage collected, nor do observers prevent {@link Observable} instances from being garbage collected.
 * </p>
 * <h3>Observable</h3>
 * <p>
 * Any component that can have changes and be observed can implement the {@link Observable} interface.  This interface
 * allows Observers to register (or be registered) to receive notifications of the changes.  However, a concrete and thread-safe 
 * implementation of this interface, called {@link ChangeObservers}, is available and should be used where possible, since it 
 * automatically manages the registered {@link ChangeObserver} instances and properly implements the register and unregister mechanisms.
 * </p>
 * <h3>Observers</h3>
 * <p>
 * Components that are to recieve notifications of changes are called <i>observers</i>.  To create an observer, simply extend 
 * the {@link ChangeObserver} abstract class and provide an implementation of the {@link ChangeObserver#notify(Changes)} method.
 * Then, register the observer with an {@link Observable} using its {@link Observable#register(Observer)} method.
 * The observer's {@link ChangeObserver#notify(Changes)} method will then be called with the changes that have
 * been made to the Observable.
 * </p>
 * <p>When an observer is no longer needed, it should be unregistered from all {@link Observable} instances with which
 * it was registered.  The {@link ChangeObserver} class automatically tracks which {@link Observable} instances it is
 * registered with, and calling the observer's {@link ChangeObserver#unregister()} will unregister the observer from
 * all of these Observables.  Alternatively, an observer can be unregistered from a single Observable using the
 * Observable's {@link Observable#unregister(Observer)} method.
 * </p>
 * <h3>Changes</h3>
 * <p>
 * The {@link Changes} class represents the set of individual changes that have been made during a single, atomic
 * operation.  Each {@link Changes} instance has information about the source of the changes, the timestamp at which
 * the changes occurred, and the individual changes that were made.  These individual changes take the form of
 * {@link org.modeshape.graph.request.ChangeRequest} objects, such as {@link org.modeshape.graph.request.CreateNodeRequest}, 
 * {@link org.modeshape.graph.request.DeleteBranchRequest}, etc.  Each request is 
 * {@link org.modeshape.graph.request.Request#isFrozen() frozen}, meaning it is immutable and will not change.  Also
 * none of the requests will be {@link org.modeshape.graph.request.Request#isCancelled() cancelled}.
 * </p>
 * <p>
 * Using the actual {@link org.modeshape.graph.request.ChangeRequest} objects as the "events" has a number of advantages.
 * First, there are already a number of existing {@link org.modeshape.graph.request.ChangeRequest} subclasses that describe
 * various types of changes in quite a bit of detail; thus no need to duplicate the structure or come up with a generic
 * event class.  
 * </p>
 * <p>
 * Second, the requests have all the state required for an event, plus they often will have more.  For example,
 * the DeleteBranchRequest has the actual location of the branch that was delete (and in this way is not much different than
 * a more generic event), but the CreateNodeRequest has the actual location of the created node along with the properties
 * of that node.  Additionally, the RemovePropertyRequest has the actual location of the node along with the name of the property
 * that was removed.  In many cases, these requests have all the information a more general event class might have but
 * then hopefully enough information for many observers to use directly without having to read the graph to decide what
 * actually changed. 
 * </p>
 * <p>
 * Third, the requests that make up a {@link Changes} instance can actually be replayed.  Consider the case of a cache
 * that is backed by a {@link org.modeshape.graph.connector.RepositorySource}, which might use an observer to keep the cache in sync.  
 * As the cache is notified of Changes, the cache can simply replay the changes against its source.
 */

package org.modeshape.graph.observe;

