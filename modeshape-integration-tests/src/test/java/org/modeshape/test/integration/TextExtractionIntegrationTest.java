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

import java.io.ByteArrayInputStream;
import javax.jcr.Session;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.extractor.tika.TikaTextExtractorTest;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.test.ModeShapeUnitTest;

/**
 * Integration test class around text extraction.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class TextExtractionIntegrationTest extends ModeShapeUnitTest {

    @Test
    @FixFor( "MODE-1561" )
    public void shouldAllowConfigurableTikaWriteLimit() throws Exception {
        JcrEngine engine = startEngineUsing("config/configRepositoryForTextExtraction.xml");
        Session session = engine.getRepository("Content").login();

        //configured in the cfg file
        int configuredWriteLimit = 100;

        //generate a string the size of the configured limit and check that it's been indexed
        String randomString = TikaTextExtractorTest.randomString(configuredWriteLimit);
        tools().uploadFile(session, "testFile", new ByteArrayInputStream(randomString.getBytes()));
        session.save();

        //test text extraction via querying, since that's where it's actually used
        String sql = "select [jcr:path] from [nt:base] where contains([nt:base].*, '" + randomString + "')";
        tools().printQuery(session, sql, 1);

        //generate a string larger than the limit and check that it hasn't been indexed
        randomString = TikaTextExtractorTest.randomString(configuredWriteLimit + 1);
        tools().uploadFile(session, "testFile1", new ByteArrayInputStream(randomString.getBytes()));
        session.save();

        sql = "select [jcr:path] from [nt:base] where contains([nt:base].*, '" + randomString + "')";
        tools().printQuery(session, sql, 0);
    }
}
