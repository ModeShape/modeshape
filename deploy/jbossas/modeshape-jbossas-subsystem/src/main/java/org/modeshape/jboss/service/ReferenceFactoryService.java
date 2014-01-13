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
package org.modeshape.jboss.service;

import org.jboss.as.naming.ManagedReference;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ValueManagedReference;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;

public class ReferenceFactoryService<T> implements Service<ManagedReferenceFactory>, ManagedReferenceFactory {
    private final InjectedValue<T> injector = new InjectedValue<T>();

    private ManagedReference reference;

    @Override
    public synchronized void start( StartContext startContext ) {
        reference = new ValueManagedReference(new ImmediateValue<Object>(injector.getValue()));
    }

    @Override
    public synchronized void stop( StopContext stopContext ) {
        reference = null;
    }

    @Override
    public synchronized ManagedReferenceFactory getValue() throws IllegalStateException, IllegalArgumentException {
        return this;
    }

    @Override
    public synchronized ManagedReference getReference() {
        return reference;
    }

    public Injector<T> getInjector() {
        return injector;
    }
}
