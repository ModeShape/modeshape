package org.infinispan.schematic.internal.document;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.concurrent.atomic.AtomicReference;
import org.infinispan.schematic.document.Document;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class JsonPerformanceTest {

    protected JsonReader reader;
    protected boolean print;

    @Before
    public void beforeTest() {
        reader = new JsonReader();
    }

    @After
    public void afterTest() {
        reader = null;
        gc();
    }

    @Test
    public void shouldReadLargeTestDocumentWithoutDateMatching() throws Exception {
        testReadingJson(getLargeTestData(), 200, false, false);
    }

    @Test
    public void shouldReadLargeTestDocumentWithDateMatching() throws Exception {
        testReadingJson(getLargeTestData(), 200, false, true);
    }

    @Test
    public void shouldReadLargeTestDocumentWithoutDataMatchingUsingJsonSimple() throws Exception {
        testReadingJsonWithJsonSimple(getLargeTestData(), 200, false);
    }

    @Test
    public void shouldReadSmallTestDocumentWithoutDateMatching() throws Exception {
        testReadingJson(getSmallTestData(), 200, false, false);
    }

    @Test
    public void shouldReadSmallTestDocumentWithDateMatching() throws Exception {
        testReadingJson(getSmallTestData(), 200, false, true);
    }

    @Test
    public void shouldReadSmallTestDocumentWithoutDataMatchingUsingJsonSimple() throws Exception {
        testReadingJsonWithJsonSimple(getSmallTestData(), 200, false);
    }

    // @Test
    // public void shouldReadLargeTestDocumentToAndFromBson() throws Exception {
    // testReadingJsonThenWritingBsonThenReadingBson(getLargeTestData(), 200, false, true);
    // }

    @Test
    public void shouldReadSmallTestDocumentToAndFromBson() throws Exception {
        testReadingJsonThenWritingBsonThenReadingBson(getSmallTestData(), 200, false, true);
    }

    public static String getLargeTestData() throws Exception {
        String result = read("sample-large-performance.json");
        assert result.length() != 0;
        return result;
    }

    public static String getSmallTestData() throws Exception {
        String result = read("sample-small-performance.json");
        assert result.length() != 0;
        return result;
    }

    private static String read( String resourcePath ) throws IOException {
        return read(JsonPerformanceTest.class.getClassLoader().getResourceAsStream(resourcePath));
    }

    private static String read( InputStream stream ) throws IOException {
        return read(new InputStreamReader(stream));
    }

    private static String read( Reader reader ) throws IOException {
        if (reader == null) return "";
        StringBuilder sb = new StringBuilder();
        try {
            int numRead = 0;
            char[] buffer = new char[1024];
            while ((numRead = reader.read(buffer)) > -1) {
                sb.append(buffer, 0, numRead);
            }
        } finally {
            reader.close();
        }
        return sb.toString();
    }

    private static void gc() {
        try {
            Thread.sleep(500L);
        } catch (InterruptedException ie) {
        }
        System.gc();
        try {
            Thread.sleep(500L);
        } catch (InterruptedException ie) {
        }
    }

    private static void runTest( String message,
                                 int loops,
                                 int testDataLength,
                                 boolean print,
                                 Runnable function ) {
        assert loops > 0;
        // Run several times to warm up ...
        for (int i = 0; i != 3; ++i) {
            function.run();
        }
        long stop = 0L;
        long start = System.nanoTime();
        try {
            for (int i = 0; i != loops; ++i) {
                function.run();
            }
        } finally {
            stop = System.nanoTime();
        }
        if (print) {
            while (message.length() < 60)
                message = message + " ";
            System.out.print(message + " -- data length: " + testDataLength);
            System.out.print(" -- average time: ");
            double delta = (stop - start) / (loops + 0D);
            String unit = "nanos";
            if (delta > 1000.0) {
                delta /= 1000000;
                unit = "millis";
            }
            System.out.println(delta + " " + unit);
        }
    }

    protected void testReadingJson( final String testData,
                                    int numberOfRuns,
                                    final boolean getValue,
                                    final boolean introspectStringValues ) throws Exception {
        assert testData != null;
        final JsonReader reader = new JsonReader();
        final AtomicReference<Exception> error = new AtomicReference<Exception>();
        final String key = "key";
        runTest("JsonReader.read(String," + introspectStringValues + ")", numberOfRuns, testData.length(), print, new Runnable() {
            @Override
            public void run() {
                try {
                    Document doc = reader.read(testData, introspectStringValues);
                    if (getValue) doc.get(key);
                } catch (Exception t) {
                    error.compareAndSet(null, t);
                }
            }
        });
        if (error.get() != null) throw error.get();
    }

    protected void testReadingJsonWithJsonSimple( final String testData,
                                                  int numberOfRuns,
                                                  final boolean getValue ) throws Exception {
        final org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
        final AtomicReference<Exception> error = new AtomicReference<Exception>();
        final String key = "key";
        runTest("org.json.simple.parser.JSONParser().parse(Reader)", numberOfRuns, testData.length(), print, new Runnable() {
            @Override
            public void run() {
                try {
                    org.json.simple.JSONObject obj = (org.json.simple.JSONObject)parser.parse(new StringReader(testData));
                    if (getValue) obj.get(key);
                } catch (Exception t) {
                    error.compareAndSet(null, t);
                }
            }
        });
        if (error.get() != null) throw error.get();
    }

    protected void testReadingJsonThenWritingBsonThenReadingBson( final String testData,
                                                                  int numberOfRuns,
                                                                  final boolean getValue,
                                                                  final boolean introspectStringValues ) throws Exception {
        assert testData != null;
        // Read the JSON into in-memory ...
        Document doc = new JsonReader().read(testData, introspectStringValues);

        // Write the in-memory out to BSON ...
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new BsonWriter().write(doc, bytes);
        final byte[] bsonBytes = bytes.toByteArray();

        final BsonReader bsonReader = new BsonReader();
        final AtomicReference<Exception> error = new AtomicReference<Exception>();
        final String key = "key";
        runTest("BsonReader.read(InputStream)", numberOfRuns, testData.length(), print, new Runnable() {
            @Override
            public void run() {
                try {
                    ByteArrayInputStream input = new ByteArrayInputStream(bsonBytes);
                    Document doc = bsonReader.read(input);
                    doc.get(key);
                } catch (Exception t) {
                    error.compareAndSet(null, t);
                }
            }
        });
        if (error.get() != null) throw error.get();
    }
}
