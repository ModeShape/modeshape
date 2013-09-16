package org.modeshape.jcr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link Upgrades}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class UpgradesTest {

    private TestUpgradeOperation operation1;
    private TestUpgradeOperation operation2;
    private TestUpgradeOperation operation3;
    private Upgrades testUpgrades;

    @Before
    public void before() {
        operation1 = new TestUpgradeOperation(2);
        operation2 = new TestUpgradeOperation(3);
        operation3 = new TestUpgradeOperation(4);
        testUpgrades = new Upgrades(operation1, operation2, operation3);
    }

    @Test
    public void shouldApplyOnlyLatestUpgrades() throws Exception {
        testUpgrades.applyUpgradesSince(0, null);
        operation1.assertCalled();
        operation2.assertCalled();
        operation3.assertCalled();

        testUpgrades.applyUpgradesSince(1, null);
        operation1.assertCalled();
        operation2.assertCalled();
        operation3.assertCalled();

        testUpgrades.applyUpgradesSince(2, null);
        operation1.assertNotCalled();
        operation2.assertCalled();
        operation3.assertCalled();

        testUpgrades.applyUpgradesSince(3, null);
        operation1.assertNotCalled();
        operation2.assertNotCalled();
        operation3.assertCalled();

        testUpgrades.applyUpgradesSince(4, null);
        operation1.assertNotCalled();
        operation2.assertNotCalled();
        operation3.assertNotCalled();

        testUpgrades.applyUpgradesSince(5, null);
        operation1.assertNotCalled();
        operation2.assertNotCalled();
        operation3.assertNotCalled();
    }

    @Test
    public void shouldReturnLatestAvailableUpgradeId() throws Exception {
        assertEquals(4, testUpgrades.getLatestAvailableUpgradeId());
        assertEquals(Upgrades.EMPTY_UPGRADES_ID, new Upgrades().getLatestAvailableUpgradeId());
    }

    @Test
    public void shouldCorrectlyDetermineIfUpgradeIsRequired() throws Exception {
        assertTrue(testUpgrades.isUpgradeRequired(-1));
        assertTrue(testUpgrades.isUpgradeRequired(0));
        assertTrue(testUpgrades.isUpgradeRequired(1));
        assertTrue(testUpgrades.isUpgradeRequired(2));
        assertTrue(testUpgrades.isUpgradeRequired(3));
        assertFalse(testUpgrades.isUpgradeRequired(4));
        assertFalse(testUpgrades.isUpgradeRequired(5));
    }

    protected static class TestUpgradeOperation extends Upgrades.UpgradeOperation {
        private boolean called = false;

        protected TestUpgradeOperation( int id ) {
            super(id);
        }

        @Override
        public void apply( Upgrades.Context resources ) {
            called = true;
        }

        protected void reset() {
            called = false;
        }

        protected void assertCalled() {
            assertTrue("Upgrade operation not called" , called);
            reset();
        }

        protected void assertNotCalled() {
            assertFalse("Upgrade operation called", called);
            reset();
        }
    }
}
