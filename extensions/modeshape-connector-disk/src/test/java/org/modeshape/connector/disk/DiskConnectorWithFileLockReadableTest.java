package org.modeshape.connector.disk;


public class DiskConnectorWithFileLockReadableTest extends DiskConnectorReadableTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected DiskSource setUpSource() {
        DiskSource source = super.setUpSource();
        source.setLockFileUsed(true);

        return source;
    }
}
