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
package org.modeshape.connector.disk;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.UUID;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.connector.disk.DiskWorkspace.ValueReference;
import org.modeshape.connector.disk.DiskWorkspace.ValueReferences;

public class DiskWorkspaceValueReferencesTest {

    /**
     * This test can be used to decode the contents of a .ref file used in the Disk Storage connector. Simply remove the @Ignore
     * annotation, and change the code to load your .ref file.
     * 
     * @throws Exception
     */
    @Ignore
    @Test
    public void shouldReadValueReferencesFromFile() throws Exception {
        File file = new File("HngcdK4ZJBscYnKeQKRezG1cX7Y=.ref");
        ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file));
        ValueReferences valueRefs = (ValueReferences)ois.readObject();
        ois.close();
        System.out.println(valueRefs);
    }

    @Test
    public void shouldWriteAndReadValueReferencesFromFile() throws Exception {
        UUID uuid = UUID.randomUUID();
        ValueReference valueRef = new ValueReference("workspaceName", uuid);
        ValueReferences valueRefs = new ValueReferences(valueRef);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(valueRefs);
        oos.close();
        byte[] bytes = baos.toByteArray();

        // Now read it back in ...
        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
        ValueReferences valueRefs2 = (ValueReferences)ois.readObject();
        ois.close();

        assertThat(valueRefs.withoutReference(valueRef).hasRemainingReferences(), is(false));
        assertThat(valueRefs2.withoutReference(valueRef).hasRemainingReferences(), is(false));
        assertThat(valueRefs, is(valueRefs2));
    }
}
