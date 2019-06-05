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
package com.rapidminer.connection.gui.model;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.math.RandomUtils;
import org.junit.Assert;
import org.junit.Test;


/**
 * Test for {@link ConnectionParameterModel} and {@link PlaceholderParameterModel}
 * @since 9.3
 * @author Jonas Wilms-Pfau
 */
public class ConnectionParameterModelTest {

	private final String groupName = RandomStringUtils.randomAlphabetic(10);
	private final String name = RandomStringUtils.randomAlphabetic(10);
	private final String value = RandomStringUtils.randomAlphabetic(10);
	private final boolean isEncrypted = RandomUtils.nextBoolean();
	private final String injectorName = RandomStringUtils.randomAlphabetic(10);
	private final boolean isEnabled = RandomUtils.nextBoolean();


	@Test
	public void testParameterModelFields() {
		PlaceholderParameterModel model = new PlaceholderParameterModel(null, groupName, name, value, isEncrypted, injectorName, isEnabled);
		PlaceholderParameterModel copy = model.copyDataOnly();
		ConnectionParameterModel copy2 = model.copyDataOnly();
		Assert.assertEquals(groupName, copy.getGroupName());
		Assert.assertEquals(name, copy.getName());
		Assert.assertEquals(value, copy.getValue());
		Assert.assertEquals(isEncrypted, copy.isEncrypted());
		// injection name is always set, so it must be true
		Assert.assertTrue(copy.isInjected());
		Assert.assertEquals(model, model);
		String otherValue = "other" + value;
		String otherName = "other" + name;
		String otherGroupName = "other" + groupName;
		Assert.assertEquals(groupName, copy2.getGroupName());
		copy.setName(otherName);
		Assert.assertEquals(otherName, copy.getName());
		copy.setGroupName(otherGroupName);
		Assert.assertEquals(otherGroupName, copy.getGroupName());
		copy.setValue(otherValue);
		Assert.assertEquals(otherValue, copy.getValue());
		copy.setEncrypted(!isEncrypted);
		Assert.assertEquals(!isEncrypted, copy.isEncrypted());
		Assert.assertTrue(copy.isInjected());
		Assert.assertNotEquals(model, copy);
	}

}