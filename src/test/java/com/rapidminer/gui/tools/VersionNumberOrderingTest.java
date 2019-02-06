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
package com.rapidminer.gui.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;


/**
 * @author Andreas Timm
 * @since 8.0.0
 */
public class VersionNumberOrderingTest {

	private static final VersionNumber SMALLEST = null;

	private static final VersionNumber VN1_0_0 = new VersionNumber(1, 0, 0);
	private static final VersionNumber VN1_0_1 = new VersionNumber(1, 0, 1);
	private static final VersionNumber VN1_1_0 = new VersionNumber(1, 1, 0);
	private static final VersionNumber VN1_1_1 = new VersionNumber(1, 1, 1);
	private static final VersionNumber VN2_0_0 = new VersionNumber(2, 0, 0);

	private static final VersionNumber VN1SNAPSHOT = new VersionNumber(1, 0, 0, "SNAPSHOT");
	private static final VersionNumber VN1ALPHA = new VersionNumber(1, 0, 0, "ALPHA");
	private static final VersionNumber VN1ALPHA1 = new VersionNumber(1, 0, 0, "ALPHA1");
	private static final VersionNumber VN1ALPHA2 = new VersionNumber(1, 0, 0, "ALPHA2");
	private static final VersionNumber VN1BETA = new VersionNumber(1, 0, 0, "BETA");
	private static final VersionNumber VN1BETA1 = new VersionNumber(1, 0, 0, "BETA1");
	private static final VersionNumber VN1RC = new VersionNumber(1, 0, 0, "RC");
	private static final VersionNumber VN1RC1 = new VersionNumber(1, 0, 0, "RC1");
	private static final VersionNumber VN1RC2 = new VersionNumber(1, 0, 0, "RC2");
	private static final VersionNumber VN1BETA2 = new VersionNumber(1, 0, 0, "BETA2");
	private static final VersionNumber VN2SNAPSHOT = new VersionNumber(2, 0, 0, "SNAPSHOT");
	private static final VersionNumber VN2ALPHA = new VersionNumber(2, 0, 0, "ALPHA");
	private static final VersionNumber VN2ALPHA1 = new VersionNumber(2, 0, 0, "ALPHA1");
	private static final VersionNumber VN2ALPHA2 = new VersionNumber(2, 0, 0, "ALPHA2");
	private static final VersionNumber VN2BETA = new VersionNumber(2, 0, 0, "BETA");
	private static final VersionNumber VN2BETA1 = new VersionNumber(2, 0, 0, "BETA1");
	private static final VersionNumber VN2BETA2 = new VersionNumber(2, 0, 0, "BETA2");
	private static final VersionNumber VN2RC = new VersionNumber(2, 0, 0, "RC");
	private static final VersionNumber VN2RC1 = new VersionNumber(2, 0, 0, "RC1");
	private static final VersionNumber VN2RC2 = new VersionNumber(2, 0, 0, "RC2");
	private static final VersionNumber VN2RC21 = new VersionNumber(2, 0, 0, "RC21");

	private static List<VersionNumber> allVersions = new ArrayList<>();
	private static List<VersionNumber> allUniqueVersions = new ArrayList<>();

	static {
		allUniqueVersions.add(SMALLEST);
		allUniqueVersions.add(VN1SNAPSHOT);
		allUniqueVersions.add(VN1ALPHA);
		allVersions.add(VN1ALPHA1);
		allUniqueVersions.add(VN1ALPHA2);
		allUniqueVersions.add(VN1BETA);
		allVersions.add(VN1BETA1);
		allUniqueVersions.add(VN1BETA2);
		allUniqueVersions.add(VN1RC);
		allVersions.add(VN1RC1);
		allUniqueVersions.add(VN1RC2);
		allUniqueVersions.add(VN1_0_0);
		allUniqueVersions.add(VN1_0_1);
		allUniqueVersions.add(VN1_1_0);
		allUniqueVersions.add(VN1_1_1);
		allUniqueVersions.add(VN2SNAPSHOT);
		allUniqueVersions.add(VN2ALPHA);
		allVersions.add(VN2ALPHA1);
		allUniqueVersions.add(VN2ALPHA2);
		allUniqueVersions.add(VN2BETA);
		allVersions.add(VN2BETA1);
		allUniqueVersions.add(VN2BETA2);
		allUniqueVersions.add(VN2RC);
		allVersions.add(VN2RC1);
		allUniqueVersions.add(VN2RC2);
		allUniqueVersions.add(VN2RC21);
		allUniqueVersions.add(VN2_0_0);

		allVersions.addAll(allUniqueVersions);
	}

	@Test
	public void lt() {
		for (int i = 0; i < allUniqueVersions.size() - 1; i++) {
			for (int k = i + 1; k < allUniqueVersions.size(); k++) {
				if(allUniqueVersions.get(i) != null) {
					Assert.assertTrue("versions " + allUniqueVersions.get(i) + " should be lower than " + allUniqueVersions.get(k), allUniqueVersions.get(i).compareTo(allUniqueVersions.get(k)) < 0);
					Assert.assertFalse("versions " + allUniqueVersions.get(i) + " should not be at least " + allUniqueVersions.get(k), allUniqueVersions.get(i).isAtLeast(allUniqueVersions.get(k)));
					Assert.assertTrue("versions " + allUniqueVersions.get(i) + " should be at most " + allUniqueVersions.get(k), allUniqueVersions.get(i).isAtMost(allUniqueVersions.get(k)));
					Assert.assertFalse("versions " + allUniqueVersions.get(i) + " should not be above " + allUniqueVersions.get(k), allUniqueVersions.get(i).isAbove(allUniqueVersions.get(k)));
				}
				Assert.assertFalse("versions " + allUniqueVersions.get(k) + " should not be lower than " + allUniqueVersions.get(i), allUniqueVersions.get(k).compareTo(allUniqueVersions.get(i)) < 0);
				Assert.assertTrue("versions " + allUniqueVersions.get(k) + " should be at least " + allUniqueVersions.get(i), allUniqueVersions.get(k).isAtLeast(allUniqueVersions.get(i)));
				Assert.assertFalse("versions " + allUniqueVersions.get(k) + " should not be at most " + allUniqueVersions.get(i), allUniqueVersions.get(k).isAtMost(allUniqueVersions.get(i)));
				Assert.assertTrue("versions " + allUniqueVersions.get(k) + " should be above " + allUniqueVersions.get(i), allUniqueVersions.get(k).isAbove(allUniqueVersions.get(i)));

			}
		}
		Assert.assertTrue(VN1ALPHA.compareTo(VN1ALPHA1) == 0);
		Assert.assertTrue(VN1BETA.compareTo(VN1BETA1) == 0);
		Assert.assertTrue(VN1RC.compareTo(VN1RC1) == 0);
		Assert.assertTrue(VN2ALPHA.compareTo(VN2ALPHA1) == 0);
		Assert.assertTrue(VN2BETA.compareTo(VN2BETA1) == 0);
		Assert.assertTrue(VN2RC.compareTo(VN2RC1) == 0);
	}

	@Test
	public void gt() {
		for (int i = allUniqueVersions.size() - 1; i > 0; i--) {
			for (int k = 0; k < i; k++) {
				Assert.assertFalse("versions " + allUniqueVersions.get(i) + " should not be lower than " + allUniqueVersions.get(k), allUniqueVersions.get(i).compareTo(allUniqueVersions.get(k)) < 0);
				Assert.assertTrue("versions " + allUniqueVersions.get(i) + " should be at least " + allUniqueVersions.get(k), allUniqueVersions.get(i).isAtLeast(allUniqueVersions.get(k)));
				Assert.assertFalse("versions " + allUniqueVersions.get(i) + " should not be at most " + allUniqueVersions.get(k), allUniqueVersions.get(i).isAtMost(allUniqueVersions.get(k)));
				Assert.assertTrue("versions " + allUniqueVersions.get(i) + " should be above " + allUniqueVersions.get(k), allUniqueVersions.get(i).isAbove(allUniqueVersions.get(k)));
			}
		}
	}

	@Test
	public void eq() {
		for (VersionNumber vn : allVersions) {
			if(vn != null) {
				Assert.assertTrue(vn.equals(vn));
			}
		}
	}

	@Test
	public void eqHashcode() {
		for (VersionNumber vn : allVersions) {
			if(vn != null) {
				Assert.assertEquals(vn.hashCode(), vn.hashCode());
			}
		}
	}

	@Test
	public void neqHashcode() {
		Set<Integer> allHashcodes = new TreeSet<>();
		for (VersionNumber vn : allUniqueVersions) {
			if (vn != null) {
				int hash = vn.hashCode();
				Assert.assertFalse("HashCode duplicate for " + vn, allHashcodes.contains(hash));
				allHashcodes.add(hash);
			}
		}
		for (VersionNumber vn : allVersions) {
			if (vn != null) {
				int hash = vn.hashCode();
				Assert.assertTrue(allHashcodes.contains(hash));
			}
		}
	}

	@Test
	public void neq() {
		for (int i = 0; i < allUniqueVersions.size(); i++) {
			for (int k = 0; k < allUniqueVersions.size(); k++) {
				if (i == k) {
					continue;
				}
				if (allUniqueVersions.get(i) != null) {
					Assert.assertFalse(allUniqueVersions.get(i).equals(allUniqueVersions.get(k)));
					if (allUniqueVersions.get(k) != null) {
						Assert.assertFalse(allUniqueVersions.get(i).hashCode() == allUniqueVersions.get(k).hashCode());
					}
					Assert.assertFalse(allUniqueVersions.get(i).compareTo(allUniqueVersions.get(k)) == 0);
				}
			}
		}
	}

	@Test
	public void stringEqual() {
		Assert.assertEquals("2.0.0-RC21", VN2RC21.getShortLongVersion());
		for(int major = 0; major <= 10; major++) {
			for(int minor = 0; minor <= 10; minor++) {
				for(int patch = 0; patch <= 799; patch += 113) {
					String classifier = null;
					if(patch %2 == 0 ) {
						classifier = UUID.randomUUID().toString().toUpperCase();
					}
					Assert.assertEquals(major + "." + minor + "." + patch + (classifier != null ? "-" + classifier : ""), new VersionNumber(major, minor, patch, classifier).getShortLongVersion());
				}
			}
		}
	}
}
