package org.infinispan.schematic.internal.document;

import java.util.Iterator;
import java.util.List;
import org.junit.Before;
import org.junit.Test;

public class BasicArrayTest {

    private BasicArray array;

    @Before
    public void beforeTest() {
        array = new BasicArray();
        for (int i = 0; i != 10; ++i) {
            array.addValue(i);
        }
    }

    @Test
    public void shouldReturnProperSubarray() {
        List<?> subArray = array.subList(3, 8);
        assert (Integer)subArray.get(0) == 3;
        assert (Integer)subArray.get(1) == 4;
        assert (Integer)subArray.get(2) == 5;
        assert (Integer)subArray.get(3) == 6;
        assert (Integer)subArray.get(4) == 7;

        Iterator<?> iter = subArray.iterator();
        int value = 3;
        while (iter.hasNext()) {
            assert (Integer)iter.next() == value++;
        }
    }

    @Test
    public void shouldCreateCopy() {
        BasicArray array2 = new BasicArray(array.size());
        array2.addAllValues(array);
        for (int i = 0; i != 10; ++i) {
            assert (Integer)array2.get(i) == i;
        }
    }

    @Test
    public void shouldCreateCopyOfSublist() {
        BasicArray array2 = new BasicArray(5);
        array2.addAllValues(array.subList(3, 8));
        for (int i = 0; i != 5; ++i) {
            assert (Integer)array2.get(i) == (i + 3);
        }
    }
}
