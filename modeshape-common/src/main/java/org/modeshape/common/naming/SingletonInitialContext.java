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
    static public void register( String name,
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
    static public void register( String name1,
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
    static public void register( String name1,
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

    /**
     * {@inheritDoc}
     */
    public Object addToEnvironment( String propName,
                                    Object propVal ) {
        return environment.put(propName, propVal);
    }

    /**
     * {@inheritDoc}
     */
    public Object removeFromEnvironment( String propName ) {
        return environment.remove(propName);
    }

    /**
     * {@inheritDoc}
     */
    public void bind( Name name,
                      Object obj ) throws NamingException {
        bind(name.toString(), obj);
    }

    /**
     * {@inheritDoc}
     */
    public void bind( String name,
                      Object obj ) throws NamingException {
        if (this.registry.putIfAbsent(name, obj) != null) {
            throw new NameAlreadyBoundException("The name \"" + name + "\" is already bound to an object");
        }
    }

    /**
     * {@inheritDoc}
     */
    public void rebind( Name name,
                        Object obj ) {
        rebind(name.toString(), obj);
    }

    /**
     * {@inheritDoc}
     */
    public void rebind( String name,
                        Object obj ) {
        this.registry.put(name, obj);
    }

    /**
     * {@inheritDoc}
     */
    public void unbind( String name ) {
        this.registry.remove(name);
    }

    /**
     * {@inheritDoc}
     */
    public void unbind( Name name ) {
        unbind(name.toString());
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup( Name name ) throws NamingException {
        return lookup(name.toString());
    }

    /**
     * {@inheritDoc}
     */
    public Object lookup( String name ) throws NamingException {
        Object result = this.registry.get(name);
        if (result == null) {
            throw new NameNotFoundException("No object is registered at \"" + name + "\"");
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink( String name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object lookupLink( Name name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void rename( Name oldName,
                        Name newName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void rename( String oldName,
                        String newName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void close() {
    }

    /**
     * {@inheritDoc}
     */
    public Name composeName( Name name,
                             Name prefix ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public String composeName( String name,
                               String prefix ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext( Name name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Context createSubcontext( String name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext( Name name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public void destroySubcontext( String name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Hashtable<?, ?> getEnvironment() {
        Hashtable<String, String> hashtable = new Hashtable<String, String>();
        Map<?, ?> map = this.environment;
        for (Map.Entry<?, ?> dd : map.entrySet()) {
            hashtable.put(dd.getKey().toString(), dd.getValue().toString());
        }
        return hashtable;
    }

    /**
     * {@inheritDoc}
     */
    public String getNameInNamespace() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser( Name name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public NameParser getNameParser( String name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<NameClassPair> list( Name name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<NameClassPair> list( String name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<Binding> listBindings( Name name ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public NamingEnumeration<Binding> listBindings( String name ) {
        throw new UnsupportedOperationException();
    }
}
