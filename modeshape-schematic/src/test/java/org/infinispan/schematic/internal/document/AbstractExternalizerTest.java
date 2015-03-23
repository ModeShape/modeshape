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
package org.infinispan.schematic.internal.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.infinispan.commons.marshall.jboss.JBossExternalizerAdapter;
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
