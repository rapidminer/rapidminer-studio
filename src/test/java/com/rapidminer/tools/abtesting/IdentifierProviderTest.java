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

import java.net.SocketException;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.tools.abtesting.IdentifierProvider;


/**
 * Tests for the {@link IdentifierProvider}
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
public class IdentifierProviderTest {

	@Test
	public void testSeed() {
		long seed = IdentifierProvider.getIdentifier();
		Assert.assertEquals(seed, IdentifierProvider.getIdentifier());
	}

	@Test
	public void testUserBasedSeed(){
		Assert.assertNotEquals("", IdentifierProvider.getUserAndOSVersion().trim());
		Assert.assertNotEquals(System.getProperty("os.version", ""), IdentifierProvider.getUserAndOSVersion().trim());
	}

	@Test
	public void testHardwareAddressSeed() throws SocketException {
		String seed =  IdentifierProvider.getHardwareAddress();
		Assert.assertNotEquals("", seed.trim());
		Assert.assertEquals(seed, IdentifierProvider.getHardwareAddress());
	}
}
