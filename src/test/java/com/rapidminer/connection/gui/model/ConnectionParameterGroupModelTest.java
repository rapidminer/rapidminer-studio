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
 * Test for {@link ConnectionParameterGroupModel}
 * @author Jonas Wilms-Pfau
 * @since 9.3
 */
public class ConnectionParameterGroupModelTest {

	private final String groupName = RandomStringUtils.randomAlphabetic(10);
	private final String name = RandomStringUtils.randomAlphabetic(10);
	private final String value = RandomStringUtils.randomAlphabetic(10);
	private final boolean isEncrypted = RandomUtils.nextBoolean();
	private final String injectorName = RandomStringUtils.randomAlphabetic(10);
	private final boolean isEnabled = RandomUtils.nextBoolean();


	@Test
	public void testConnectionParameterGroupModel() {
		ConnectionParameterGroupModel model = new ConnectionParameterGroupModel(null, groupName);
		ConnectionParameterGroupModel clone = model.copyDataOnly();
		Assert.assertEquals(groupName, clone.getName());
		clone.addOrSetParameter(name, value, !isEncrypted, injectorName, !isEnabled);
		clone.addOrSetParameter(name, value, isEncrypted, injectorName, isEnabled);
		ConnectionParameterGroupModel clone2 = clone.copyDataOnly();
		clone.removeParameter(name);
		Assert.assertEquals(0, clone.getParameters().size());
		Assert.assertEquals(1, clone2.getParameters().size());
		Assert.assertEquals(isEncrypted, clone2.getParameters().get(0).isEncrypted());
		Assert.assertTrue(clone2.getParameters().get(0).isInjected());
		Assert.assertEquals(isEnabled, clone2.getParameters().get(0).isEnabled());
	}
}