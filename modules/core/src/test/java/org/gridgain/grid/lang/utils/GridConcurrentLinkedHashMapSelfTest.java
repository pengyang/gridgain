/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */
package org.gridgain.grid.lang.utils;

import org.gridgain.testframework.junits.common.*;
import org.jdk8.backport.*;

import java.util.*;

/**
 * This class tests basic contracts of {@code ConcurrentLinkedHashMap}.
 */
public class GridConcurrentLinkedHashMapSelfTest extends GridCommonAbstractTest {
    /** */
    private static final int KEYS_UPPER_BOUND = 1000;

    /** */
    private static final int INSERTS_COUNT = 10000;

    /** Random used for key generation. */
    private static final Random rnd = new Random();

    /**
     *
     */
    public void testInsertionOrder() {
        testOrder(false);
    }

    /**
     *
     */
    public void testInsertionOrderWithUpdate() {
        testOrder(true);
    }

    /**
     *
     */
    public void testEvictionInsert() {
        final int mapSize = 1000;

        Map<Integer, String> tst = new ConcurrentLinkedHashMap<>(10, 0.75f, 1, 1000);

        Map<Integer, String> map = new LinkedHashMap<Integer, String>(10, 0.75f, false) {
            @Override protected boolean removeEldestEntry(Map.Entry<Integer, String> eldest) {
                return size() > mapSize;
            }
        };

        for (int i = 0; i < mapSize; i++) {
            tst.put(i, "value" + i);
            map.put(i, "value" + i);
        }

        Random rnd1 = new Random();

        int iterCnt = 100000;
        int keyCnt = 10000;
        for (int i = 0; i < iterCnt; i++) {
            int key = rnd1.nextInt(keyCnt);

            tst.put(key, "value" + key);
            map.put(key, "value" + key);
        }

        Iterator<Map.Entry<Integer, String>> tstIt = tst.entrySet().iterator();

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            assertTrue("No enough elements in key set", tstIt.hasNext());

            Map.Entry<Integer, String> tstEntry = tstIt.next();

            assertEquals("Key mismatch", tstEntry.getKey(), entry.getKey());
            assertEquals("Value mismatch", tstEntry.getValue(), entry.getValue());
        }

        assertEquals("Invalid map size", mapSize, tst.size());
    }

    /**
     * @throws Exception If failed.
     */
    public void testIteratorRehash() throws Exception {
        ConcurrentLinkedHashMap<Integer, Date> map = new ConcurrentLinkedHashMap<>(10);

        for (int i = 0; i < 10; i++)
            map.put(i, new Date(0));

        for (Date date : map.values())
            assertEquals(0, date.getTime());

        for (int i = 0; i < 1000; i++)
            map.put(i, new Date(1));

        for (Date date : map.values())
            assertEquals(1, date.getTime());
    }

    /**
     * Tests order of elements in map.
     *
     * @param update {@code true} if test should update map via iterator.
     */
    private void testOrder(boolean update) {
        Map<Integer, String> tst = new ConcurrentLinkedHashMap<>();
        Map<Integer, String> map = new LinkedHashMap<>(10, 0.75f);

        for (int i = 0; i < INSERTS_COUNT; i++) {
            int key = rnd.nextInt(KEYS_UPPER_BOUND);

            if (rnd.nextBoolean()) {
                tst.put(key, "value" + key);
                map.put(key, "value" + key);
            }
            else {
                tst.remove(key);
                map.remove(key);
            }
        }

        if (update) {
            Iterator<Map.Entry<Integer, String>> tstIt = tst.entrySet().iterator();

            for (Iterator<Map.Entry<Integer, String>> mapIt = map.entrySet().iterator(); mapIt.hasNext(); ) {
                Map.Entry<Integer, String> entry = mapIt.next();
                assertTrue("No enough elements in key set", tstIt.hasNext());

                Map.Entry<Integer, String> tstEntry = tstIt.next();

                assertEquals("Key mismatch", tstEntry.getKey(), entry.getKey());
                assertEquals("Value mismatch", tstEntry.getValue(), entry.getValue());

                if (entry.getKey() % 2 == 0) {
                    mapIt.remove();
                    tstIt.remove();
                }
            }
        }

        Iterator<Map.Entry<Integer, String>> tstIt = tst.entrySet().iterator();

        for (Map.Entry<Integer, String> entry : map.entrySet()) {
            assertTrue("No enough elements in key set after removal", tstIt.hasNext());

            Map.Entry<Integer, String> tstEntry = tstIt.next();

            assertEquals("Key mismatch after removal", tstEntry.getKey(), entry.getKey());
            assertEquals("Value mismatch after removal", tstEntry.getValue(), entry.getValue());
        }
    }

    /**
     * Tests iterator when concurrent modifications remove and add the same keys to the map.
     *
     */
    public void testIteratorDuplicates() {
        Map<Integer, String> tst = new ConcurrentLinkedHashMap<>();

        for (int i = 0; i < 10; i++)
            tst.put(i, "val" + i);

        Iterator<Integer> it = tst.keySet().iterator();
        for (int i = 0; i < 5; i++) {
            assertTrue("Not enough elements", it.hasNext());
            assertEquals(i, it.next().intValue());
        }

        for (int i = 0; i < 5; i++) {
            tst.remove(i);
            tst.put(i, "val" + i);
        }

        for (int i = 5; i < 10; i++) {
            assertTrue("Not enough elements", it.hasNext());
            assertEquals(i, it.next().intValue());
        }

        assertFalse("Duplicate key", it.hasNext());
    }

    /**
     *
     */
    public void testDescendingMethods() {
        ConcurrentLinkedHashMap<Integer, Integer> tst = new ConcurrentLinkedHashMap<>();

        for (int i = 0; i < 100; i++)
            tst.put(i, i);

        int nextVal = 99;

        for (int i : tst.descendingKeySet()) {
            assert nextVal == i : "Unexpected value: " + i;

            nextVal--;
        }

        assert nextVal == -1;

        // Test values.
        nextVal = 99;

        for (int i : tst.descendingValues()) {
            assert nextVal == i : "Unexpected value: " + i;

            nextVal--;
        }

        assert nextVal == -1 : "Unexpected value: " + nextVal;

        // Test entries.
        nextVal = 99;

        for (Map.Entry<Integer, Integer> e : tst.descendingEntrySet()) {
            assert nextVal == e.getKey() : "Unexpected value: " + nextVal;
            assert nextVal == e.getValue() : "Unexpected value: " + nextVal;

            nextVal--;
        }

        assert nextVal == -1 : "Unexpected value: " + nextVal;

        // Test keys as enumeration.
        nextVal = 99;

        for (Enumeration<Integer> e = tst.descendingKeys(); e.hasMoreElements();) {
            int i = e.nextElement();

            assert nextVal == i : "Unexpected value: " + i;

            nextVal--;
        }

        assert nextVal == -1 : "Unexpected value: " + nextVal;

        // Test keys as enumeration.
        nextVal = 99;

        for (Enumeration<Integer> e = tst.descendingElements(); e.hasMoreElements();) {
            int i = e.nextElement();

            assert nextVal == i : "Unexpected value: " + i;

            nextVal--;
        }

        assert nextVal == -1 : "Unexpected value: " + nextVal;
    }
}
