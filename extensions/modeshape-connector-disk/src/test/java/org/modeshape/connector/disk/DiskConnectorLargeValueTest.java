package org.modeshape.connector.disk;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.Base64;
import org.modeshape.common.util.FileUtil;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Node;
import org.modeshape.graph.connector.MockRepositoryContext;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.BinaryFactory;
import org.modeshape.graph.property.basic.FileSystemBinary;

public class DiskConnectorLargeValueTest {

    // Extremely low threshold for testing
    private final int LARGE_VALUE_THRESHOLD = 20;
    private final String REPO_ROOT_PATH = "./target/repoRootPath";

    private File repoRootPath;
    private File largeValuePath;

    protected ExecutionContext context;
    private BinaryFactory binFactory;
    private DiskSource source;
    private Graph graph;

    @Before
    public void beforeEach() {
        repoRootPath = new File(REPO_ROOT_PATH);
        if (repoRootPath.exists()) {
            FileUtil.delete(repoRootPath);
        }
        repoRootPath.mkdirs();

        context = new ExecutionContext();
        binFactory = context.getValueFactories().getBinaryFactory();

        source = new DiskSource();
        source.setName("Disk Source");
        source.setLargeValueSizeInBytes(LARGE_VALUE_THRESHOLD);
        source.setRepositoryRootPath(REPO_ROOT_PATH);
        source.initialize(new MockRepositoryContext(context));

        largeValuePath = new File(repoRootPath, source.getLargeValuePath());

        graph = Graph.create(source, context);
    }

    private byte[] createBinaryValueOfSize( int size ) {
        byte[] value = new byte[size];
        String seed = "The quick brown fox jumped over the lazy dog";
        int seedSize = seed.length();

        for (int i = 0; i < size; i++) {
            value[i] = (byte)seed.charAt(i % seedSize);
        }

        return value;
    }

    private String keyFor( byte[] value ) {
        try {
            return Base64.encodeBytes(value, Base64.URL_SAFE);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Test
    public void shouldNotStoreBinaryAsLargeValueWhenSizeDoesNotExceedThreshold() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD - 1));
        graph.create("/noLargeValue").with("binaryProp", binary).and();

        assertThat(largeValuePath.list().length, is(0));

        Node node = graph.getNodeAt("/noLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertFalse(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));
    }

    @Test
    public void shouldNotStoreBinaryAsLargeValueWhenSizeEqualsThreshold() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD));
        graph.create("/noLargeValue").with("binaryProp", binary).and();

        assertThat(largeValuePath.list().length, is(0));

        Node node = graph.getNodeAt("/noLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertFalse(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));
    }

    @Test
    public void shouldStoreBinaryAsLargeValueWhenSizeExceedsThreshold() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/oneLargeValue").with("binaryProp", binary).and();

        assertThat(largeValuePath.list().length, is(2));

        Node node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

    }

    @Test
    public void shouldReuseExistingIfPossibleLargeValueWhenSizeExceedsThreshold() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/oneLargeValue").with("binaryProp", binary).and();

        assertThat(largeValuePath.list().length, is(2));

        Node node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        graph.create("/secondLargeValue").with("binaryProp", binary).and();

        assertThat(largeValuePath.list().length, is(2));

        node = graph.getNodeAt("/secondLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

    }

    @Test
    public void shouldStoreSingleCopyOfMultipleValuesForSamePropertyAsLargeValuesWhenSizeExceedsThreshold() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/twoLargeValues").with("binaryProp", binary, binary).and();

        assertThat(largeValuePath.list().length, is(2));

        Node node = graph.getNodeAt("/twoLargeValues");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        assertTrue(node.getProperty("binaryProp").getValuesAsArray()[1] instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getValuesAsArray()[1] instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getValuesAsArray()[1], is(binary));

    }

    @Test
    public void shouldStoreMultipleValuesForSamePropertyAsLargeValuesWhenSizeExceedsThreshold() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        Binary binary2 = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 10));
        graph.create("/twoLargeValues").with("binaryProp", binary, binary2).and();

        assertThat(largeValuePath.list().length, is(4));

        Node node = graph.getNodeAt("/twoLargeValues");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        assertTrue(node.getProperty("binaryProp").getValuesAsArray()[1] instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getValuesAsArray()[1] instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getValuesAsArray()[1], is(binary2));

    }

    @Test
    public void shouldStoreOnlyValuesThatExceedThresholdAsLargeValues() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        Binary binary2 = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD - 1));
        graph.create("/twoLargeValues").with("binaryProp", binary, binary2).and();

        assertThat(largeValuePath.list().length, is(2));

        Node node = graph.getNodeAt("/twoLargeValues");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        assertTrue(node.getProperty("binaryProp").getValuesAsArray()[1] instanceof Binary);
        assertFalse(node.getProperty("binaryProp").getValuesAsArray()[1] instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getValuesAsArray()[1], is(binary2));

    }

    @Test
    public void shouldOnlyStoreLargeBinariesInLargeValueDirectory() {
        String largeStringValue = "Now is the time for all good men to come to the aid of their party";
        assertTrue(largeStringValue.length() > LARGE_VALUE_THRESHOLD);

        graph.create("/largeStringValue").with("stringProp", largeStringValue).and();

        assertThat(largeValuePath.list().length, is(0));

        Node node = graph.getNodeAt("/largeStringValue");
        assertThat(node.getProperty("stringProp"), is(notNullValue()));
        assertThat((String)node.getProperty("stringProp").getFirstValue(), is(largeStringValue));

    }

    @Test
    public void shouldRemoveLargeValueWhenNoLongerInUse() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/oneLargeValue").with("binaryProp", binary).and();

        Set<String> largeValuePaths = new HashSet<String>();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        String hash = keyFor(binary.getHash());
        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        Node node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        graph.remove("binaryProp").on("/oneLargeValue").and();

        assertThat(largeValuePath.list().length, is(0));

        node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(nullValue()));

    }

    @Test
    public void shouldNotRemoveLargeValueWhenStillInUseInSameNode() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/twoLargeValues").with("binaryProp", binary, binary).and();

        String hash = keyFor(binary.getHash());
        Set<String> largeValuePaths = new HashSet<String>();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        Node node = graph.getNodeAt("/twoLargeValues");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(2));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        graph.set("binaryProp").on("/twoLargeValues").to(binary).and();

        largeValuePaths.clear();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        node = graph.getNodeAt("/twoLargeValues");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

    }

    @Test
    public void shouldNotRemoveLargeValueWhenStillInUseInOtherNode() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/oneLargeValue").with("binaryProp", binary).and();
        graph.create("/otherLargeValue").with("binaryProp", binary).and();

        String hash = keyFor(binary.getHash());
        Set<String> largeValuePaths = new HashSet<String>();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        Node node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        node = graph.getNodeAt("/otherLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        graph.delete("/otherLargeValue").and();

        largeValuePaths.clear();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));
    }

    @Test
    public void shouldRemoveLargeValueWhenSizeOfPropertyShrinks() {
        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/oneLargeValue").with("binaryProp", binary).and();

        String hash = keyFor(binary.getHash());
        Set<String> largeValuePaths = new HashSet<String>();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        Node node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

        Binary newBinary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD - 1));
        graph.set("binaryProp").on("/oneLargeValue").to(newBinary).and();

        largeValuePaths.clear();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(0));

        node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertFalse(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(newBinary));

    }

    @Test
    public void shouldAllowNestedLargeValuesPath() {
        source = new DiskSource();
        source.setName("Disk Source");
        source.setLargeValueSizeInBytes(LARGE_VALUE_THRESHOLD);
        source.setRepositoryRootPath(REPO_ROOT_PATH);
        source.setLargeValuePath("large/values");
        source.initialize(new MockRepositoryContext(context));

        graph = Graph.create(source, context);

        largeValuePath = new File(repoRootPath, source.getLargeValuePath());

        Binary binary = binFactory.create(createBinaryValueOfSize(LARGE_VALUE_THRESHOLD + 1));
        graph.create("/oneLargeValue").with("binaryProp", binary).and();

        String hash = keyFor(binary.getHash());
        Set<String> largeValuePaths = new HashSet<String>();
        largeValuePaths.addAll(Arrays.asList(largeValuePath.list()));

        assertThat(largeValuePaths.size(), is(2));
        assertTrue(largeValuePaths.contains(hash + ".dat"));
        assertTrue(largeValuePaths.contains(hash + ".ref"));

        Node node = graph.getNodeAt("/oneLargeValue");
        assertThat(node.getProperty("binaryProp"), is(notNullValue()));
        assertThat(node.getProperty("binaryProp").size(), is(1));
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof Binary);
        assertTrue(node.getProperty("binaryProp").getFirstValue() instanceof FileSystemBinary);
        assertThat((Binary)node.getProperty("binaryProp").getFirstValue(), is(binary));

    }
}
