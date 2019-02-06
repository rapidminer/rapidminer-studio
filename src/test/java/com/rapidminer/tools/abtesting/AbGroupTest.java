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
package com.rapidminer.tools.abtesting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;


/**
 * Tests for the {@link AbGroupProvider}
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
public class AbGroupTest {

	@Test
	public void testReproducibleResult() {
		int numberOfGroups = 10;
		AbGroupProvider provider = new AbGroupProvider(UUID.randomUUID().toString());
		int group = provider.getGroup(numberOfGroups);
		for (int i = 0; i <= 10; i++) {
			Assert.assertEquals(group, provider.getGroup(numberOfGroups));
		}
	}

	@Test
	public void testRandomDistribution() {
		int numberOfGroups = Math.max(1, (int) Math.round(Math.random() * 10));
		Set<Integer> hit = new HashSet<>(numberOfGroups);
		//This should take less then 100 iterations
		for (int i = 0; i < (numberOfGroups*numberOfGroups) * 10000; i++) {
			int group = new AbGroupProvider(UUID.randomUUID().toString()).getGroup(numberOfGroups);
			hit.add(group);
			if(hit.size() == numberOfGroups){
				break;
			}
		}
		Assert.assertEquals(numberOfGroups, hit.size());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testZeroGroups() {
		new AbGroupProvider(UUID.randomUUID().toString()).getGroup(0);
	}

	@Test
	public void testOneGroup() {
		int expected = 0;
		Assert.assertEquals(expected, new AbGroupProvider(UUID.randomUUID().toString()).getGroup(expected + 1));
	}

	@Test
	public void testRandomInRange() {
		AbGroupProvider provider = new AbGroupProvider(UUID.randomUUID().toString());
		for (int i = 1; i <= 100; i++) {
			int group = provider.getGroup(i);
			Assert.assertTrue(group >= 0 && group < i);
		}
	}

	@Test
	public void testEmptySeed(){
		new AbGroupProvider("");
	}

	@Test
	public void testNullSeed(){
		new AbGroupProvider(null);
	}

	@Test
	public void testFallBackProvider() {
		int group = AbGroupProvider.get(42);
		Assert.assertTrue(group >= 0 && group < 42);
	}

	@Test
	public void testListener() {
		final int abGroups = 5;
		final int initialGroup = 0;
		final int targetGroup = Math.max(Math.min(1, (int) Math.round(Math.random() * (abGroups))), abGroups-1);
		final int listenerTriggerTarget = 1;
		AbGroupProvider provider = new AbGroupProvider(null);
		AbGroupChanger.changeGroup(initialGroup, abGroups, provider);
		Assert.assertEquals(initialGroup, provider.getGroup(abGroups));
		final AtomicInteger listenerTriggered = new AtomicInteger(0);
		AbGroupChangedListener listener = (newGroup, oldGroup, numberOfGroups) -> {
			Assert.assertNotEquals("Group was not changed", newGroup, oldGroup);
			Assert.assertEquals("Current group is wrong", initialGroup, oldGroup);
			Assert.assertEquals("New group is wrong", targetGroup, newGroup);
			listenerTriggered.incrementAndGet();
		};
		provider.registerAbGroupChangedListener(listener, abGroups, false);
		//Change group and verify that it got triggered only once
		AbGroupChanger.changeGroup(targetGroup, abGroups, provider);
		Assert.assertEquals(targetGroup, provider.getGroup(abGroups));
		Assert.assertEquals("Listener was not triggered on group change", listenerTriggerTarget, listenerTriggered.get());
		//Remove listener, change group back to initial value
		provider.removeAbGroupChangedListener(listener);
		AbGroupChanger.changeGroup(initialGroup, abGroups, provider);
		Assert.assertEquals(initialGroup, provider.getGroup(abGroups));
		Assert.assertEquals("Removed listener got triggered", listenerTriggerTarget, listenerTriggered.get());
	}

}
