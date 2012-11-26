/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.modeshape.cmis.manager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author kulikov
 */
public class CopyTest {

    private Copy cp = new Copy();

    public CopyTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of copy method, of class Copy.
     */
    @Test
    public void testCopy() throws Exception {
        String path = getClass().getResource("/repository.properties").toURI().getPath();
        File src = new File(path);
        File dir = src.getParentFile();

        cp.copy("/repository.properties",dir, "jndi-name:java:///jcr/sample");

        FileInputStream in = new FileInputStream(dir.getAbsolutePath() + "/repository.properties");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        int b = 0;
        while (b != -1) {
            b = in.read();
            if (b != -1) {
                bout.write(b);
            }
        }
        String content = bout.toString();

        assertTrue(content.indexOf("java:///jcr/sample") > 0);
        assertTrue(content.indexOf("${jndi-name}") == -1);
    }

}
