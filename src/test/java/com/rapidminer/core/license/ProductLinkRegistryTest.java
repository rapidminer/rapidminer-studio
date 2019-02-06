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
package com.rapidminer.core.license;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.rapidminer.license.StudioLicenseConstants;


/**
 * Test for ProductLinkRegistry
 *
 * @author Jonas Wilms-Pfau
 * @since 9.1.0
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ProductLinkRegistryTest {

	private static final String UNREGISTERED = "unregistered";
	private static final String EXT1 = "ext1";
	private static final String STUDIO = StudioLicenseConstants.PRODUCT_ID;

	private static final String EXT1_URL = "http://www.example.com/ext1";
	private static final String STUDIO_URL = "http://www.example.com/studio";

	@Test
	public void a_unRegisteredTest() {
		Assert.assertEquals(ProductLinkRegistry.PURCHASE.get(UNREGISTERED, STUDIO_URL), STUDIO_URL);
	}

	@Test
	public void b_nullTest() {
		Assert.assertEquals(ProductLinkRegistry.PURCHASE.get(null, STUDIO_URL), STUDIO_URL);
	}

	@Test
	public void c_registerTest() {
		ProductLinkRegistry.PURCHASE.register(EXT1, STUDIO_URL);
		ProductLinkRegistry.PURCHASE.register(EXT1, EXT1_URL);
		Assert.assertEquals(ProductLinkRegistry.PURCHASE.get(EXT1, STUDIO_URL), EXT1_URL);
	}

	@Test
	public void d_studioTest() {
		ProductLinkRegistry.PURCHASE.register(EXT1, EXT1_URL);
		Assert.assertEquals(ProductLinkRegistry.PURCHASE.get(STUDIO, STUDIO_URL), STUDIO_URL);
	}

	@Test(expected = NullPointerException.class)
	public void e_registerNullProductId() {
		ProductLinkRegistry.PURCHASE.register(null, STUDIO_URL);
	}

	@Test(expected = NullPointerException.class)
	public void f_registerNullLink() {
		ProductLinkRegistry.PURCHASE.register(EXT1, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void g_registerInvalidLink() {
		ProductLinkRegistry.PURCHASE.register(EXT1, EXT1);
	}

	@Test(expected = UnsupportedOperationException.class)
	public void h_registerStudio() {
		ProductLinkRegistry.PURCHASE.register(STUDIO, STUDIO_URL);
	}
}
