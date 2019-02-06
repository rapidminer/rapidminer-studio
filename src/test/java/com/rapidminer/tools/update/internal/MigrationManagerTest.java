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
package com.rapidminer.tools.update.internal;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.rapidminer.gui.tools.VersionNumber;


/**
 * Tests for the necessary Tests retrieved from the {@link MigrationManager}
 *
 * @author Andreas Timm
 * @since 9.1.0
 */
public class MigrationManagerTest {

	@Test
	public void checkNull() {
		final List<MigrationManager.MigrationStep> necessaryMigrationSteps = MigrationManager.getNecessaryMigrationSteps(null, null);
		Assert.assertNotNull(necessaryMigrationSteps);
		Assert.assertTrue(necessaryMigrationSteps.isEmpty());
	}

	@Test
	public void checkTests() {
		// right now there is only the migration from any version to 9.1.0 so all other versions around that are not to be migrated
		VersionNumber VN_8_0_0 = new VersionNumber(8, 0, 0);
		VersionNumber VN_9_0_0 = new VersionNumber(9, 0, 0);
		VersionNumber VN_9_1_0 = new VersionNumber(9, 1, 0);

		List<MigrationManager.MigrationStep> necessaryMigrationSteps = MigrationManager.getNecessaryMigrationSteps(VN_8_0_0, VN_8_0_0);
		Assert.assertEquals(0, necessaryMigrationSteps.size());

		necessaryMigrationSteps = MigrationManager.getNecessaryMigrationSteps(VN_8_0_0, VN_9_0_0);
		Assert.assertEquals(0, necessaryMigrationSteps.size());

		necessaryMigrationSteps = MigrationManager.getNecessaryMigrationSteps(VN_8_0_0, VN_9_1_0);
		Assert.assertEquals(1, necessaryMigrationSteps.size());

		necessaryMigrationSteps = MigrationManager.getNecessaryMigrationSteps(VN_9_0_0, VN_9_1_0);
		Assert.assertEquals(1, necessaryMigrationSteps.size());

		necessaryMigrationSteps = MigrationManager.getNecessaryMigrationSteps(VN_9_1_0, VN_9_1_0);
		Assert.assertEquals(0, necessaryMigrationSteps.size());
	}
}
