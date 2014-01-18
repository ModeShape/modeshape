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
package org.modeshape.connector.filesystem;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * @author johnament
 */
public class InclusionExclusionFilenameFilterTest {

    private InclusionExclusionFilenameFilter filter;

    @Before
    public void setUp() {
        filter = new InclusionExclusionFilenameFilter();
    }

    @Test
    public void testEmptyFilter() {
        assertTrue(filter.accept(null, "anystring"));
    }

    @Test
    public void testInclusionOnly() {
        filter.setInclusionPattern("(.+)\\.mode");
        assertTrue(filter.accept(null, "myfile.mode"));
        assertFalse(filter.accept(null, "anotherfile.txt"));
    }

    @Test
    public void testExclusionOnly() {
        filter.setExclusionPattern("(.+)\\.mode");
        assertFalse(filter.accept(null, "myfile.mode"));
        assertTrue(filter.accept(null, "anotherfile.txt"));
    }

    @Test
    public void testInclusionExclusion() {
        filter.setInclusionPattern("(.+)\\.mode");
        filter.setExclusionPattern("ignore_me(.+)\\.mode");
        assertTrue(filter.accept(null, "validfile.mode"));
        assertFalse(filter.accept(null, "ignore_meinvalidfile.mode"));
    }

}
