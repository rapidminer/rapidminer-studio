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

import java.util.concurrent.Executors;
import java.util.logging.Level;

import com.rapidminer.license.LicenseEvent;
import com.rapidminer.license.LicenseManagerListener;
import com.rapidminer.tools.LogService;


/***
 * Listener that updates the {@link AbGroupProvider#seed} on license change
 *
 * @author Jonas Wilms-Pfau
 * @since 8.2
 */
class UpdateSeedOnLicenseChange implements LicenseManagerListener {

	/**
	 * The provider that gets {@link AbGroupProvider#updateSeed(long) updated} on {@link LicenseEvent.LicenseEventType#ACTIVE_LICENSE_CHANGED license changes}
	 */
	private final AbGroupProvider provider;

	/**
	 * Creates a new {@link LicenseManagerListener} that informs the given provider
	 *
	 * @param provider {@link AbGroupProvider#updateSeed(long) updated} on {@link LicenseEvent.LicenseEventType#ACTIVE_LICENSE_CHANGED license changes}
	 */
	public UpdateSeedOnLicenseChange(AbGroupProvider provider){
		if (provider == null) {
			throw new IllegalArgumentException("provider must not be null");
		}
		this.provider = provider;
	}

	@Override
	public <S, C> void handleLicenseEvent(LicenseEvent<S, C> e) {
		//Clear the INSTANCE in case the license has changed, i.e. during first login
		if (LicenseEvent.LicenseEventType.ACTIVE_LICENSE_CHANGED.equals(e.getType())) {
			//Run this in it's own thread, since this method "must not cause any delay!"
			try {
				Executors.newSingleThreadExecutor().submit(() -> provider.updateSeed(IdentifierProvider.getIdentifier()));
			} catch (Exception er) {
				//This should never happen
				LogService.getRoot().log(Level.WARNING, "com.rapidminer.tools.abtesting.UpdateSeedOnLicenseChange.ab_group_update_failed", er);
			}
		}
	}
}