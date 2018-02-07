/**
 * Copyright (C) 2001-2018 by RapidMiner and the contributors
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
package com.rapidminer.search;

import org.junit.Assert;
import org.junit.Test;


/**
 * Test methods from GlobalSearchCategory
 *
 * @author Andreas Timm
 * @since 8.1
 */
public class GlobalSearchCategoryTest {

	@Test
	public void testEqualsHashcode() {
		GlobalSearchCategory cat1 = new GlobalSearchCategory("cat1", null);
		GlobalSearchCategory cat2 = new GlobalSearchCategory("cat2", null);
		GlobalSearchCategory cat3 = new GlobalSearchCategory("cat3", null);

		Object o = null;
		Object object = new Object();

		Assert.assertTrue(cat1.equals(cat1));
		Assert.assertTrue(cat2.equals(cat2));
		Assert.assertTrue(cat3.equals(cat3));

		Assert.assertEquals(cat1.hashCode(), cat1.hashCode());
		Assert.assertEquals(cat2.hashCode(), cat2.hashCode());
		Assert.assertEquals(cat3.hashCode(), cat3.hashCode());

		Assert.assertFalse(cat1.equals(o));
		Assert.assertFalse(cat1.equals(object));
		Assert.assertFalse(cat2.equals(o));
		Assert.assertFalse(cat2.equals(object));
		Assert.assertFalse(cat3.equals(o));
		Assert.assertFalse(cat3.equals(object));

		Assert.assertFalse(cat1.equals(cat2));
		Assert.assertFalse(cat1.equals(cat3));
		Assert.assertFalse(cat2.equals(cat1));
		Assert.assertFalse(cat2.equals(cat3));
		Assert.assertFalse(cat3.equals(cat1));
		Assert.assertFalse(cat3.equals(cat2));

		Assert.assertNotEquals(cat1.hashCode(), cat2.hashCode());
		Assert.assertNotEquals(cat1.hashCode(), cat3.hashCode());
		Assert.assertNotEquals(cat2.hashCode(), cat3.hashCode());

		Assert.assertNotEquals(cat1.hashCode(), object.hashCode());
		Assert.assertNotEquals(cat2.hashCode(), object.hashCode());
		Assert.assertNotEquals(cat3.hashCode(), object.hashCode());

	}
}
