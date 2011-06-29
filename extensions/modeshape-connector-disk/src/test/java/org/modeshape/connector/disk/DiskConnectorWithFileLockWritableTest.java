package org.modeshape.connector.disk;


public class DiskConnectorWithFileLockWritableTest extends DiskConnectorWritableTest {

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
