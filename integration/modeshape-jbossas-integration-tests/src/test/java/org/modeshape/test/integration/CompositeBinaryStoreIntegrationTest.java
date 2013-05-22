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

package org.modeshape.test.integration;

import javax.annotation.Resource;
import javax.jcr.Session;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.ValueFactory;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.CompositeBinaryStore;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Test which verifies that the ModeShape composite binary store configuration inside of AS7 is correct.
 *
 * @author Chris Beer
 */
@RunWith( Arquillian.class)
public class CompositeBinaryStoreIntegrationTest {


    @Resource( mappedName = "/jcr/compositeBinaryStoreRepository" )
    private JcrRepository repository;

    private Session session;

    @Deployment
    public static WebArchive createDeployment() {
        return ShrinkWrap.create(WebArchive.class, "composite-binary-store-test.war")
                         .setManifest(new File("src/main/webapp/META-INF/MANIFEST.MF"));
    }


    @Before
    public void before() throws Exception {
        assertNotNull("repository should not be null", repository);
    }

    @Test
    public void shouldStoreDataInEachOfTheNamedBinaryStores() throws Exception {

        session = repository.login();

        final Binary binaryInDefaultStore = ((ValueFactory) session.getValueFactory()).createBinary(new ByteArrayInputStream("abcdefghijklmnopqrstuvwxyz".getBytes()), "default");
        final Binary binaryInOtherStore = ((ValueFactory) session.getValueFactory()).createBinary(new ByteArrayInputStream("123456789101112".getBytes()), "other");
        final Binary binaryInAnotherStore = ((ValueFactory) session.getValueFactory()).createBinary(new ByteArrayInputStream("qwertyuiopasdfghjklzxcvbnm".getBytes()), "another-fsbs");

        final BinaryStore binaryStore = ((JcrRepository) repository).runningState().binaryStore();

        assertTrue(binaryStore instanceof CompositeBinaryStore);
        final Iterator<Map.Entry<String, BinaryStore>> namedStoreIterator = ((CompositeBinaryStore) binaryStore).getNamedStoreIterator();
        Map<String,BinaryStore> namedStoreMap = new HashMap<String,BinaryStore>();

        while (namedStoreIterator.hasNext()) {
            final Map.Entry<String, BinaryStore> entry = namedStoreIterator.next();
            namedStoreMap.put(entry.getKey(), entry.getValue());
        }

        assertTrue(namedStoreMap.get("default").hasBinary(((BinaryValue)binaryInDefaultStore).getKey()));
        assertTrue(namedStoreMap.get("other").hasBinary(((BinaryValue)binaryInOtherStore).getKey()));
        assertTrue(namedStoreMap.get("another-fsbs").hasBinary(((BinaryValue)binaryInAnotherStore).getKey()));
        session.save();
    }

}
