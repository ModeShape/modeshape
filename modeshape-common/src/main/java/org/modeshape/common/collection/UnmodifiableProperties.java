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
package org.modeshape.common.collection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;

/**
 * An immutable {@link Properties} implementation.
 */
@Immutable
public class UnmodifiableProperties extends Properties {

    private static final long serialVersionUID = -4670639332874922546L;
    private Properties delegate;

    public UnmodifiableProperties( Properties props ) {
        super();
        this.delegate = props != null ? props : new Properties();
    }

    @Override
    public synchronized Object clone() {
        return new UnmodifiableProperties(this.delegate);
    }

    @Override
    public synchronized boolean contains( Object value ) {
        return delegate.contains(value);
    }

    @Override
    public boolean containsKey( Object key ) {
        return this.delegate.containsKey(key);
    }

    @Override
    public boolean containsValue( Object value ) {
        return this.delegate.containsValue(value);
    }

    @Override
    public Enumeration<Object> elements() {
        return this.delegate.elements();
    }

    @Override
    public boolean equals( Object o ) {
        return this.delegate.equals(o);
    }

    @Override
    public Object get( Object key ) {
        return this.delegate.get(key);
    }

    @Override
    public String getProperty( String key,
                               String defaultValue ) {
        return this.delegate.getProperty(key, defaultValue);
    }

    @Override
    public String getProperty( String key ) {
        return this.delegate.getProperty(key);
    }

    @Override
    public int hashCode() {
        return this.delegate.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    @Override
    public Enumeration<Object> keys() {
        return this.delegate.keys();
    }

    @Override
    public void list( PrintStream out ) {
        this.delegate.list(out);
    }

    @Override
    public void list( PrintWriter out ) {
        this.delegate.list(out);
    }

    @Override
    public Enumeration<?> propertyNames() {
        return this.delegate.propertyNames();
    }

    @Override
    public int size() {
        return this.delegate.size();
    }

    @Override
    public void store( OutputStream out,
                       String comments ) throws IOException {
        this.delegate.store(out, comments);
    }

    @Override
    public void storeToXML( OutputStream os,
                            String comment,
                            String encoding ) throws IOException {
        this.delegate.storeToXML(os, comment, encoding);
    }

    @Override
    public void storeToXML( OutputStream os,
                            String comment ) throws IOException {
        this.delegate.storeToXML(os, comment);
    }

    @Override
    public String toString() {
        return this.delegate.toString();
    }

    @Override
    public Collection<Object> values() {
        return this.delegate.values();
    }

    @Override
    public synchronized void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<Object, Object>> entrySet() {
        return Collections.unmodifiableSet(super.entrySet());
    }

    @Override
    public Set<Object> keySet() {
        return Collections.unmodifiableSet(super.keySet());
    }

    @Override
    public synchronized void load( InputStream inStream ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void loadFromXML( InputStream in ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized Object put( Object key,
                                    Object value ) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void putAll( Map<? extends Object, ? extends Object> t ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object remove( Object key ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized Object setProperty( String key,
                                            String value ) {
        throw new UnsupportedOperationException();
    }

}
