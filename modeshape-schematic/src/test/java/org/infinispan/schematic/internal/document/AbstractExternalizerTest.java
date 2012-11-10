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
package org.infinispan.schematic.internal.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.infinispan.marshall.AdvancedExternalizer;
import org.infinispan.marshall.jboss.JBossExternalizerAdapter;
import org.infinispan.schematic.internal.SchematicEntryDelta;
import org.infinispan.schematic.internal.SchematicExternalizer;
import org.infinispan.schematic.internal.delta.AddValueIfAbsentOperation;
import org.infinispan.schematic.internal.delta.AddValueOperation;
import org.infinispan.schematic.internal.delta.ClearOperation;
import org.infinispan.schematic.internal.delta.PutIfAbsentOperation;
import org.infinispan.schematic.internal.delta.PutOperation;
import org.infinispan.schematic.internal.delta.RemoveAllValuesOperation;
import org.infinispan.schematic.internal.delta.RemoveAtIndexOperation;
import org.infinispan.schematic.internal.delta.RemoveOperation;
import org.infinispan.schematic.internal.delta.RemoveValueOperation;
import org.infinispan.schematic.internal.delta.RetainAllValuesOperation;
import org.infinispan.schematic.internal.delta.SetValueOperation;
import org.jboss.marshalling.ClassExternalizerFactory;
import org.jboss.marshalling.Externalizer;
import org.jboss.marshalling.MappingClassExternalizerFactory;
import org.jboss.marshalling.Marshaller;
import org.jboss.marshalling.MarshallerFactory;
import org.jboss.marshalling.Marshalling;
import org.jboss.marshalling.MarshallingConfiguration;
import org.jboss.marshalling.Unmarshaller;
import org.junit.BeforeClass;

public class AbstractExternalizerTest {

    private static MarshallerFactory marshallerFactory;
    private static MarshallingConfiguration configuration;
    private static final Map<Class<?>, Externalizer> externalizersByClass = new HashMap<Class<?>, Externalizer>();

    @BeforeClass
    public static void beforeAll() {
        // Get the factory for the "river" marshalling protocol
        marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");

        // Create a configuration
        configuration = new MarshallingConfiguration();
        // Use version 3
        configuration.setVersion(3);

        addExternalizer(new DocumentExternalizer());
        addExternalizer(new ArrayExternalizer());
        addExternalizer(new PutOperation.Externalizer());
        addExternalizer(new PutIfAbsentOperation.Externalizer());
        addExternalizer(new RemoveOperation.Externalizer());
        addExternalizer(new AddValueOperation.Externalizer());
        addExternalizer(new AddValueIfAbsentOperation.Externalizer());
        addExternalizer(new ClearOperation.Externalizer());
        addExternalizer(new RemoveValueOperation.Externalizer());
        addExternalizer(new RemoveAllValuesOperation.Externalizer());
        addExternalizer(new RemoveAtIndexOperation.Externalizer());
        addExternalizer(new RetainAllValuesOperation.Externalizer());
        addExternalizer(new SetValueOperation.Externalizer());
        addExternalizer(new SchematicEntryDelta.Externalizer());
        addExternalizer(new Paths.Externalizer());
        ClassExternalizerFactory externalizerFactory = new MappingClassExternalizerFactory(externalizersByClass);
        configuration.setClassExternalizerFactory(externalizerFactory);
    }

    protected static void addExternalizer( SchematicExternalizer<?> externalizer ) {
        Externalizer adapter = new JBossExternalizerAdapter(externalizer);
        for (Class<?> clazz : externalizer.getTypeClasses()) {
            externalizersByClass.put(clazz, adapter);
        }
    }

    protected static void addExternalizer( AdvancedExternalizer<?> externalizer ) {
        Externalizer adapter = new JBossExternalizerAdapter(externalizer);
        for (Class<?> clazz : externalizer.getTypeClasses()) {
            externalizersByClass.put(clazz, adapter);
        }
    }

    protected byte[] marshall( Object object ) throws IOException {
        final Marshaller marshaller = marshallerFactory.createMarshaller(configuration);
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try {
            marshaller.start(Marshalling.createByteOutput(os));
            marshaller.writeObject(object);
            marshaller.finish();
            os.close();
        } finally {
            // clean up stream resource
            os.close();
        }
        return os.toByteArray();
    }

    protected Object unmarshall( byte[] bytes ) throws IOException, ClassNotFoundException {
        final Unmarshaller unmarshaller = marshallerFactory.createUnmarshaller(configuration);
        final ByteArrayInputStream is = new ByteArrayInputStream(bytes);
        try {
            unmarshaller.start(Marshalling.createByteInput(is));
            Object result = unmarshaller.readObject();
            unmarshaller.finish();
            is.close();
            return result;
        } finally {
            // clean up stream resource
            try {
                is.close();
            } catch (IOException e) {
                System.err.print("Stream close failed: ");
                e.printStackTrace();
            }
        }
    }

}
