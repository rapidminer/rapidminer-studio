/**
 * Copyright (C) 2001-2019 by RapidMiner and the contributors
 *
 * Complete list of developers available at our web site:
 *
 * http://rapidminer.com
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU Affero General Public License as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/.
 */
package com.rapidminer.tools.container;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.TreeMap;
import java.util.WeakHashMap;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * Tests  for {@link StackingMap}.
 *
 * @author Jan Czogalla
 * @since 8.2
 */
public class StackingMapTest {

	public static final int TEST_SEED = 284092;
	public static final int TEST_MAP_SIZE = 20;
	public static final int MAXIMUM_RANDOM_STRING_LENGTH = 20;
	private static Map<String, String> wrappableHashMap;
	private static Map<String, String> wrappableTreeMap;

	@BeforeClass
	public static void prepareWrappableMaps() {
		wrappableHashMap = new HashMap<>();
		wrappableTreeMap = new TreeMap<>();
		Random rng = new Random(TEST_SEED);
		for (int i = 0; i < TEST_MAP_SIZE; i++) {
			String k = randomString(rng);
			String v = randomString(rng);
			wrappableHashMap.put(k, v);
			wrappableTreeMap.put(k, v);
		}
	}

	private static String randomString(Random rng) {
		int size = 1+rng.nextInt(MAXIMUM_RANDOM_STRING_LENGTH);
		StringBuilder builder = new StringBuilder(size);
		for (int i = 0; i < size; i++) {
			boolean upper = rng.nextBoolean();
			builder.append((char) ((upper ? 'A' : 'a') + rng.nextInt(26)));
		}
		return builder.toString();
	}

	/**
	 * Test if all entries are present in a wrapped map
	 */
	@Test
	public void wrapHashMap() {
		StackingMap<String, String> wrappedMap = new StackingMap<>(wrappableHashMap);
		Assert.assertEquals("Different size of maps", wrappableHashMap.size(), wrappedMap.size());
		for (Entry<String, String> e : wrappableHashMap.entrySet()) {
			String v = wrappedMap.get(e.getKey());
			Assert.assertNotNull("Key not present: " + e.getKey(), v);
			Assert.assertEquals("Different values for same key", e.getValue(), v);
		}
	}

	/**
	 * Test if the backing map is actually a tree map by comparing iteration order
	 */
	@Test
	public void wrapTreeMap() {
		StackingMap<String, String> wrappedMap = new StackingMap<>(wrappableTreeMap);
		Iterator<Entry<String, String>> wrappedIterator = wrappedMap.entrySet().iterator();
		Iterator<Entry<String, String>> treeIterator = wrappableTreeMap.entrySet().iterator();
		while (wrappedIterator.hasNext() && treeIterator.hasNext()) {
			Entry<String, String> a = wrappedIterator.next();
			Entry<String, String> b = treeIterator.next();
			Assert.assertEquals("Wrong order for keys", b.getKey(), a.getKey());
			Assert.assertEquals("Different values for same key", b.getValue(), a.getValue());
		}
		Assert.assertEquals("Different size of maps", treeIterator.hasNext(), wrappedIterator.hasNext());
	}

	/**
	 * Test if a weak hash map as a backing map works as expected
	 */
	@Test
	public void backedByWeakHashMap() {
		StackingMap<Object, String> weakBackend = new StackingMap<>(WeakHashMap.class);
		Assert.assertTrue("Newly created map is not empty", weakBackend.isEmpty());
		Object a = new Object();
		weakBackend.push(a, "test");
		Assert.assertFalse("Present key object is not present in map after insertion", weakBackend.isEmpty());
		Assert.assertNotNull("Present key object is not present in map after insertion", weakBackend.get(a));
		a = null;
		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
		}
		Assert.assertTrue("Weak hash map does not get rid of key", weakBackend.isEmpty());
	}

	/**
	 * Test if null as a value argument to push is ignored
	 */
	@Test
	public void ignoreNullValue() {
		StackingMap<Object, Object> stackingMap = new StackingMap<>();
		stackingMap.push(new Object(), null);
		Assert.assertTrue("Map is not empty after insertion of null value", stackingMap.isEmpty());
	}

	/**
	 * Test if values are stacked and can be accessed correctly
	 */
	@Test
	public void stackValues() {
		StackingMap<Object, Object> stackingMap = new StackingMap<>();
		Object key = new Object();
		Object item1 = new Object();
		stackingMap.push(key, item1);
		Assert.assertEquals("Size is wrong", 1, stackingMap.size());
		Object item2 = new Object();
		stackingMap.push(key, item2);
		Assert.assertEquals("Size is wrong", 1, stackingMap.size());
		Assert.assertEquals("Top item is not the same as last pushed item", item2, stackingMap.peek(key));
		Assert.assertEquals("Top item is not the same as last pushed item", item2, stackingMap.pop(key));
		Assert.assertEquals("Size is wrong", 1, stackingMap.size());
		Assert.assertEquals("Top item is not the same as last pushed item", item1, stackingMap.pop(key));
		Assert.assertTrue("Popped map is not empty", stackingMap.isEmpty());
	}
}
