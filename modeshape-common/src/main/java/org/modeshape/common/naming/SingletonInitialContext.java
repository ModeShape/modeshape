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
package org.modeshape.common.naming;

import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.Binding;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NameAlreadyBoundException;
import javax.naming.NameClassPair;
import javax.naming.NameNotFoundException;
import javax.naming.NameParser;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.spi.InitialContextFactory;
import org.modeshape.common.SystemFailureException;

/**
 * A simple and limited {@link Context JNDI naming context} that can be used in unit tests for code that
 * {@link Context#lookup(String) looks up} objects.
 * <p>
 * This can be used easily in a unit test by either using one of two methods. The first is using the convenient static
 * <code>configure</code> method that takes one, two or three name/object pairs:
 * 
 * <pre>
 * SingletonInitialContext.register(name, obj);
 * SingletonInitialContext.register(name1, obj1, name2, obj2);
 * SingletonInitialContext.register(name1, obj1, name2, obj2, name3, obj3);
 * </pre>
 * 
 * </p>
 * <p>
 * The other approach is to set the system property for the {@link InitialContextFactory}:
 * 
 * <pre>
 * System.setProperty(&quot;java.naming.factory.initial&quot;, &quot;org.modeshape.common.mock.SingletonInitialContextFactory&quot;);
 * </pre>
 * 
 * and then to {@link Context#bind(String, Object) bind} an object.
 * 
 * @see SingletonInitialContextFactory
 * @author Luca Stancapiano
 * @author Randall Hauch
 */
public class SingletonInitialContext implements Context {

    /**
     * A convenience method that registers the supplied object with the supplied name.
     * 
     * @param name the JNDI name
     * @param obj the object to be registered
     */
    public static void register( String name,
                                 Object obj ) {
        register(name, obj, null, null, null, null);
    }

    /**
     * A convenience method that registers the supplied objects with the supplied names.
     * 
     * @param name1 the JNDI name for the first object
     * @param obj1 the first object to be registered
     * @param name2 the JNDI name for the second object
     * @param obj2 the second object to be registered
     */
    public static void register( String name1,
                                 Object obj1,
                                 String name2,
                                 Object obj2 ) {
        register(name1, obj1, name2, obj2, null, null);
    }

    /**
     * A convenience method that registers the supplied objects with the supplied names.
     * 
     * @param name1 the JNDI name for the first object
     * @param obj1 the first object to be registered
     * @param name2 the JNDI name for the second object
     * @param obj2 the second object to be registered
     * @param name3 the JNDI name for the third object
     * @param obj3 the third object to be registered
     */
    public static void register( String name1,
                                 Object obj1,
                                 String name2,
                                 Object obj2,
                                 String name3,
                                 Object obj3 ) {
        SingletonInitialContextFactory.initialize();
        try {
            javax.naming.InitialContext context = new javax.naming.InitialContext();
            if (name1 != null) context.rebind(name1, obj1);
            if (name2 != null) context.rebind(name2, obj2);
            if (name3 != null) context.rebind(name3, obj3);
        } catch (NamingException e) {
            throw new SystemFailureException("Unable to create the mock InitialContext", e);
        }
    }

    private final Map<String, Object> environment = new ConcurrentHashMap<String, Object>();
    private final ConcurrentHashMap<String, Object> registry = new ConcurrentHashMap<String, Object>();

    /* package */SingletonInitialContext( Hashtable<?, ?> environment ) {
        if (environment != null) {
            for (Map.Entry<?, ?> entry : environment.entrySet()) {
                this.environment.put(entry.getKey().toString(), entry.getValue());
            }
        }
    }

    @Override
    public Object addToEnvironment( String propName,
                                    Object propVal ) {
        return environment.put(propName, propVal);
    }

    @Override
    public Object removeFromEnvironment( String propName ) {
        return environment.remove(propName);
    }

    @Override
    public void bind( Name name,
                      Object obj ) throws NamingException {
        bind(name.toString(), obj);
    }

    @Override
    public void bind( String name,
                      Object obj ) throws NamingException {
        if (this.registry.putIfAbsent(name, obj) != null) {
            throw new NameAlreadyBoundException("The name \"" + name + "\" is already bound to an object");
        }
    }

    @Override
    public void rebind( Name name,
                        Object obj ) {
        rebind(name.toString(), obj);
    }

    @Override
    public void rebind( String name,
                        Object obj ) {
        this.registry.put(name, obj);
    }

    @Override
    public void unbind( String name ) {
        this.registry.remove(name);
    }

    @Override
    public void unbind( Name name ) {
        unbind(name.toString());
    }

    @Override
    public Object lookup( Name name ) throws NamingException {
        return lookup(name.toString());
    }

    @Override
    public Object lookup( String name ) throws NamingException {
        Object result = this.registry.get(name);
        if (result == null) {
            throw new NameNotFoundException("No object is registered at \"" + name + "\"");
        }
        return result;
    }

    @Override
    public Object lookupLink( String name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object lookupLink( Name name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rename( Name oldName,
                        Name newName ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void rename( String oldName,
                        String newName ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
    }

    @Override
    public Name composeName( Name name,
                             Name prefix ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String composeName( String name,
                               String prefix ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext( Name name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context createSubcontext( String name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext( Name name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void destroySubcontext( String name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Hashtable<?, ?> getEnvironment() {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        Map<?, ?> map = this.environment;
        for (Map.Entry<?, ?> dd : map.entrySet()) {
            hashtable.put(dd.getKey().toString(), dd.getValue().toString());
        }
        return hashtable;
    }

    @Override
    public String getNameInNamespace() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser( Name name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NameParser getNameParser( String name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list( Name name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<NameClassPair> list( String name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings( Name name ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public NamingEnumeration<Binding> listBindings( String name ) {
        throw new UnsupportedOperationException();
    }
}
